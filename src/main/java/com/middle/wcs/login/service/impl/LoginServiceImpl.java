package com.middle.wcs.login.service.impl;

import com.middle.wcs.hander.BusinessException;
import com.middle.wcs.hander.CommonErrorCode;
import com.middle.wcs.login.dao.LoginServiceMapper;
import com.middle.wcs.login.entity.LoginDTO;
import com.middle.wcs.login.service.LoginService;
import com.middle.wcs.user.dao.UserInfoMapper;
import com.middle.wcs.user.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 功能描述: (对外提供预约信息的中台接口)
 * @author 文亮
 * @since 2022年5月13日14:07:01
 */
@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Resource
    private LoginServiceMapper loginServiceMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Autowired
    private LoginFailCountService loginFailCountService;

    /**
     * 登录
     * @param loginDTO 登录入参
     * @return 登录出参
     */
    @Override
    @Transactional
    public UserInfo login(LoginDTO loginDTO) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserCode(loginDTO.getUserCode());
        // 通过userCode去查询密码
        List<UserInfo> entity =  userInfoMapper.selectUserList(userInfo);
        if(entity.size() != 1) {
            throw BusinessException.build(CommonErrorCode.NOT_EXITS_USER_CODE);
        }

        UserInfo user = entity.get(0);

        // 检查账号是否被锁定
        if (user.getIsLocked() != null && user.getIsLocked() == 1) {
            throw BusinessException.build(CommonErrorCode.ACCOUNT_LOCKED);
        }

        // 验证密码是否正确
        if(!(null != loginDTO.getUserPassword() && loginDTO.getUserPassword().equals(user.getUserPassword()))) {
            // 如果是操作员，增加登录失败次数
            if ("OPERATOR".equals(user.getUserRole())) {
                // 使用独立的Service更新失败次数，避免回滚
                int remainingChances = loginFailCountService.updateLoginFailCount(user.getUserCode());
                if (remainingChances <= 0) {
                    throw BusinessException.build(CommonErrorCode.PASSWORD_ERROR_TOO_MANY);
                } else {
                    throw BusinessException.build(CommonErrorCode.PASSWORD_ERROR_LIMIT, String.valueOf(remainingChances));
                }
            } else {
                // 管理员/工艺员不限制登录次数
                throw BusinessException.build(CommonErrorCode.PASSWORD_ERROR);
            }
        }

        // 登录成功，重置失败次数
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            loginFailCountService.resetLoginFailCount(user.getUserCode());
        }

        // 判断密码是否超过3个月未修改
        user.setPasswordExpired(isPasswordExpired(user));

        return user;
    }

    /**
     * 判断密码是否过期（超过3个月未修改）
     * @param user 用户信息
     * @return true-已过期，false-未过期
     */
    private boolean isPasswordExpired(UserInfo user) {
        // admin账号不强制改密
        if ("admin".equalsIgnoreCase(user.getUserCode())) {
            return false;
        }
        Date changeTime = user.getPasswordChangeTime();
        // 如果从未修改过密码（旧数据），视为已过期
        if (changeTime == null) {
            return true;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(changeTime);
        cal.add(Calendar.MONTH, 3);
        Date expireDate = cal.getTime();
        return new Date().after(expireDate);
    }
}
