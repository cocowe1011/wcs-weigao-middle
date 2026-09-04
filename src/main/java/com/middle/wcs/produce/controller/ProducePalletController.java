package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.entity.dto.MatchAndAssignDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.dto.SendDestinationDTO;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.middle.wcs.produce.service.ProducePalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    @ApiOperation("发送目的地：前端传入已定位的托盘信息+01006扫码条码（barcodes可选，PDA端不传），后端汇总扫码状态并回写目的地。skipScanCheck=true时跳过扫码判断直接赋值目的地")
    @PostMapping("/sendDestination")
    public ResponseResult<PalletDetailDTO> sendDestination(
            @ApiParam(value = "发送目的地请求", required = true) @RequestBody SendDestinationDTO dto) {
        return ResponseResult.success(producePalletService.sendDestination(dto));
    }

    @ApiOperation("为托盘分配虚拟ID（触发写虚拟ID请求时调用）")
    @PostMapping("/assignVirtualId")
    public ResponseResult<PalletDetailDTO> assignVirtualId(
            @ApiParam(value = "托盘信息", required = true) @RequestBody ProducePallet po) {
        return ResponseResult.success(producePalletService.assignVirtualId(po));
    }

    @ApiOperation("根据扫描条码匹配托盘并分配虚拟ID（原子操作：匹配+按当日递增生成不重复虚拟ID+持久化）")
    @PostMapping("/matchAndAssignVirtualId")
    public ResponseResult<PalletDetailDTO> matchAndAssignVirtualId(
            @ApiParam(value = "匹配分配虚拟ID请求", required = true) @RequestBody MatchAndAssignDTO dto) {
        PalletDetailDTO result = producePalletService.matchAndAssignVirtualId(dto);
        if (result == null) {
            return ResponseResult.fail();
        }
        return ResponseResult.success(result);
    }

    @ApiOperation("故障托盘重新发送目的地（999托盘修正为正确目的地编码+1/2后缀）")
    @PostMapping("/resendDestination")
    public ResponseResult<PalletDetailDTO> resendDestination(
            @ApiParam(value = "重新发送目的地请求", required = true) @RequestBody SendDestinationDTO dto) {
        return ResponseResult.success(producePalletService.resendDestination(dto));
    }

    @ApiOperation("恢复建档状态：上货区删除托盘时，把托盘及其货物重置为刚建档时的状态（不删数据，不影响下一次上货）")
    @PostMapping("/reset")
    public ResponseResult<PalletDetailDTO> resetPallet(
            @ApiParam(value = "托盘信息", required = true) @RequestBody ProducePallet po) {
        return ResponseResult.success(producePalletService.resetPallet(po));
    }

    @ApiOperation("真删托盘：删除托盘及其下属所有货物")
    @PostMapping("/delete")
    public ResponseResult<Integer> deletePallet(
            @ApiParam(value = "托盘信息", required = true) @RequestBody ProducePallet po) {
        return ResponseResult.success(producePalletService.deletePallet(po));
    }
}
