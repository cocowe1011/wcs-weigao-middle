package com.middle.wcs.produce.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.produce.service.ProduceGoodsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

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
    public ResponseResult<Void> markScanned(
            @ApiParam(value = "包含 uid 和 scanLocation（01002 或 01006）的请求体", required = true)
            @RequestBody Map<String, String> body) {
        String uid = body.get("uid");
        String scanLocation = body.get("scanLocation");
        if (uid == null || uid.isEmpty()) {
            return ResponseResult.fail();
        }
        produceGoodsService.markScanned(uid, scanLocation);
        return ResponseResult.success();
    }
}
