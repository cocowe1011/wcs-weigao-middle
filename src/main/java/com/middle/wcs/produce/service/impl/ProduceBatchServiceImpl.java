package com.middle.wcs.produce.service.impl;

import com.middle.wcs.produce.dao.ProduceBatchDestinationMapper;
import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.dao.ProducePalletMapper;
import com.middle.wcs.produce.entity.dto.BatchDetailDTO;
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
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid);
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

        for (PalletDetailDTO palletDTO : palletDTOs) {
            // 2. 插入托盘
            ProducePallet pallet = new ProducePallet();
            pallet.setBatchId(batchId);
            pallet.setPalletNo(palletDTO.getPalletNo() != null ? palletDTO.getPalletNo() : "");
            pallet.setTrayStatus("0");
            pallet.setInvalidFlag("0");
            pallet.setCreatedAt(new Date());
            producePalletMapper.insert(pallet);
            Long palletId = pallet.getId();

            // 3. 插入货物（逐条插入，MyBatis-Plus自动分配雪花ID）
            List<ProduceGoods> goodsList = palletDTO.getGoods();
            if (goodsList != null && !goodsList.isEmpty()) {
                for (ProduceGoods g : goodsList) {
                    g.setBatchId(batchId);
                    g.setPalletId(palletId);
                    g.setScanStatus("0");
                    g.setInvalidFlag("0");
                    g.setCreatedAt(new Date());
                    produceGoodsMapper.insert(g);
                }
            }

            savedPalletDTOs.add(PalletDetailDTO.from(pallet, goodsList != null ? goodsList : new ArrayList<>()));
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
        ProduceBatchDestination activeDest = produceBatchDestinationMapper.selectActiveByBatchId(batchId);
        if (activeDest != null) {
            activeDest.setStatus("1");
            activeDest.setCancelTime(new Date());
            produceBatchDestinationMapper.updateById(activeDest);
        }
        return rows;
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
}
