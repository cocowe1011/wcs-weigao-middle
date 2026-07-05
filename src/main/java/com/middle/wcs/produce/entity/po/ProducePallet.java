package com.middle.wcs.produce.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 生产托盘实体
 */
@Data
@TableName("produce_pallet")
public class ProducePallet {

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchId;

    /** 托盘号（MSE托盘编码直接存此字段） */
    private String palletNo;

    /** MSE是否入库 0否 1是 */
    private String toWarehouse;

    /** 托盘状态 0待扫 1部分已扫 2全部已扫 */
    private String trayStatus;

    /** 虚拟托盘ID（PC端触发写虚拟ID请求时自动生成） */
    private String virtualId;

    /** 上货状态 0-未上货 1-已上货 */
    private String loadStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date loadTime;

    /** 发送的目的地编码（全扫=原始编码，未全扫=编码拼3） */
    private String sendDestinationCode;

    /** 是否已发送目的地 0-未发送 1-已发送 */
    private String sendStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    /** 作废标识 0正常 1作废 */
    private String invalidFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
