package com.middle.wcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middle.wcs.order.entity.po.OrderInfo;
import com.middle.wcs.order.dao.OrderInfoMapper;
import com.middle.wcs.order.service.OrderInfoService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;

/**
 * (OrderInfo)服务实现类
 *
 * @author makejava
 * @since 2024-12-28 23:59:48
 */
@Service
public class OrderInfoServiceImpl implements OrderInfoService {
    
    @Resource
    private OrderInfoMapper orderInfoMapper;
    
    @Override
    public Integer save(OrderInfo orderInfo) {
        return orderInfoMapper.insert(orderInfo);
    }
    
    @Override
    public Integer update(OrderInfo orderInfo) {
        return orderInfoMapper.updateById(orderInfo);
    }
    
    @Override
    public OrderInfo getOrderInfoById(Long id) {
        return orderInfoMapper.selectById(id);
    }

    @Override
    public List<OrderInfo> selectByList(OrderInfo dto) {
        QueryWrapper<OrderInfo> wrapper= new QueryWrapper<>(dto);
        return this.orderInfoMapper.selectList(wrapper);
    }
}
