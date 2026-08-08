package com.middle.wcs.produce.controller;

import com.github.pagehelper.PageInfo;
import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.dto.BatchDetailDTO;
import com.middle.wcs.produce.entity.dto.BatchHistoryQueryDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.middle.wcs.produce.service.ProduceBatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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

    @ApiOperation("根据批次ID查询批次详情（批次+托盘+货物嵌套）")
    @GetMapping("/getById")
    public ResponseResult<BatchDetailDTO> getById(
            @ApiParam(value = "批次ID", required = true) @RequestParam Long id) {
        return ResponseResult.success(produceBatchService.getBatchDetailById(id));
    }

    @ApiOperation("历史批次分页查询")
    @PostMapping("/selectListByPage")
    public ResponseResult<PageInfo<ProduceBatch>> selectListByPage(
            @ApiParam(value = "查询条件", required = true) @RequestBody BatchHistoryQueryDTO query) {
        return ResponseResult.success(produceBatchService.selectListByPage(query));
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
    public ResponseResult<Integer> confirm(
            @ApiParam(value = "批次信息", required = true) @RequestBody ProduceBatch po) {
        return ResponseResult.success(produceBatchService.confirm(po));
    }

    @ApiOperation("取消执行批次，状态更新为待确认")
    @PostMapping("/cancel")
    public ResponseResult<Integer> cancel(
            @ApiParam(value = "批次信息", required = true) @RequestBody ProduceBatch po) {
        return ResponseResult.success(produceBatchService.cancel(po));
    }

    @ApiOperation("完成批次，状态更新为完成（status=3）")
    @PostMapping("/finish")
    public ResponseResult<Integer> finish(
            @ApiParam(value = "批次信息", required = true) @RequestBody ProduceBatch po) {
        return ResponseResult.success(produceBatchService.finish(po));
    }

    @ApiOperation("向批次添加一个空托盘")
    @PostMapping("/addPallet")
    public ResponseResult<PalletDetailDTO> addPallet(
            @ApiParam(value = "托盘信息", required = true) @RequestBody ProducePallet po) {
        return ResponseResult.success(produceBatchService.addPallet(po));
    }

    @ApiOperation("向托盘添加一件货物")
    @PostMapping("/addGoods")
    public ResponseResult<ProduceGoods> addGoods(
            @ApiParam(value = "货物信息", required = true) @RequestBody ProduceGoods po) {
        return ResponseResult.success(produceBatchService.addGoods(po));
    }

    @ApiOperation("更新批次档案目的地（灭菌柜编码 sterilizerNameCode）")
    @PostMapping("/updateSterilizerNameCode")
    public ResponseResult<Integer> updateSterilizerNameCode(
            @ApiParam(value = "批次信息（id + sterilizerNameCode）", required = true) @RequestBody ProduceBatch po) {
        return ResponseResult.success(produceBatchService.updateSterilizerNameCode(po));
    }
}
