package com.middle.wcs.udi.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MSE 查询订单信息 模拟接口（开发测试用）
 *
 * <p>真实 MSE 地址：https://mes-prod.weigaogroup.com/captcha/MJsendUDI.ashx?method=getProductInfo&FBARCODE=(01)...
 * 开发环境下 PC 端 WCS 调用本 mock（localhost:7005），返回与真实 MSE 一致的完整结构。
 * 出入参完整示例参考 wcs-weigao-front/接口地址及出入参示例.txt（7 个托盘）。
 * 说明：直接用代码构建返回结构，不依赖资源文件，部署稳定。</p>
 *
 * @author mock
 */
@Api(tags = "MSE查询订单信息模拟接口")
@RestController
public class MseMockController {

    /** 注射器20ml 相关固定值 */
    private static final String SYRINGE_CODE = "01.02.01.0615";
    private static final String SYRINGE_NAME = "注射器20ml";
    private static final String SYRINGE_SPEC = "20ml1.2*38X筒外针";
    private static final String SYRINGE_GTIN = "26932992101039";

    /** 溶药注射器20ml 相关固定值 */
    private static final String SOLVENT_CODE = "01.02.03.0617";
    private static final String SOLVENT_NAME = "溶药注射器20ml";
    private static final String SOLVENT_SPEC = "20ml1.6*33X筒外侧孔针";
    private static final String SOLVENT_GTIN = "26932992101558";

    private static final String BATCH_NO = "20260621";
    private static final String PROD_DATE = "2026-06-24";

    @ApiOperation("模拟 MSE 通过 udi 查询本次生产灭菌计划所有码垛信息（完整7托盘）")
    @GetMapping("/mse/getProductInfo")
    public Map<String, Object> getProductInfo(
            @RequestParam(value = "method", required = false) String method,
            @RequestParam(value = "FBARCODE", required = false) String fbarcode) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sterilization_order_no", "202606220503022-MJ");
        data.put("pallet_quantity", 7);
        data.put("sterilizer_name_code", "3202");
        data.put("process_plan_name_code", "EO");

        List<Map<String, Object>> palletList = new ArrayList<>();

        // 托盘1：溶药注射器20ml。把扫码传入的 FBARCODE 作为首条 udi，保证 PDA 回查命中
        List<Map<String, Object>> d1 = new ArrayList<>();
        if (fbarcode != null && !fbarcode.isEmpty()) {
            d1.add(material(SOLVENT_CODE, SOLVENT_NAME, SOLVENT_SPEC, fbarcode));
        }
        addUdis(d1, SOLVENT_CODE, SOLVENT_NAME, SOLVENT_SPEC, SOLVENT_GTIN, "01B", "0146", "0147", "0148");
        addUdis(d1, SOLVENT_CODE, SOLVENT_NAME, SOLVENT_SPEC, SOLVENT_GTIN, "02B", "0148", "0149", "0150", "0153");
        addUdis(d1, SOLVENT_CODE, SOLVENT_NAME, SOLVENT_SPEC, SOLVENT_GTIN, "03B", "0143", "0144");
        palletList.add(pallet("MJF260624052718", true, d1));

        // 托盘2：注射器20ml
        List<Map<String, Object>> d2 = new ArrayList<>();
        addUdis(d2, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0057", "0058", "0059", "0060", "0061");
        addUdis(d2, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0055", "0056", "0057", "0058", "0059");
        addUdis(d2, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0058", "0059", "0060", "0061", "0062", "0063");
        palletList.add(pallet("MJF260625052925", true, d2));

        // 托盘3：注射器20ml
        List<Map<String, Object>> d3 = new ArrayList<>();
        addUdis(d3, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0062", "0063", "0064", "0065", "0066");
        addUdis(d3, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0060", "0061", "0062", "0063", "0064");
        addUdis(d3, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0064", "0065", "0066", "0067", "0068", "0069");
        palletList.add(pallet("MJF260625052933", true, d3));

        // 托盘4：注射器20ml
        List<Map<String, Object>> d4 = new ArrayList<>();
        addUdis(d4, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0072", "0073", "0074", "0075", "0076", "0077");
        addUdis(d4, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0072", "0073", "0074", "0075");
        addUdis(d4, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0076", "0077", "0078", "0079", "0080", "0081");
        palletList.add(pallet("MJF260625052953", true, d4));

        // 托盘5：注射器20ml
        List<Map<String, Object>> d5 = new ArrayList<>();
        addUdis(d5, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0078", "0079", "0080", "0081");
        addUdis(d5, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0076", "0077", "0078", "0079", "0080", "0081", "0082");
        addUdis(d5, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0082", "0083", "0084", "0085", "0086");
        palletList.add(pallet("MJF260625052970", true, d5));

        // 托盘6：注射器20ml
        List<Map<String, Object>> d6 = new ArrayList<>();
        addUdis(d6, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0082", "0083", "0084", "0085", "0086", "0087");
        addUdis(d6, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0083", "0084", "0085", "0086", "0087");
        addUdis(d6, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0087", "0088", "0089", "0090", "0091");
        palletList.add(pallet("MJF260625052982", true, d6));

        // 托盘7：注射器20ml
        List<Map<String, Object>> d7 = new ArrayList<>();
        addUdis(d7, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "04C", "0094", "0095", "0096", "0097");
        addUdis(d7, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "05C", "0093", "0094", "0095", "0096", "0097", "0098");
        addUdis(d7, SYRINGE_CODE, SYRINGE_NAME, SYRINGE_SPEC, SYRINGE_GTIN, "06C", "0097", "0098", "0099", "0100", "0101", "0102");
        palletList.add(pallet("MJF260625052998", true, d7));

        data.put("pallet_list", palletList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "OK");
        result.put("data", data);
        return result;
    }

    /** 按 (91)code(21)seq 批量追加货物 */
    private void addUdis(List<Map<String, Object>> details, String productCode, String productName,
                         String spec, String gtin, String code, String... seqs) {
        for (String seq : seqs) {
            details.add(material(productCode, productName, spec, buildUdi(gtin, code, seq)));
        }
    }

    /** 拼装 GS1 带括号 udi */
    private String buildUdi(String gtin, String code, String seq) {
        return "(01)" + gtin + "(10)" + BATCH_NO + "(11)260624(17)290620(91)" + code + "(21)" + seq;
    }

    private Map<String, Object> material(String productCode, String productName, String specification, String udi) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product_code", productCode);
        m.put("product_name", productName);
        m.put("product_specification", specification);
        m.put("production_batch_number", BATCH_NO);
        m.put("production_date", PROD_DATE);
        m.put("udi", udi);
        return m;
    }

    private Map<String, Object> pallet(String palletCode, boolean toWarehouse, List<Map<String, Object>> materialDetails) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("pallet_code", palletCode);
        p.put("to_warehouse", toWarehouse);
        p.put("material_details", materialDetails);
        return p;
    }
}
