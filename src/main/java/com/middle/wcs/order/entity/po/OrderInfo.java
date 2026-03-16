package com.middle.wcs.order.entity.po;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * (OrderInfo)实体类
 *
 * @author makejava
 * @since 2024-12-28 23:59:48
 */
@Data
@TableName("order_info")
public class OrderInfo {
    /**
    * 主键
    */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 批次号
     */
    private String batchId;

    /**
     * 虚拟id（托盘号）
     */
    private String trayCode;

    /**
     * 货物明细(JSON)
     */
    private String trayDetail;

    /**
     * 扫码明细(JSON)
     */
    private String scanList;

    /**
     * 作废标识
     */
    private String invalidFlag;

    /**
     * 0待执行1执行中2已完成
     */
    private String trayStatus;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishTime;
}