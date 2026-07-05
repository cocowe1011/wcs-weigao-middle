package com.middle.wcs.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middle.wcs.order.entity.po.QueueInfo;
import com.middle.wcs.order.dao.QueueInfoMapper;
import com.middle.wcs.order.service.QueueInfoService;
import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.dao.ProducePalletMapper;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.pagehelper.page.PageMethod.startPage;

/**
 * (QueueInfo)表服务实现类
 *
 * @author makejava
 * @since 2025-01-01 12:44:45
 */
@Service("queueInfoService")
public class QueueInfoServiceImpl implements QueueInfoService {

    @Resource
    private QueueInfoMapper queueInfoMapper;

    @Resource
    private ProduceBatchMapper produceBatchMapper;

    @Resource
    private ProducePalletMapper producePalletMapper;

    @Resource
    private ProduceGoodsMapper produceGoodsMapper;


    /**
     * 修改数据
     *
     * @param entity 实例对象
     * @return 实例对象
     */
    @Override
    public int update(QueueInfo entity) {
        return this.queueInfoMapper.updateById(entity);
    }

    @Override
    public List<QueueInfo> queryQueueList() {
        QueueInfo queueInfo = new QueueInfo();
        QueryWrapper<QueueInfo> wrapper= new QueryWrapper<>(queueInfo);
        return queueInfoMapper.selectList(wrapper);
    }

    @Override
    public QueueInfo getQueueInfoById(Long id) {
        return queueInfoMapper.selectById(id);
    }

    @Override
    public Map<String, Object> getProductInfo(String queueCode) {
        // data 数据体：{ queueNum, pallet_list:[...] }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("queueNum", queueCode);
        List<Map<String, Object>> palletList = new ArrayList<>();
        data.put("pallet_list", palletList);

        if (queueCode == null || queueCode.trim().isEmpty()) {
            return data;
        }

        // 按队列名（预热柜/灭菌柜编号）查询队列
        QueryWrapper<QueueInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("queue_name", queueCode.trim());
        QueueInfo queue = queueInfoMapper.selectOne(wrapper);
        if (queue == null || queue.getTrayInfo() == null || queue.getTrayInfo().trim().isEmpty()) {
            return data;
        }

        // 解析 tray_info（前端存的是 trayInfo 数组的 JSON 串），按顺序收集去重后的 palletId
        JSONArray trays;
        try {
            trays = JSON.parseArray(queue.getTrayInfo());
        } catch (Exception e) {
            return data;
        }
        if (trays == null || trays.isEmpty()) {
            return data;
        }
        Set<Long> palletIds = new LinkedHashSet<>();
        for (int i = 0; i < trays.size(); i++) {
            JSONObject tray = trays.getJSONObject(i);
            if (tray == null) {
                continue;
            }
            Long palletId = tray.getLong("palletId");
            if (palletId != null) {
                palletIds.add(palletId);
            }
        }
        if (palletIds.isEmpty()) {
            return data;
        }
        List<Long> palletIdList = new ArrayList<>(palletIds);

        // 1. 批量查托盘，过滤作废，按 id 建索引
        Map<Long, ProducePallet> palletMapById = new HashMap<>();
        Set<Long> batchIds = new LinkedHashSet<>();
        List<ProducePallet> pallets = producePalletMapper.selectBatchIds(palletIdList);
        if (pallets != null) {
            for (ProducePallet p : pallets) {
                if (p == null || "1".equals(p.getInvalidFlag())) {
                    continue;
                }
                palletMapById.put(p.getId(), p);
                if (p.getBatchId() != null) {
                    batchIds.add(p.getBatchId());
                }
            }
        }

        // 2. 批量查批次，按 id 建索引（提供订单号与工艺方案）
        Map<Long, ProduceBatch> batchMapById = new HashMap<>();
        if (!batchIds.isEmpty()) {
            List<ProduceBatch> batches = produceBatchMapper.selectBatchIds(new ArrayList<>(batchIds));
            if (batches != null) {
                for (ProduceBatch b : batches) {
                    if (b != null) {
                        batchMapById.put(b.getId(), b);
                    }
                }
            }
        }

        // 3. 一次 IN 查询批量取货物，按 palletId 分组
        Map<Long, List<ProduceGoods>> goodsByPalletId = new HashMap<>();
        List<ProduceGoods> allGoods = produceGoodsMapper.selectByPalletIds(palletIdList);
        if (allGoods != null) {
            for (ProduceGoods g : allGoods) {
                goodsByPalletId
                        .computeIfAbsent(g.getPalletId(), k -> new ArrayList<>())
                        .add(g);
            }
        }

        // 4. 按队列托盘顺序组装
        for (Long palletId : palletIdList) {
            ProducePallet pallet = palletMapById.get(palletId);
            if (pallet == null) {
                continue;
            }
            ProduceBatch batch = pallet.getBatchId() != null ? batchMapById.get(pallet.getBatchId()) : null;

            List<Map<String, Object>> materialDetails = new ArrayList<>();
            List<ProduceGoods> goodsList = goodsByPalletId.get(palletId);
            if (goodsList != null) {
                for (ProduceGoods g : goodsList) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("product_code", g.getProductCode());
                    m.put("product_name", g.getProductName());
                    m.put("product_specification", g.getSpec());
                    m.put("production_batch_number", g.getProductionBatchNumber());
                    m.put("production_date", g.getProductionDate());
                    m.put("udi", g.getUdi());
                    materialDetails.add(m);
                }
            }

            Map<String, Object> palletMap = new LinkedHashMap<>();
            palletMap.put("sterilization_order_no", batch != null ? batch.getSterilizationOrderNo() : null);
            palletMap.put("process_plan_name_code", batch != null ? batch.getProcessPlanNameCode() : null);
            // MSE 托盘编码直接存于 pallet_no
            palletMap.put("pallet_code", pallet.getPalletNo());
            palletMap.put("to_warehouse", "1".equals(pallet.getToWarehouse()));
            palletMap.put("material_details", materialDetails);
            palletList.add(palletMap);
        }

        return data;
    }

}
