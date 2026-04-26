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
 * 批次目的地设置流水实体
 * 一个批次可多次设置目的地，每条记录代表一次设置操作
 * status: 0-激活(当前有效) 1-已取消
 */
@Data
@TableName("produce_batch_destination")
public class ProduceBatchDestination {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchId;

    /** 目的地编码，取值范围 3201~3215 */
    private String destinationCode;

    /** 状态 0-激活 1-已取消 */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date setTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date cancelTime;

    /** 作废标识 0正常 1作废 */
    private String invalidFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
