package com.middle.wcs.produce.service.impl;

import com.middle.wcs.hander.BusinessException;
import com.middle.wcs.hander.ResultCodeEnum;
import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.dao.ProducePalletMapper;
import com.middle.wcs.produce.entity.dto.MatchAndAssignDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.dto.SendDestinationDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.middle.wcs.produce.service.ProduceBatchService;
import com.middle.wcs.produce.service.ProducePalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生产托盘 Service 实现
 */
@Slf4j
@Service
public class ProducePalletServiceImpl implements ProducePalletService {

    @Resource
    private ProducePalletMapper producePalletMapper;

    @Resource
    private ProduceBatchMapper produceBatchMapper;

    @Resource
    private ProduceGoodsMapper produceGoodsMapper;

    @Lazy
    @Resource
    private ProduceBatchService produceBatchService;

    @Override
    public List<PalletDetailDTO> listByBatchId(Long batchId) {
        List<ProducePallet> pallets = producePalletMapper.selectByBatchId(batchId);
        List<PalletDetailDTO> result = new ArrayList<>();
        if (pallets == null || pallets.isEmpty()) {
            return result;
        }
        // 批量查所有托盘的货物再按 palletId 分组，避免逐托盘 selectByPalletId 的 N+1
        List<Long> palletIds = new ArrayList<>();
        for (ProducePallet p : pallets) {
            palletIds.add(p.getId());
        }
        List<ProduceGoods> allGoods = produceGoodsMapper.selectByPalletIds(palletIds);
        Map<Long, List<ProduceGoods>> goodsByPallet = new HashMap<>();
        if (allGoods != null) {
            for (ProduceGoods g : allGoods) {
                List<ProduceGoods> list = goodsByPallet.get(g.getPalletId());
                if (list == null) {
                    list = new ArrayList<>();
                    goodsByPallet.put(g.getPalletId(), list);
                }
                list.add(g);
            }
        }
        for (ProducePallet p : pallets) {
            List<ProduceGoods> goods = goodsByPallet.get(p.getId());
            result.add(PalletDetailDTO.from(p, goods == null ? new ArrayList<>() : goods));
        }
        return result;
    }

    @Override
    @Transactional
    public PalletDetailDTO sendDestination(SendDestinationDTO dto) {
        Long palletId = dto.getPalletId();
        String virtualId = dto.getVirtualId();
        String destinationCode = dto.getDestinationCode();
        List<String> barcodes = dto.getBarcodes();
        boolean skipScanCheck = Boolean.TRUE.equals(dto.getSkipScanCheck());
        // 1. palletId + virtualId 双条件定位托盘
        ProducePallet pallet = producePalletMapper.selectByIdAndVirtualId(palletId, virtualId);
        if (pallet == null) {
            throw new RuntimeException("托盘不存在或 virtualId 不匹配: palletId=" + palletId);
        }
        if ("1".equals(pallet.getSendStatus())) {
            throw new RuntimeException("该托盘已发送过目的地，请勿重复发送");
        }

        // 2. 更新01006扫码状态：将传入的barcodes对应的货物标记为01006已扫码（可选，PC端传）
        // 条码不存在/已作废时不抛异常，跳过即可，最终 tray_status 不为 "2" 会走 999 分支
        if (barcodes != null && !barcodes.isEmpty()) {
            for (String uid : barcodes) {
                produceGoodsMapper.markScanned(uid, "01006", pallet.getBatchId());
            }
        }

        // 3. 汇总该托盘下所有 goods 的 scan_status
        List<ProduceGoods> goodsList = produceGoodsMapper.selectByPalletId(palletId);
        long scanned = goodsList.stream().filter(g -> "1".equals(g.getScanStatus())).count();
        String trayStatus;
        if (scanned == 0) {
            trayStatus = "0";
        } else if (scanned < goodsList.size()) {
            trayStatus = "1";
        } else {
            trayStatus = "2";
        }

        // 4. 确定发送目的地编码
        // skipScanCheck=true：跳过扫码判断，直接赋值目的地编码+1/2后缀
        // 全扫→destinationCode+1/2后缀，非全扫→"999"（异常标记，便于查询）
        String sendCode;
        if (skipScanCheck || "2".equals(trayStatus)) {
            String suffix = determineSuffixForFullScan(pallet.getBatchId());
            sendCode = destinationCode + suffix;
        } else {
            sendCode = "999";
        }

        // 5. 回写托盘（palletId + virtualId 双条件）
        producePalletMapper.sendDestination(palletId, virtualId, trayStatus, sendCode);

        // 6. 全部托盘发出真实目的地后自动完结（999 不算）
        produceBatchService.tryFinishIfAllSent(pallet.getBatchId());

        // 7. 回查更新后数据
        ProducePallet updated = producePalletMapper.selectById(palletId);
        return PalletDetailDTO.from(updated, goodsList);
    }

