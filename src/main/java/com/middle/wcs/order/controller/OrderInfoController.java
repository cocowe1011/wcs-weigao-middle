package com.middle.wcs.order.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.order.entity.po.OrderInfo;
import com.middle.wcs.order.service.OrderInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * (OrderInfo)控制器
 *
 * @author makejava
 * @since 2024-12-28 23:59:48
 */

@Api(tags = "订单管理接口")
@RestController
@RequestMapping("/order_info")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;
    
    @ApiOperation("保存订单信息")
    @PostMapping("/save")
    public ResponseResult<Integer> save(@ApiParam(value = "订单信息", required = true) @RequestBody OrderInfo po) {
        return ResponseResult.success(this.orderInfoService.save(po));
    }
    
    @ApiOperation("更新订单信息")
    @PostMapping("/update")
    public ResponseResult<Integer> update(@ApiParam(value = "订单信息", required = true) @RequestBody OrderInfo po) {
        return ResponseResult.success(this.orderInfoService.update(po));
    }

    @ApiOperation("根据ID查询订单信息")
    @GetMapping("/getOrderInfoById")
    public ResponseResult<OrderInfo> getOrderInfoById(@ApiParam(value = "订单ID", required = true) Long id) {
        return ResponseResult.success(this.orderInfoService.getOrderInfoById(id));
    }
    /**
     * 入参查询
     * @param dto 入参
     * @return 出参
     */
    @ApiOperation("入参查询")
    @PostMapping("/selectByList")
    public ResponseResult<List<OrderInfo>> selectByList(@RequestBody OrderInfo dto) {
        return ResponseResult.success(this.orderInfoService.selectByList(dto));
    }
}
