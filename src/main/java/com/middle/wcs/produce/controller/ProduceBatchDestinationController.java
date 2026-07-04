package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.po.ProduceBatchDestination;
import com.middle.wcs.produce.service.ProduceBatchDestinationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 批次目的地设置控制器
 */
@Api(tags = "批次目的地接口")
@RestController
@RequestMapping("/produce_batch_destination")
public class ProduceBatchDestinationController {

    @Resource
    private ProduceBatchDestinationService destinationService;

    @ApiOperation("查询批次当前激活的目的地，不存在则返回 null")
    @GetMapping("/current")
    public ResponseResult<ProduceBatchDestination> current(
            @ApiParam(value = "批次ID", required = true) @RequestParam Long batchId) {
        return ResponseResult.success(destinationService.getCurrentByBatchId(batchId));
    }

    @ApiOperation("设置目的地（覆盖旧设置，要求批次已允许生产）")
    @PostMapping("/set")
    public ResponseResult<ProduceBatchDestination> set(
            @ApiParam(value = "目的地设置信息", required = true) @RequestBody ProduceBatchDestination po) {
        return ResponseResult.success(destinationService.set(po));
    }

    @ApiOperation("取消当前批次的目的地设置")
    @PostMapping("/cancel")
    public ResponseResult<Integer> cancel(
            @ApiParam(value = "目的地设置信息", required = true) @RequestBody ProduceBatchDestination po) {
        return ResponseResult.success(destinationService.cancel(po));
    }
}
