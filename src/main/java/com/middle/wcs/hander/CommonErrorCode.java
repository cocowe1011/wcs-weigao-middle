package com.middle.wcs.hander;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:
 * @fileName: CommonErrorCode.java
 * @author: jason
 * @createAt: 2020/8/20 10:02 上午
 * @updateBy: jason
 * @remark: Copyright
 */
@Getter
@AllArgsConstructor
public enum CommonErrorCode implements IErrorCode {

    /**
     * 已有重复账号
     */
    DUPLICATE_USER_CODE("0001", "已有重复账号！"),
    /**
     * 未查询到账户信息
     */
    NOT_EXITS_USER_CODE("0002", "未查询到账户信息！"),
    /**
     * 密码错误
     */
    PASSWORD_ERROR("0003", "密码错误！"),
    /**
     * 账号已被锁定
     */
    ACCOUNT_LOCKED("0004", "账号已被锁定，请联系管理员解锁"),
    /**
     * 密码错误次数限制
     */
    PASSWORD_ERROR_LIMIT("0005", "用户名或密码错误，您还有$1次尝试机会，3次错误后账号将被锁定"),
    /**
     * 密码错误次数过多
     */
    PASSWORD_ERROR_TOO_MANY("0006", "密码错误次数过多，您的账号已被锁定，请联系管理员解锁"),
    /**
     * 已有批次正在执行
     */
    BATCH_ALREADY_EXECUTING("0007", "已有批次正在执行，请等待完成后再确认新批次"),
    /**
     * 不允许删除管理员账号
     */
    CANNOT_DELETE_ADMIN("0008", "不允许删除管理员账号！"),
    ;
    private String code;
    private String msg;

    @Override
    public String prefix() {
        return "common";
    }
}