    /**
     * 确定全扫状态下的后缀：1/2交替
     * 规则：从当前批次已发送目的地托盘中，按发送时间倒序追溯，
     * 跳过后缀3，找到最近的1或2后缀，本次取其相反值。
     * 若无历史记录则默认为2。
     */
    private String determineSuffixForFullScan(Long batchId) {
        // 直接查询当前批次所有已发送目的地的托盘，按 send_time DESC（最近的在前）
        List<ProducePallet> sentList = producePalletMapper.selectSentByBatchIdDesc(batchId);
        // 正序遍历（已按 send_time DESC，第一条就是最近发送的）
        for (ProducePallet p : sentList) {
            String code = p.getSendDestinationCode();
            if (code != null && code.length() > 0) {
                String lastChar = code.substring(code.length() - 1);
                if ("1".equals(lastChar)) {
                    return "2";
                } else if ("2".equals(lastChar)) {
                    return "1";
                }
                // 后缀为3时不参与交替，继续向前追溯
            }
        }
        // 无历史记录或历史全为3，默认为2（先发送xxxx2，再发送xxxx1）
        return "2";
    }

    @Override
    @Transactional
    public PalletDetailDTO assignVirtualId(ProducePallet po) {
        Long palletId = po.getId();
        String virtualId = po.getVirtualId();
        if (virtualId == null || virtualId.isEmpty()) {
            throw new RuntimeException("虚拟ID不能为空");
        }
        ProducePallet pallet = producePalletMapper.selectById(palletId);
        if (pallet == null || "1".equals(pallet.getInvalidFlag())) {
            throw new RuntimeException("托盘不存在: " + palletId);
        }
        if (pallet.getVirtualId() != null && !pallet.getVirtualId().isEmpty()) {
            throw new RuntimeException("该托盘已分配虚拟ID: " + pallet.getVirtualId());
        }

        ProduceBatch batch = produceBatchMapper.selectById(pallet.getBatchId());
        if (batch == null || (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus()))) {
            throw new RuntimeException("托盘所属批次未处于运行状态，无法分配虚拟ID");
        }

        producePalletMapper.assignVirtualId(palletId, virtualId);

