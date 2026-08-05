package com.middle.wcs.user.service.impl;

import com.middle.wcs.hander.BusinessException;
import com.middle.wcs.hander.CommonErrorCode;
import com.middle.wcs.user.entity.UserInfo;
import com.middle.wcs.user.dao.UserInfoMapper;
import com.middle.wcs.user.service.UserInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @classDesc: 业务逻辑:(UserInfo)
 * @author: makejava
 * @date: 2023-06-27 20:58:08
 * @copyright 作者
 */
@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Resource
    private UserInfoMapper userInfoMapper;


    @Override
    public Integer save (UserInfo userInfo) {
        // 先判断userCode有没有被注册过
        List<UserInfo> entity =  userInfoMapper.selectUserList(userInfo);
        if(entity.size() > 0) {
            throw BusinessException.build(CommonErrorCode.DUPLICATE_USER_CODE);
        }
        // 设置默认值
        if (userInfo.getUserRole() == null || userInfo.getUserRole().isEmpty()) {
            userInfo.setUserRole("OPERATOR"); // 默认角色为操作员
        }
        userInfo.setLoginFailCount(0);
        userInfo.setIsLocked(0);
        userInfo.setCreateTime(new Date());
        userInfo.setUpdateTime(new Date());
        userInfo.setPasswordChangeTime(new Date());
        return userInfoMapper.insert(userInfo);
    }

    @Override
    public Boolean verifyName(UserInfo userInfo) {
        List<UserInfo> entity =  userInfoMapper.selectUserList(userInfo);
        if(entity.size() < 1) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        return userInfo.getUserName().equals(entity.get(0).getUserName());
    }

    @Override
    public Integer updatePassword(UserInfo userInfo) {
        return userInfoMapper.updatePassword(userInfo);
    }

    /**
     * verifyPassword
     */
    @Override
    public Boolean verifyPassword(UserInfo userInfo) {
        List<UserInfo> entity =  userInfoMapper.selectUserList(userInfo);
        if(entity.size() < 1) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        return userInfo.getUserPassword().equals(entity.get(0).getUserPassword());
    }

    @Override
    public List<UserInfo> getAllOperators() {
        return userInfoMapper.selectAllUsers();
    }

    @Override
    public Integer unlockUser(Long userId) {
        return userInfoMapper.unlockUser(userId);
    }

    @Override
    public Integer lockUser(Long userId) {
        return userInfoMapper.lockUser(userId);
    }

    @Override
    public Integer deleteUser(Long userId) {
        // 先查询用户信息
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        // 不允许删除 admin 账号
        if ("admin".equals(userInfo.getUserCode())) {
            throw BusinessException.build(CommonErrorCode.CANNOT_DELETE_ADMIN);
        }
        Integer result = userInfoMapper.deleteUser(userId);
        if (result == 0) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        return result;
    }

    @Override
    public Integer resetPassword(Long userId, String newPassword) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        userInfo.setUserPassword(newPassword);
        return userInfoMapper.updatePassword(userInfo);
    }

    @Override
    public Integer updateUserInfo(UserInfo userInfo) {
        UserInfo existingUser = userInfoMapper.selectById(userInfo.getUserId());
        if (existingUser == null) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        // 不允许修改 admin 账号
        if ("admin".equals(existingUser.getUserCode())) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }
        return userInfoMapper.updateUserInfo(userInfo);
    }
}
