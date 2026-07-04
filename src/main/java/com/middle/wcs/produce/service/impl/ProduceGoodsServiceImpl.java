package com.middle.wcs.produce.service.impl;

import com.middle.wcs.produce.dao.ProduceGoodsMapper;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.service.ProduceGoodsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 生产货物 Service 实现
 */
@Service
public class ProduceGoodsServiceImpl implements ProduceGoodsService {

    @Resource
    private ProduceGoodsMapper produceGoodsMapper;

    @Override
    public Integer markScanned(ProduceGoods po) {
        String uid = po.getUid();
        String scanLocation = po.getScanLocation();
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid);
        if (goods == null) {
            throw new RuntimeException("货物不存在或已作废: " + uid);
        }
        int rows = produceGoodsMapper.markScanned(uid, scanLocation);
        if (rows == 0) {
            throw new RuntimeException("扫码更新失败: " + uid);
        }
        return rows;
    }

    @Override
    public Integer deleteById(ProduceGoods po) {
        Long id = po.getId();
        if (id == null) {
            throw new RuntimeException("货物ID不能为空");
        }
        int rows = produceGoodsMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("货物不存在: " + id);
        }
        return rows;
    }
}
