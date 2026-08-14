package com.middle.wcs.produce.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.middle.wcs.produce.dao.ProduceBatchDestinationMapper;
import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.dao.ProducePalletMapper;
import com.middle.wcs.produce.entity.dto.BatchDetailDTO;
import com.middle.wcs.produce.entity.dto.BatchHistoryQueryDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceBatchDestination;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.middle.wcs.produce.service.ProduceBatchService;
import com.middle.wcs.hander.BusinessException;
import com.middle.wcs.hander.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.pagehelper.page.PageMethod.startPage;

/**
 * 生产批次 Service 实现
 */
@Service
public class ProduceBatchServiceImpl implements ProduceBatchService {

    @Resource
    private ProduceBatchMapper produceBatchMapper;

    @Resource
    private ProducePalletMapper producePalletMapper;

    @Resource
    private ProduceGoodsMapper produceGoodsMapper;

    @Resource
    private ProduceBatchDestinationMapper produceBatchDestinationMapper;

    @Override
    public BatchDetailDTO getByGoodsUid(String uid) {
        // 1. 按 uid 精确查找货物
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid, null);
        if (goods == null) {
            return null;
        }

        // 2. 查找批次
        ProduceBatch batch = produceBatchMapper.selectById(goods.getBatchId());
        if (batch == null) {
            return null;
        }

        // 3. 查找该批次下所有托盘
        List<ProducePallet> pallets = producePalletMapper.selectByBatchId(batch.getId());

        // 4. 逐个托盘查货物，组装 DTO
        List<PalletDetailDTO> palletDTOs = new ArrayList<>();
        for (ProducePallet pallet : pallets) {
            List<ProduceGoods> goodsList = produceGoodsMapper.selectByPalletId(pallet.getId());
            palletDTOs.add(PalletDetailDTO.from(pallet, goodsList));
        }

