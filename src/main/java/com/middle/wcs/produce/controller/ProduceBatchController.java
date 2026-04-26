package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.dto.BatchDetailDTO;
import com.middle.wcs.produce.service.ProduceBatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 生产批次控制器
 */
@Api(tags = "生产批次接口")
@RestController
@RequestMapping("/produce_batch")
public class ProduceBatchController {

    @Resource
    private ProduceBatchService produceBatchService;

    @ApiOperation("获取当前正在执行的批次（status=1或2），若无则返回null")
    @GetMapping("/getCurrentExecuting")
    public ResponseResult<BatchDetailDTO> getCurrentExecuting() {
        return ResponseResult.success(produceBatchService.getCurrentExecuting());
    }

    @ApiOperation("根据货物UID查询批次详情（批次+托盘+货物嵌套）")
    @GetMapping("/getByGoodsUid")
    public ResponseResult<BatchDetailDTO> getByGoodsUid(
            @ApiParam(value = "货物唯一码", required = true) @RequestParam String uid) {
        return ResponseResult.success(produceBatchService.getByGoodsUid(uid));
    }

    @ApiOperation("保存批次（批次+托盘+货物，事务）")
    @PostMapping("/save")
    public ResponseResult<BatchDetailDTO> save(
            @ApiParam(value = "批次详情", required = true) @RequestBody BatchDetailDTO dto) {
        return ResponseResult.success(produceBatchService.save(dto));
    }

    @ApiOperation("确认批次，状态更新为已确认")
    @PostMapping("/confirm")
    public ResponseResult<Void> confirm(
            @ApiParam(value = "包含 batchId 的请求体", required = true) @RequestBody Map<String, Long> body) {
        Long batchId = body.get("batchId");
        if (batchId == null) {
            return ResponseResult.fail();
        }
        produceBatchService.confirm(batchId);
        return ResponseResult.success();
    }

    @ApiOperation("取消执行批次，状态更新为待确认")
    @PostMapping("/cancel")
    public ResponseResult<Void> cancel(
            @ApiParam(value = "包含 batchId 的请求体", required = true) @RequestBody Map<String, Long> body) {
        Long batchId = body.get("batchId");
        if (batchId == null) {
            return ResponseResult.fail();
        }
        produceBatchService.cancel(batchId);
        return ResponseResult.success();
    }
}