        // 回查更新后的托盘
        ProducePallet updated = producePalletMapper.selectById(palletId);
        List<ProduceGoods> goods = produceGoodsMapper.selectByPalletId(palletId);
        return PalletDetailDTO.from(updated, goods);
    }

    @Override
    @Transactional
    public PalletDetailDTO matchAndAssignVirtualId(MatchAndAssignDTO dto) {
        Long batchId = dto.getBatchId();
        List<String> barcodes = dto.getBarcodes();
        if (barcodes == null || barcodes.isEmpty()) {
            return null;
        }
        // 去重：扫码枪可能重复扫同一条码；收紧规则以“去重条码数”做全归属校验，避免重复导致 HAVING 计数对不上而误拦
        List<String> distinctBarcodes = new ArrayList<String>(new LinkedHashSet<String>(barcodes));
        // 1. 校验批次是否处于运行状态
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null || (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus()))) {
            throw new RuntimeException("批次未处于运行状态，无法分配虚拟ID");
        }

        // 2. 匹配托盘：提交条码必须“全部”归属当前批次同一个未上货托盘，否则不匹配（错扫/混托均拒绝上货）
        ProducePallet matchedPallet = producePalletMapper.selectUnassignedByBarcodes(
                batchId, distinctBarcodes, distinctBarcodes.size());
        if (matchedPallet == null) {
            // 逐条码查实时库归因，抛业务异常把详细原因带回前端展示；code 与原 fail() 一致(201)，仅 message 变明细。
            // 失败路径无任何写库，@Transactional 回滚无副作用。
            String reason = buildNoMatchReason(batchId, distinctBarcodes);
            // 留痕：仅记条码数量，明细原因由 ExceptionAdvice 统一打印，避免此处重复刷长日志
            log.info("分配虚拟ID未匹配到托盘: batchId={}, 条码数={}", batchId, distinctBarcodes.size());
            throw BusinessException.build("produce", ResultCodeEnum.FAIL.getCode(), reason);
        }

        // 3. 生成下一个虚拟ID：当日已分配的最大虚拟ID + 1（跨批次不重复，次日从10000重新开始）
        int nextVirtualId = nextVirtualIdToday();

        // 4. 持久化虚拟ID到数据库
        String virtualIdStr = String.valueOf(nextVirtualId);
        producePalletMapper.assignVirtualId(matchedPallet.getId(), virtualIdStr);

        // 5. 更新所有匹配条码的扫码状态（01002已扫码）
        int scannedRows = 0;
        for (String uid : distinctBarcodes) {
            scannedRows += produceGoodsMapper.markScanned(uid, "01002", batchId);
        }

        // 留痕：一次分配一条汇总日志，收紧后 scannedRows 应等于去重条码数（全部同批次同托盘）
        log.info("分配虚拟ID成功: batchId={}, palletId={}, palletNo={}, virtualId={}, 条码数={}, 标记已扫{}条",
                batchId, matchedPallet.getId(), matchedPallet.getPalletNo(), virtualIdStr,
                distinctBarcodes.size(), scannedRows);

        // 6. 回查更新后的托盘
        ProducePallet updated = producePalletMapper.selectById(matchedPallet.getId());
        List<ProduceGoods> goods = produceGoodsMapper.selectByPalletId(matchedPallet.getId());
        return PalletDetailDTO.from(updated, goods);
    }

    /**
     * 匹配失败逐条码归因：查实时库，把每个提交条码归类，严格对应收紧后的命中条件
     * （全部条码须同属“当前批次 + 未作废 + 未分配虚拟ID”的同一个托盘）。
     * 失败主因分两类：① 有条码不合格（不存在/不属本批次/货物或托盘作废/托盘已上货）；
     * ② 全部合格但分属多个托盘（混托）。
     * 性能：固定 2 次批量查询（selectByUids 走 uid 索引 + selectBatchIds 走主键），
     * 结果条数只与提交条码数相关，不随托盘/货物大表体量增长，不逐条码查库。
     */
    private String buildNoMatchReason(Long batchId, List<String> barcodes) {
        // 1. 一次批量查所有提交条码对应的货物（不过滤批次/作废，拿到完整现场）
        List<ProduceGoods> goodsList = produceGoodsMapper.selectByUids(barcodes);
        Map<String, List<ProduceGoods>> goodsByUid = new HashMap<String, List<ProduceGoods>>();
        Set<Long> palletIds = new HashSet<Long>();
        if (goodsList != null) {
            for (ProduceGoods g : goodsList) {
                List<ProduceGoods> list = goodsByUid.get(g.getUid());
                if (list == null) {
                    list = new ArrayList<ProduceGoods>();
                    goodsByUid.put(g.getUid(), list);
                }
                list.add(g);
                if (g.getPalletId() != null) {
                    palletIds.add(g.getPalletId());
                }
            }
        }
        // 2. 一次批量查涉及到的托盘（selectBatchIds 走主键，空集合需守卫，避免无效 IN 查询）
        Map<Long, ProducePallet> palletById = new HashMap<Long, ProducePallet>();
        if (!palletIds.isEmpty()) {
            List<ProducePallet> pallets = producePalletMapper.selectBatchIds(palletIds);
            if (pallets != null) {
                for (ProducePallet p : pallets) {
                    palletById.put(p.getId(), p);
                }
            }
        }
        // 3. 逐条码归因：不合格条码按原因分组；合格条码按所属托盘分组（用于识别混托）
        Map<String, List<String>> reasonToUids = new LinkedHashMap<String, List<String>>();
        Map<Long, List<String>> okPalletToUids = new LinkedHashMap<Long, List<String>>();
        for (String uid : barcodes) {
            List<ProduceGoods> rows = goodsByUid.get(uid);
            String reason = attributeOne(uid, batchId, rows, palletById);
            if (reason != null) {
                addGroup(reasonToUids, reason, uid);
            } else {
                addGroup(okPalletToUids, inBatchPalletId(batchId, rows), uid);
            }
        }
        // 4. 拼装消息：优先暴露不合格条码；全合格时再区分混托/并发
        List<String> parts = new ArrayList<String>();
        if (!reasonToUids.isEmpty()) {
            for (Map.Entry<String, List<String>> e : reasonToUids.entrySet()) {
                parts.add(e.getKey() + formatUids(e.getValue()));
            }
        } else if (okPalletToUids.size() >= 2) {
            StringBuilder mix = new StringBuilder("条码分属")
                    .append(okPalletToUids.size()).append("个不同托盘(一次只能上货一个)：");
            boolean first = true;
            for (Map.Entry<Long, List<String>> e : okPalletToUids.entrySet()) {
                if (!first) {
                    mix.append("、");
                }
                first = false;
                ProducePallet p = palletById.get(e.getKey());
                String pno = (p != null && p.getPalletNo() != null && !p.getPalletNo().isEmpty())
                        ? p.getPalletNo() : String.valueOf(e.getKey());
                mix.append("托盘").append(pno).append(formatUids(e.getValue()));
            }
            parts.add(mix.toString());
        } else {
            List<String> uids = okPalletToUids.isEmpty()
                    ? barcodes : okPalletToUids.values().iterator().next();
            parts.add("托盘状态正常却未命中(可能并发,请重试)" + formatUids(uids));
        }
        return "条码匹配失败：" + String.join("；", parts);
    }

    /**
     * 对单个条码归因：返回 null 表示该条码归属一个合格托盘（本批次 + 货物未作废 + 托盘未作废 + 未分配虚拟ID）；
     * 否则返回不合格原因。同一条码跨批次存在多行时，优先按当前批次的货物行判定。
     */
    private String attributeOne(String uid, Long batchId, List<ProduceGoods> rows, Map<Long, ProducePallet> palletById) {
        if (rows == null || rows.isEmpty()) {
            return "条码不存在";
        }
        // 优先取当前批次的货物行
        ProduceGoods inBatch = null;
        for (ProduceGoods g : rows) {
            if (batchId != null && batchId.equals(g.getBatchId())) {
                inBatch = g;
                break;
            }
        }
        if (inBatch == null) {
            return "不属于当前批次";
        }
        if ("1".equals(inBatch.getInvalidFlag())) {
            return "货物已作废";
        }
        ProducePallet pallet = inBatch.getPalletId() == null ? null : palletById.get(inBatch.getPalletId());
        if (pallet == null) {
            return "所属托盘不存在";
        }
        if ("1".equals(pallet.getInvalidFlag())) {
            return "所属托盘已作废";
        }
        if (pallet.getVirtualId() != null && !pallet.getVirtualId().isEmpty()) {
            return "所属托盘已分配虚拟ID(疑似已上货,虚拟ID=" + pallet.getVirtualId() + ")";
        }
        // 合格：本批次 + 未作废 + 未分配虚拟ID
        return null;
    }

    /** 取该条码在当前批次的货物行所属托盘ID（合格条码用） */
    private Long inBatchPalletId(Long batchId, List<ProduceGoods> rows) {
        if (rows != null) {
            for (ProduceGoods g : rows) {
                if (batchId != null && batchId.equals(g.getBatchId())) {
                    return g.getPalletId();
                }
            }
        }
        return null;
    }

    /** 归因分组累加 */
    private <K> void addGroup(Map<K, List<String>> map, K key, String uid) {
        List<String> list = map.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(key, list);
        }
        list.add(uid);
    }

    /** 条码列表格式化：(N条)[...]，最多列 20 个，超出以「…等N条」收尾 */
    private String formatUids(List<String> uids) {
        String body = uids.size() <= 20
                ? String.join(",", uids)
                : String.join(",", uids.subList(0, 20)) + "…等" + uids.size() + "条";
        return "(" + uids.size() + "条)[" + body + "]";
    }

    /**
     * 按当日已分配虚拟ID递增生成下一个ID（10000-29999）。
     * 跨批次共用同一序号，当日不允许重复；次日从 10000 重新开始。
     * 回绕到 10000 时跳过当日已占用的号。
     */
    private int nextVirtualIdToday() {
        List<Integer> usedIds = producePalletMapper.selectUsedVirtualIdsToday();
        Set<Integer> usedSet = new HashSet<Integer>();
        int maxId = 9999;
        if (usedIds != null) {
            for (Integer id : usedIds) {
                if (id == null) {
                    continue;
                }
                usedSet.add(id);
                if (id > maxId) {
                    maxId = id;
                }
            }
        }
        int next = maxId >= 29999 ? 10000 : maxId + 1;
        if (next < 10000) {
            next = 10000;
        }
        int start = next;
        while (usedSet.contains(next)) {
            next++;
            if (next > 29999) {
                next = 10000;
            }
            if (next == start) {
                throw new RuntimeException("当日虚拟ID已用尽（10000-29999）");
            }
        }
        return next;
    }

    @Override
    public PalletDetailDTO getByGoodsUid(String uid) {
        // 1. 根据 UID 查找货物
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid, null);
        if (goods == null) {
            return null;
        }
        // 2. 根据 palletId 查找托盘
        ProducePallet pallet = producePalletMapper.selectById(goods.getPalletId());
        if (pallet == null) {
            return null;
        }
        // 3. 根据 palletId 查找托盘下所有货物
        List<ProduceGoods> goodsList = produceGoodsMapper.selectByPalletId(pallet.getId());
        // 4. 返回 PalletDetailDTO
        return PalletDetailDTO.from(pallet, goodsList);
    }

    @Override
    @Transactional
    public PalletDetailDTO resendDestination(SendDestinationDTO dto) {
        Long palletId = dto.getPalletId();
        String virtualId = dto.getVirtualId();
        String destinationCode = dto.getDestinationCode();
        // 1. 定位托盘
        ProducePallet pallet = producePalletMapper.selectByIdAndVirtualId(palletId, virtualId);
        if (pallet == null) {
            throw new RuntimeException("托盘不存在或 virtualId 不匹配: palletId=" + palletId);
        }
        // 仅允许 999 异常托盘重新发送
        if (!"999".equals(pallet.getSendDestinationCode())) {
            throw new RuntimeException("仅允许999异常托盘重新发送目的地");
        }

        // 2. 重新汇总该托盘下所有 goods 的 scan_status（PDA扫码复检后可能有更新）
        List<ProduceGoods> goodsList = produceGoodsMapper.selectByPalletId(palletId);
        long scanned = goodsList.stream().filter(g -> "1".equals(g.getScanStatus())).count();
        String trayStatus;
        if (scanned == 0) {
            trayStatus = "0";
        } else if (scanned < goodsList.size()) {
            trayStatus = "1";
        } else {
            trayStatus = "2";
        }

        // 3. 计算1/2后缀
        String suffix = determineSuffixForFullScan(pallet.getBatchId());
        String sendCode = destinationCode + suffix;

        // 4. 更新数据库
        producePalletMapper.sendDestination(palletId, virtualId, trayStatus, sendCode);

        // 5. 复检改写 999 后，若全部已发真实目的地则自动完结
        produceBatchService.tryFinishIfAllSent(pallet.getBatchId());

        // 6. 回查
        ProducePallet updated = producePalletMapper.selectById(palletId);
        return PalletDetailDTO.from(updated, goodsList);
    }

    @Override
    @Transactional
    public PalletDetailDTO resetPallet(ProducePallet po) {
        Long palletId = po == null ? null : po.getId();
        if (palletId == null) {
            throw new RuntimeException("托盘ID不能为空");
        }
        ProducePallet pallet = producePalletMapper.selectById(palletId);
        if (pallet == null || "1".equals(pallet.getInvalidFlag())) {
            throw new RuntimeException("托盘不存在: " + palletId);
        }

        // 1. 托盘恢复建档初始状态：清空虚拟ID、上货信息、扫码汇总与目的地发送信息
        producePalletMapper.resetToArchived(palletId);
        // 2. 托盘下货物扫码信息回到未扫，uid、品名等建档数据保留
        int goodsRows = produceGoodsMapper.resetScanByPalletId(palletId);
        // 3. 批次若已自动完结则回退为生产中并恢复激活目的地，保证不影响下一次上货
        boolean reopened = produceBatchService.reopenIfFinished(pallet.getBatchId());

        // 留痕：记录重置前的关键现场信息，便于事后追溯
        log.info("托盘恢复建档状态: palletId={}, palletNo={}, batchId={}, 原虚拟ID={}, 原发送状态={}, 重置货物{}条, 批次回退={}",
                palletId, pallet.getPalletNo(), pallet.getBatchId(), pallet.getVirtualId(),
                pallet.getSendStatus(), goodsRows, reopened);

        // 4. 回查恢复后的托盘与货物
        ProducePallet updated = producePalletMapper.selectById(palletId);
        List<ProduceGoods> goods = produceGoodsMapper.selectByPalletId(palletId);
        return PalletDetailDTO.from(updated, goods);
    }

    @Override
    @Transactional
    public Integer deletePallet(ProducePallet po) {
        Long palletId = po.getId();
        // 先删托盘下属所有货物，再删托盘本身
        produceGoodsMapper.deleteByPalletId(palletId);
        int rows = producePalletMapper.deletePalletById(palletId);
        if (rows == 0) {
            throw new RuntimeException("托盘不存在: " + palletId);
        }
        return rows;
    }
}
