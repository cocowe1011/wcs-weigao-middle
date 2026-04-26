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
    public void markScanned(String uid, String scanLocation) {
        ProduceGoods goods = produceGoodsMapper.selectByUid(uid);
        if (goods == null) {
            throw new RuntimeException("货物不存在或已作废: " + uid);
        }
        int rows = produceGoodsMapper.markScanned(uid, scanLocation);
        if (rows == 0) {
            throw new RuntimeException("扫码更新失败: " + uid);
        }
    }
}
