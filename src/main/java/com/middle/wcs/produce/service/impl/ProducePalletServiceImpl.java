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
    public PalletDetailDTO sendDestination(Long palletId, String virtualId, String destinationCode) {
        // 1. palletId + virtualId 双条件定位托盘
        ProducePallet pallet = producePalletMapper.selectByIdAndVirtualId(palletId, virtualId);
        if (pallet == null) {
            throw new RuntimeException("托盘不存在或 virtualId 不匹配: palletId=" + palletId);
        }
        if ("1".equals(pallet.getSendStatus())) {
            throw new RuntimeException("该托盘已发送过目的地，请勿重复发送");
        }

        // 2. 汇总该托盘下所有 goods 的 scan_status
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

        // 3. 确定发送目的地值：全扫=原始编码，未全扫=编码拼"3"
        String sendCode = "2".equals(trayStatus) ? destinationCode : (destinationCode + "3");

        // 4. 回写托盘（palletId + virtualId 双条件）
        producePalletMapper.sendDestination(palletId, virtualId, trayStatus, sendCode);

        // 5. 回查更新后数据
        ProducePallet updated = producePalletMapper.selectById(palletId);
        return PalletDetailDTO.from(updated, goodsList);
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
}
