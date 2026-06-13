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

    @ApiOperation("根据货物UID查询所属托盘信息（简化接口，仅返回托盘详情）")
    @GetMapping("/getByGoodsUid")
    public ResponseResult<PalletDetailDTO> getByGoodsUid(
            @ApiParam(value = "货物唯一码", required = true) @RequestParam String uid) {
        return ResponseResult.success(producePalletService.getByGoodsUid(uid));
    }

    @ApiOperation("发送目的地：前端传入已定位的托盘信息+01006扫码条码（barcodes可选，PDA端不传），后端汇总扫码状态并回写目的地")
    @PostMapping("/sendDestination")
    public ResponseResult<PalletDetailDTO> sendDestination(
            @ApiParam(value = "包含 palletId(String)、virtualId、destinationCode、barcodes(List<String>，可选) 的请求体", required = true)
            @RequestBody Map<String, Object> body) {
        Long palletId = Long.parseLong(body.get("palletId").toString());
        String virtualId = (String) body.get("virtualId");
        String destinationCode = (String) body.get("destinationCode");
        @SuppressWarnings("unchecked")
        List<String> barcodes = (List<String>) body.get("barcodes");
        return ResponseResult.success(producePalletService.sendDestination(palletId, virtualId, destinationCode, barcodes));
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

    @ApiOperation("根据扫描条码匹配托盘并分配虚拟ID（原子操作：匹配+生成递增虚拟ID+持久化）")
    @PostMapping("/matchAndAssignVirtualId")
    public ResponseResult<PalletDetailDTO> matchAndAssignVirtualId(
            @ApiParam(value = "包含 batchId(Long) 和 barcodes(List<String>) 的请求体", required = true)
            @RequestBody Map<String, Object> body) {
        Long batchId = Long.parseLong(body.get("batchId").toString());
        @SuppressWarnings("unchecked")
        List<String> barcodes = (List<String>) body.get("barcodes");
        if (barcodes == null || barcodes.isEmpty()) {
            return ResponseResult.fail();
        }
        PalletDetailDTO result = producePalletService.matchAndAssignVirtualId(batchId, barcodes);
        if (result == null) {
            return ResponseResult.fail();
        }
        return ResponseResult.success(result);
    }

    @ApiOperation("故障托盘重新发送目的地（999托盘修正为正确目的地编码+1/2后缀）")
    @PostMapping("/resendDestination")
    public ResponseResult<PalletDetailDTO> resendDestination(
            @ApiParam(value = "包含 palletId(String)、virtualId、destinationCode 的请求体", required = true)
            @RequestBody Map<String, Object> body) {
        Long palletId = Long.parseLong(body.get("palletId").toString());
        String virtualId = (String) body.get("virtualId");
        String destinationCode = (String) body.get("destinationCode");
        return ResponseResult.success(producePalletService.resendDestination(palletId, virtualId, destinationCode));
    }
}
