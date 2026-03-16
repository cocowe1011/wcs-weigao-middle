package com.middle.wcs.order.service;

import com.middle.wcs.order.entity.po.OrderInfo;
import java.util.List;

/**
 * (OrderInfo)服务接口
 *
 * @author makejava
 * @since 2024-12-28 23:59:48
 */
public interface OrderInfoService {
    
    /**
     * 保存订单信息
     *
     * @param orderInfo 订单信息
     */
    Integer save(OrderInfo orderInfo);
    
    /**
     * 更新订单信息
     *
     * @param orderInfo 订单信息
     */
    Integer update(OrderInfo orderInfo);
    
    /**
     * 根据id查询订单信息
     *
     * @param id 订单id
     * @return 订单信息
     */
    OrderInfo getOrderInfoById(Long id);

    List<OrderInfo> selectByList(OrderInfo dto);
}
