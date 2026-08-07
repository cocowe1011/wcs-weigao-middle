package com.middle.wcs.produce.entity.dto;

import lombok.Data;

/**
 * 历史批次分页查询入参
 */
@Data
public class BatchHistoryQueryDTO {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** MSE灭菌单号 */
    private String sterilizationOrderNo;

    /** 创建时间起（对应 created_at，格式 yyyy-MM-dd） */
    private String createdAtStart;

    /** 创建时间止（对应 created_at，格式 yyyy-MM-dd，含当天） */
    private String createdAtEnd;
}
