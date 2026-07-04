package com.middle.wcs.produce.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 发送目的地请求 DTO
 * 用于 sendDestination 和 resendDestination 接口
 */
@Data
public class SendDestinationDTO {

    /** 托盘ID */
    private Long palletId;

    /** 虚拟托盘ID */
    private String virtualId;

    /** 当前激活的目的地编码 */
    private String destinationCode;

    /** 01006扫码缓存的条码列表（可选，resendDestination不传） */
    private List<String> barcodes;

    /** 是否跳过扫码判断（可选，resendDestination不传） */
    private Boolean skipScanCheck;
}
