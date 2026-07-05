package com.middle.wcs.produce.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 托盘详情 DTO（含货物列表）
 */
@Data
public class PalletDetailDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchId;

    private String palletNo;

    /** MSE是否入库 0否 1是 */
    private String toWarehouse;

    private String trayStatus;

    /** 虚拟托盘ID */
    private String virtualId;

    /** 上货状态 0-未上货 1-已上货 */
    private String loadStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date loadTime;

    /** 发送的目的地编码 */
    private String sendDestinationCode;

    /** 是否已发送目的地 0-未发送 1-已发送 */
    private String sendStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    private String invalidFlag;

    /** 托盘下的货物列表 */
    private List<ProduceGoods> goods;

    public static PalletDetailDTO from(ProducePallet pallet, List<ProduceGoods> goods) {
        PalletDetailDTO dto = new PalletDetailDTO();
        dto.setId(pallet.getId());
        dto.setBatchId(pallet.getBatchId());
        dto.setPalletNo(pallet.getPalletNo());
        dto.setToWarehouse(pallet.getToWarehouse());
        dto.setTrayStatus(pallet.getTrayStatus());
        dto.setVirtualId(pallet.getVirtualId());
        dto.setLoadStatus(pallet.getLoadStatus());
        dto.setLoadTime(pallet.getLoadTime());
        dto.setSendDestinationCode(pallet.getSendDestinationCode());
        dto.setSendStatus(pallet.getSendStatus());
        dto.setSendTime(pallet.getSendTime());
        dto.setInvalidFlag(pallet.getInvalidFlag());
        dto.setGoods(goods);
        return dto;
    }
}