        return BatchDetailDTO.of(batch, palletDTOs);
    }

    @Override
    @Transactional
    public BatchDetailDTO save(BatchDetailDTO dto) {
        // 1. 插入批次
        ProduceBatch batch = dto.getBatch();
        batch.setStatus("0");
        batch.setInvalidFlag("0");
        batch.setCreatedAt(new Date());
        produceBatchMapper.insert(batch);
        Long batchId = batch.getId();

        List<PalletDetailDTO> palletDTOs = dto.getPallets();
        if (palletDTOs == null) {
            palletDTOs = new ArrayList<>();
        }

        List<PalletDetailDTO> savedPalletDTOs = new ArrayList<>();
        // 汇总所有货物，最后批量插入（避免逐条 insert 的大量网络往返）
        List<ProduceGoods> allGoods = new ArrayList<>();
        Date now = new Date();

        for (PalletDetailDTO palletDTO : palletDTOs) {
            // 2. 插入托盘
            ProducePallet pallet = new ProducePallet();
            pallet.setBatchId(batchId);
            pallet.setPalletNo(palletDTO.getPalletNo() != null ? palletDTO.getPalletNo() : "");
            pallet.setToWarehouse(palletDTO.getToWarehouse());
            pallet.setTrayStatus("0");
            pallet.setInvalidFlag("0");
            pallet.setCreatedAt(now);
            producePalletMapper.insert(pallet);
            Long palletId = pallet.getId();

            // 3. 组装货物（手动分配雪花ID，收集后统一批量插入）
            List<ProduceGoods> goodsList = palletDTO.getGoods();
            if (goodsList != null && !goodsList.isEmpty()) {
                for (ProduceGoods g : goodsList) {
                    g.setId(IdWorker.getId());
                    g.setBatchId(batchId);
                    g.setPalletId(palletId);
                    g.setScanStatus("0");
                    g.setInvalidFlag("0");
                    // created_at 由数据库默认 SYSDATETIME() 生成，这里不赋值
                    allGoods.add(g);
                }
            }

            savedPalletDTOs.add(PalletDetailDTO.from(pallet, goodsList != null ? goodsList : new ArrayList<>()));
        }

        // 4. 批量插入货物（分片，规避 SQL Server 单语句参数上限）
        if (!allGoods.isEmpty()) {
            final int chunkSize = 100;
            for (int from = 0; from < allGoods.size(); from += chunkSize) {
                int to = Math.min(from + chunkSize, allGoods.size());
                produceGoodsMapper.insertBatch(allGoods.subList(from, to));
            }
        }

        return BatchDetailDTO.of(batch, savedPalletDTOs);
    }

    @Override
    public BatchDetailDTO getCurrentExecuting() {
        ProduceBatch batch = produceBatchMapper.selectCurrentExecuting();
        if (batch == null) {
            return null;
        }
        return buildBatchDetail(batch);
    }

    @Override
    public BatchDetailDTO getBatchDetailById(Long batchId) {
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            return null;
        }
        return buildBatchDetail(batch);
    }

    @Override
    public PageInfo<ProduceBatch> selectListByPage(BatchHistoryQueryDTO query) {
        if (query == null) {
            query = new BatchHistoryQueryDTO();
        }
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        Page<ProduceBatch> page = startPage(pageNum, pageSize);
        produceBatchMapper.selectListByPage(query);
        return new PageInfo<>(page);
    }

    /**
     * 组装批次详情（批次 + 托盘 + 货物嵌套）
     */
    private BatchDetailDTO buildBatchDetail(ProduceBatch batch) {
        List<ProducePallet> pallets = producePalletMapper.selectByBatchId(batch.getId());
        List<PalletDetailDTO> palletDTOs = new ArrayList<>();
        for (ProducePallet pallet : pallets) {
            List<ProduceGoods> goodsList = produceGoodsMapper.selectByPalletId(pallet.getId());
            palletDTOs.add(PalletDetailDTO.from(pallet, goodsList));
        }
        return BatchDetailDTO.of(batch, palletDTOs);
    }

    @Override
    public Integer confirm(ProduceBatch po) {
        Long batchId = po.getId();
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        // 限定同一时间只能执行一个批次：若已有其他批次 status=1 或 2，则拒绝
        int count = produceBatchMapper.countExecutingExcludeBatch(batchId);
        if (count > 0) {
            throw BusinessException.build(CommonErrorCode.BATCH_ALREADY_EXECUTING);
        }
        batch.setStatus("1");
        batch.setConfirmTime(new Date());
        return produceBatchMapper.updateById(batch);
    }

    @Override
    @Transactional
    public Integer cancel(ProduceBatch po) {
        Long batchId = po.getId();
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        if (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus())) {
            throw new RuntimeException("当前批次状态不可取消: " + batch.getStatus());
        }
        batch.setStatus("0");
        batch.setConfirmTime(null);
        int rows = produceBatchMapper.updateById(batch);

        // 同步取消该批次当前激活的目的地设置
        cancelActiveDestination(batchId);
        return rows;
    }

    @Override
    @Transactional
    public Integer finish(ProduceBatch po) {
        Long batchId = po.getId();
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        // 已完成：幂等直接返回
        if ("3".equals(batch.getStatus())) {
            return 0;
        }
        if (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus())) {
            throw new RuntimeException("当前批次状态不可完成: " + batch.getStatus());
        }
        return doFinish(batch);
    }

    @Override
    @Transactional
    public boolean tryFinishIfAllSent(Long batchId) {
        if (batchId == null) {
            return false;
        }
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            return false;
        }
        if ("3".equals(batch.getStatus())) {
            return true;
        }
        if (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus())) {
            return false;
        }
        List<ProducePallet> pallets = producePalletMapper.selectByBatchId(batchId);
        if (pallets == null || pallets.isEmpty()) {
            return false;
        }
        boolean allSentRealDest = pallets.stream().allMatch(this::hasRealDestination);
        if (!allSentRealDest) {
            return false;
        }
        doFinish(batch);
        return true;
    }

    /** 已发送且编码不是 999 异常 */
    private boolean hasRealDestination(ProducePallet pallet) {
        if (!"1".equals(pallet.getSendStatus())) {
            return false;
        }
        String code = pallet.getSendDestinationCode();
        return code != null && !code.isEmpty() && !"999".equals(code);
    }

    /**
     * 将批次标记为完成，并取消激活目的地
     */
    private Integer doFinish(ProduceBatch batch) {
        batch.setStatus("3");
        batch.setFinishTime(new Date());
        int rows = produceBatchMapper.updateById(batch);
        cancelActiveDestination(batch.getId());
        return rows;
    }

    private void cancelActiveDestination(Long batchId) {
        ProduceBatchDestination activeDest = produceBatchDestinationMapper.selectActiveByBatchId(batchId);
        if (activeDest != null) {
            activeDest.setStatus("1");
            activeDest.setCancelTime(new Date());
            produceBatchDestinationMapper.updateById(activeDest);
        }
    }

    @Override
    @Transactional
    public PalletDetailDTO addPallet(ProducePallet po) {
        Long batchId = po.getBatchId();
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        // 根据批次下已有托盘数量生成托盘号
        List<ProducePallet> existingPallets = producePalletMapper.selectByBatchId(batchId);
        int nextNo = (existingPallets == null ? 0 : existingPallets.size()) + 1;
        String palletNo = "TP-" + String.format("%02d", nextNo);

        ProducePallet pallet = new ProducePallet();
        pallet.setBatchId(batchId);
        pallet.setPalletNo(palletNo);
        pallet.setTrayStatus("0");
        pallet.setInvalidFlag("0");
        pallet.setCreatedAt(new Date());
        producePalletMapper.insert(pallet);

        return PalletDetailDTO.from(pallet, new ArrayList<>());
    }

    @Override
    @Transactional
    public ProduceGoods addGoods(ProduceGoods po) {
        Long batchId = po.getBatchId();
        Long palletId = po.getPalletId();
        String uid = po.getUid() != null ? po.getUid().trim() : null;
        if (uid == null || uid.isEmpty()) {
            throw new RuntimeException("货物UID不能为空");
        }
        // 校验批次和托盘存在
        ProducePallet pallet = producePalletMapper.selectById(palletId);
        if (pallet == null) {
            throw new RuntimeException("托盘不存在: " + palletId);
        }
        if (!batchId.equals(pallet.getBatchId())) {
            throw new RuntimeException("托盘不属于指定批次");
        }

        ProduceGoods goods = new ProduceGoods();
        goods.setBatchId(batchId);
        goods.setPalletId(palletId);
        goods.setUid(uid);
        // 模拟数据：品名和规格使用固定模板
        goods.setProductName("一次性口罩");
        goods.setSpec("1000个/箱");
        goods.setRemark("");
        goods.setScanStatus("0");
        goods.setInvalidFlag("0");
        goods.setCreatedAt(new Date());
        produceGoodsMapper.insert(goods);

        return goods;
    }

    @Override
    public Integer updateSterilizerNameCode(ProduceBatch po) {
        if (po == null || po.getId() == null) {
            throw new RuntimeException("批次ID不能为空");
        }
        ProduceBatch batch = produceBatchMapper.selectById(po.getId());
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + po.getId());
        }
        String code = po.getSterilizerNameCode() != null ? po.getSterilizerNameCode().trim() : "";
        batch.setSterilizerNameCode(code);
        return produceBatchMapper.updateById(batch);
    }
}
