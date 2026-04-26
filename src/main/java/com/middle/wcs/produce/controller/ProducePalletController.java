package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.service.ProducePalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 生产托盘控制器
 */
@Api(tags = "生产托盘接口")
@RestController
@RequestMapping("/produce_pallet")
public class ProducePalletController {

    @Resource
    private ProducePalletService producePalletService;

    @ApiOperation("查询批次下所有托盘列表（含扫码状态、发送状态等完整字段）")
    @GetMapping("/listByBatchId")
    public ResponseResult<List<PalletDetailDTO>> listByBatchId(
            @ApiParam(value = "批次ID", required = true) @RequestParam Long batchId) {
        return ResponseResult.success(producePalletService.listByBatchId(batchId));
    }

    @ApiOperation("发送目的地：前端传入已定位的托盘信息，后端汇总扫码状态并回写目的地")
    @PostMapping("/sendDestination")
    public ResponseResult<PalletDetailDTO> sendDestination(
            @ApiParam(value = "包含 palletId(String)、virtualId、destinationCode 的请求体", required = true)
            @RequestBody Map<String, String> body) {
        Long palletId = Long.parseLong(body.get("palletId"));
        String virtualId = body.get("virtualId");
        String destinationCode = body.get("destinationCode");
        return ResponseResult.success(producePalletService.sendDestination(palletId, virtualId, destinationCode));
    }

    @ApiOperation("为托盘分配虚拟ID（触发写虚拟ID请求时调用）")
    @PostMapping("/assignVirtualId")
    public ResponseResult<PalletDetailDTO> assignVirtualId(
            @ApiParam(value = "包含 palletId(String) 和 virtualId 的请求体", required = true)
            @RequestBody Map<String, String> body) {
        Long palletId = Long.parseLong(body.get("palletId"));
        String virtualId = body.get("virtualId");
        if (virtualId == null || virtualId.isEmpty()) {
            return ResponseResult.fail();
        }
        return ResponseResult.success(producePalletService.assignVirtualId(palletId, virtualId));
    }
}
