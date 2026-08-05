package com.middle.wcs.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.Date;
/**
 * @classDesc: 实体类:(UserInfo)
 * @author: makejava
 * @date: 2023-06-27 20:58:16
 * @copyright 作者
 */
@Data
@TableName("user_info")
public class UserInfo {
    /**
     * 用户主键id
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 用户登录code
     */
    private String userCode;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户角色（ADMIN-管理员，OPERATOR-操作员，TECHNICIAN-工艺员）
     */
    private String userRole;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 是否锁定（0-否，1-是）
     */
    private Integer isLocked;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 密码最后修改时间
     */
    private Date passwordChangeTime;

    /**
     * 密码是否过期（非数据库字段，登录时计算）
     */
    @TableField(exist = false)
    private Boolean passwordExpired;

}
