package com.middle.wcs.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.middle.wcs.order.entity.po.OrderInfo;

import java.util.List;

/**
 * (OrderInfo)表数据库访问层
 *
 * @author makejava
 * @since 2024-12-28 23:59:48
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
