package com.middle.wcs.user.service;

import com.middle.wcs.user.entity.UserInfo;
import java.util.List;

/**
 * @classDesc: 业务接口:(UserInfo)
 * @author: makejava
 * @date: 2023-06-27 20:58:12
 * @copyright 作者
 */
public interface UserInfoService {

    Integer save (UserInfo userInfo);

    Boolean verifyName(UserInfo userInfo);

    Integer updatePassword(UserInfo userInfo);

    Boolean verifyPassword(UserInfo userInfo);

    /**
     * 获取所有操作员列表
     * @return 操作员列表
     */
    List<UserInfo> getAllOperators();

    /**
     * 解锁用户
     * @param userId 用户ID
     * @return 操作结果
     */
    Integer unlockUser(Long userId);

    /**
     * 锁定用户
     * @param userId 用户ID
     * @return 操作结果
     */
    Integer lockUser(Long userId);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 操作结果
     */
    Integer deleteUser(Long userId);

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 操作结果
     */
    Integer resetPassword(Long userId, String newPassword);

    /**
     * 更新用户信息（姓名、角色）
     * @param userInfo 用户信息
     * @return 操作结果
     */
    Integer updateUserInfo(UserInfo userInfo);
}
