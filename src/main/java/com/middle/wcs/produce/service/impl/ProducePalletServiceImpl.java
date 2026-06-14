package com.middle.wcs.produce.service.impl;

import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.dao.ProducePalletMapper;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.middle.wcs.produce.service.ProducePalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产托盘 Service 实现
 */
@Service
public class ProducePalletServiceImpl implements ProducePalletService {

    @Resource
    private ProducePalletMapper producePalletMapper;

    @Resource
    private ProduceBatchMapper produceBatchMapper;

    @Resource
    private ProduceGoodsMapper produceGoodsMapper;

    @Override
    public List<PalletDetailDTO> listByBatchId(Long batchId) {
        List<ProducePallet> pallets = producePalletMapper.selectByBatchId(batchId);
        List<PalletDetailDTO> result = new ArrayList<>();
        for (ProducePallet pallet : pallets) {
            List<ProduceGoods> goods = produceGoodsMapper.selectByPalletId(pallet.getId());
            result.add(PalletDetailDTO.from(pallet, goods));
        }
        return result;
    }

    @Override
    @Transactional
    public PalletDetailDTO sendDestination(Long palletId, String virtualId, String destinationCode, List<String> barcodes) {
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
                produceGoodsMapper.markScanned(uid, "01006");
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

        // 4. 确定发送目的地编码：全扫→destinationCode+1/2后缀，非全扫→"999"（异常标记，便于查询）
        String sendCode;
        if ("2".equals(trayStatus)) {
            String suffix = determineSuffixForFullScan(pallet.getBatchId());
            sendCode = destinationCode + suffix;
        } else {
            sendCode = "999";
        }

        // 5. 回写托盘（palletId + virtualId 双条件）
        producePalletMapper.sendDestination(palletId, virtualId, trayStatus, sendCode);

        // 6. 回查更新后数据
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
    public PalletDetailDTO assignVirtualId(Long palletId, String virtualId) {
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
    public PalletDetailDTO matchAndAssignVirtualId(Long batchId, List<String> barcodes) {
        // 1. 校验批次是否处于运行状态
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null || (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus()))) {
            throw new RuntimeException("批次未处于运行状态，无法分配虚拟ID");
        }

        // 2. 根据条码匹配批次中尚未分配虚拟ID的托盘
        ProducePallet matchedPallet = producePalletMapper.selectUnassignedByBarcodes(batchId, barcodes);
        if (matchedPallet == null) {
            return null;
        }

        // 3. 生成下一个虚拟ID：查询当前批次已分配的最大虚拟ID + 1
        Integer maxVirtualId = producePalletMapper.selectMaxVirtualIdByBatchId(batchId);
        int nextVirtualId;
        if (maxVirtualId == null || maxVirtualId < 10000) {
            nextVirtualId = 10000;
        } else if (maxVirtualId >= 29999) {
            // 超范围回绕到10000（需确认该范围内无冲突）
            nextVirtualId = 10000;
        } else {
            nextVirtualId = maxVirtualId + 1;
        }

        // 4. 持久化虚拟ID到数据库
        String virtualIdStr = String.valueOf(nextVirtualId);
        producePalletMapper.assignVirtualId(matchedPallet.getId(), virtualIdStr);

        // 5. 更新所有匹配条码的扫码状态（01002已扫码）
        for (String uid : barcodes) {
            produceGoodsMapper.markScanned(uid, "01002");
        }

        // 6. 回查更新后的托盘
        ProducePallet updated = producePalletMapper.selectById(matchedPallet.getId());
        List<ProduceGoods> goods = produceGoodsMapper.selectByPalletId(matchedPallet.getId());
        return PalletDetailDTO.from(updated, goods);
    }

    @Override
    public PalletDetailDTO getByGoodsUid(String uid) {
        // 1. 根据 UID 查找货物
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid);
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
    public PalletDetailDTO resendDestination(Long palletId, String virtualId, String destinationCode) {
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

        // 5. 回查
        ProducePallet updated = producePalletMapper.selectById(palletId);
        return PalletDetailDTO.from(updated, goodsList);
    }

    @Override
    @Transactional
    public void deletePallet(Long palletId) {
        // 先删托盘下属所有货物，再删托盘本身
        produceGoodsMapper.deleteByPalletId(palletId);
        int rows = producePalletMapper.deletePalletById(palletId);
        if (rows == 0) {
            throw new RuntimeException("托盘不存在: " + palletId);
        }
    }
}
