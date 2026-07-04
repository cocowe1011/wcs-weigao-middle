package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.service.ProduceGoodsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 生产货物控制器
 */
@Api(tags = "生产货物接口")
@RestController
@RequestMapping("/produce_goods")
public class ProduceGoodsController {

    @Resource
    private ProduceGoodsService produceGoodsService;

    @ApiOperation("扫码回写：将货物标记为已扫码（scan_status=1，覆盖 scan_location/scan_time）")
    @PostMapping("/markScanned")
    public ResponseResult<Integer> markScanned(
            @ApiParam(value = "货物信息", required = true) @RequestBody ProduceGoods po) {
        return ResponseResult.success(produceGoodsService.markScanned(po));
    }

    @ApiOperation("真删货物：按主键ID删除单条货物记录")
    @PostMapping("/delete")
    public ResponseResult<Integer> deleteGoods(
            @ApiParam(value = "货物信息", required = true) @RequestBody ProduceGoods po) {
        return ResponseResult.success(produceGoodsService.deleteById(po));
    }
}
