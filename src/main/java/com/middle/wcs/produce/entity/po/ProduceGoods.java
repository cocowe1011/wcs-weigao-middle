package com.middle.wcs.produce.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 生产货物实体
 */
@Data
@TableName("produce_goods")
public class ProduceGoods {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long palletId;

    /** 货物唯一码 */
    private String uid;

    /** 品名 */
    private String productName;

    /** 规格 */
    private String spec;

    /** 备注 */
    private String remark;

    /** 扫码状态 0未扫 1已扫 */
    private String scanStatus;

    /** 扫码位置 */
    private String scanLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scanTime;

    /** 作废标识 0正常 1作废 */
    private String invalidFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
