package com.middle.wcs.produce.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 生产批次实体
 */
@Data
@TableName("produce_batch")
public class ProduceBatch {

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** MSE灭菌单号 */
    private String sterilizationOrderNo;

    /** MSE托盘数量 */
    private Integer palletQuantity;

    /** MSE灭菌柜名称/编码 */
    private String sterilizerNameCode;

    /** MSE工艺方案名称/编码 */
    private String processPlanNameCode;

    /** 状态 0待确认 1已确认 2生产中 3完成 */
    private String status;

    /** 作废标识 0正常 1作废 */
    private String invalidFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date confirmTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
