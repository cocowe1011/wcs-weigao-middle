package com.middle.wcs.user.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.user.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @classDesc: 数据访问接口:(UserInfo)
 * @author: makejava
 * @date: 2023-06-27 20:58:15
 * @copyright 作者
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /**
     * 查询用户信息
     * @param userInfo 入参
     * @return 出参
     */
    List<UserInfo> selectUserList(UserInfo userInfo);

    Integer updatePassword(UserInfo userInfo);

    /**
     * 查询所有操作员用户
     * @return 操作员列表
     */
    List<UserInfo> selectAllUsers();

    /**
     * 更新登录失败次数
     * @param userInfo 用户信息
     * @return 更新结果
     */
    Integer updateLoginFailCount(UserInfo userInfo);

    /**
     * 解锁用户
     * @param userId 用户ID
     * @return 更新结果
     */
    Integer unlockUser(@Param("userId") Long userId);

    /**
     * 锁定用户
     * @param userId 用户ID
     * @return 更新结果
     */
    Integer lockUser(@Param("userId") Long userId);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    Integer deleteUser(@Param("userId") Long userId);

    /**
     * 插入默认管理员账号
     * @return 插入结果
     */
    Integer insertDefaultAdmin();

    /**
     * 更新用户信息（姓名、角色）
     * @param userInfo 用户信息
     * @return 更新结果
     */
    Integer updateUserInfo(UserInfo userInfo);
}
