package com.middle.wcs.user.controller;

import com.middle.wcs.hander.ResponseResult;
import com.middle.wcs.user.entity.UserInfo;
import com.middle.wcs.user.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


/**
 * @classDesc: 控制器:(UserInfo)
 * @author: makejava
 * @date: 2023-06-27 20:58:06
 * @copyright 作者
 */
@RestController
@RequestMapping("/userInfo")
public class UserInfoController {

    @Resource
    private UserInfoService userInfoService;

    /**
     * 注册操作员账号（仅管理员可用）
     * @param userInfo 用户信息
     * @return 注册结果
     */
    @PostMapping("/registerOperator")
    public ResponseResult<Integer> registerOperator (@RequestBody UserInfo userInfo) {
        return  ResponseResult.success(userInfoService.save(userInfo));
    }

    /**
     * 获取所有操作员列表（仅管理员可用）
     * @return 操作员列表
     */
    @GetMapping("/getOperatorList")
    public ResponseResult<List<UserInfo>> getOperatorList() {
        return ResponseResult.success(userInfoService.getAllOperators());
    }

    /**
     * 解锁用户（仅管理员可用）
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/unlockUser/{userId}")
    public ResponseResult<Integer> unlockUser(@PathVariable Long userId) {
        return ResponseResult.success(userInfoService.unlockUser(userId));
    }

    /**
     * 锁定用户（仅管理员可用）
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/lockUser/{userId}")
    public ResponseResult<Integer> lockUser(@PathVariable Long userId) {
        return ResponseResult.success(userInfoService.lockUser(userId));
    }

    /**
     * 删除用户（仅管理员可用）
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/deleteUser/{userId}")
    public ResponseResult<Integer> deleteUser(@PathVariable Long userId) {
        return ResponseResult.success(userInfoService.deleteUser(userId));
    }

    /**
     * 重置用户密码（仅管理员可用）
     * @param params 包含userId和newPassword
     * @return 操作结果
     */
    @PostMapping("/resetPassword")
    public ResponseResult<Integer> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        String newPassword = params.get("newPassword").toString();
        return ResponseResult.success(userInfoService.resetPassword(userId, newPassword));
    }

    @PostMapping("/verifyName")
    public ResponseResult<Boolean> verifyName (@RequestBody UserInfo userInfo) {
        return  ResponseResult.success(userInfoService.verifyName(userInfo));
    }

    @PostMapping("/updatePassword")
    public ResponseResult<Integer> updatePassword (@RequestBody UserInfo userInfo) {
        return  ResponseResult.success(userInfoService.updatePassword(userInfo));
    }

    @PostMapping("/verifyPassword")
    public ResponseResult<Boolean> verifyPassword (@RequestBody UserInfo userInfo) {
        return  ResponseResult.success(userInfoService.verifyPassword(userInfo));
    }

    /**
     * 更新用户信息（姓名、角色）（仅管理员可用）
     * @param userInfo 用户信息
     * @return 操作结果
     */
    @PostMapping("/updateUserInfo")
    public ResponseResult<Integer> updateUserInfo(@RequestBody UserInfo userInfo) {
        return ResponseResult.success(userInfoService.updateUserInfo(userInfo));
    }
}
