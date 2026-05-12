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

        // 3. 确定目的地后缀：未全扫→3，全扫→1/2交替（3不参与交替，需向前追溯）
        String suffix;
        if (!"2".equals(trayStatus)) {
            // 未全部扫描完成，后缀固定为3
            suffix = "3";
        } else {
            // 全部扫描完成，根据当前批次上一个已发送目的地托盘的后缀交替
            suffix = determineSuffixForFullScan(pallet.getBatchId());
        }

        // 4. 构造发送目的地编码：原始编码 + 后缀（如 3201 + 1 = 32011）
        String sendCode = destinationCode + suffix;

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
     * 若无历史记录则默认为1。
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
        // 无历史记录或历史全为3，默认为1
        return "1";
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
