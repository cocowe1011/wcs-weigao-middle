package com.middle.wcs.produce.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 条码匹配分配虚拟ID请求 DTO
 */
@Data
public class MatchAndAssignDTO {

    /** 当前批次ID */
    private Long batchId;

    /** 扫描到的条码列表 */
    private List<String> barcodes;
}
