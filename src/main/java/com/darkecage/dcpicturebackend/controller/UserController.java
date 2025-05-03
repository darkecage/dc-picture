package com.darkecage.dcpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkecage.dcpicturebackend.annotation.AuthCheck;
import com.darkecage.dcpicturebackend.common.BaseResponse;
import com.darkecage.dcpicturebackend.common.DeleteRequest;
import com.darkecage.dcpicturebackend.common.ResultUtils;
import com.darkecage.dcpicturebackend.constant.UserConstant;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.model.dto.user.*;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.LoginUserVO;
import com.darkecage.dcpicturebackend.model.vo.UserVO;
import com.darkecage.dcpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * @title: 用户注册
     * @author: darkecage
     * @date: 2025/5/1 2:11
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Long>
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegiesterRequest userRegiesterRequest){
        ThrowUtils.throwIf(userRegiesterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegiesterRequest.getUserAccount();
        String userPassword = userRegiesterRequest.getUserPassword();
        String checkPassword = userRegiesterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * @title: 用户登录
     * @author: darkecage
     * @date: 2025/5/2 2:31
     * @param: userLoginRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.model.vo.LoginUserVO>
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO result = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(result);
    }


    /**
     * @title: 用户登录
     * @author: darkecage
     * @date: 2025/5/3 0:17
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.model.vo.LoginUserVO>
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * @title: 用户注销
     * @author: darkecage
     * @date: 2025/5/3 0:20
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean logout = userService.userLogout(request);
        return ResultUtils.success(logout);
    }

    /**
     * @title: 创建用户
     * @author: darkecage
     * @date: 2025/5/3 2:55
     * @param: userAddRequest
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Long>
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        //默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        //插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * @title: 根据ID获取用户信息(仅管理员)
     * @author: darkecage
     * @date: 2025/5/3 3:03
     * @param: id
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.model.entity.User>
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * @title: 根据ID获取脱敏后的用户信息
     * @author: darkecage
     * @date: 2025/5/3 3:07
     * @param: id
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.model.vo.UserVO>
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * @title: 删除用户
     * @author: darkecage
     * @date: 2025/5/3 3:11
     * @param: deleteRequest
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * @title: 更新用户信息
     * @author: darkecage
     * @date: 2025/5/3 3:15
     * @param: userUpdateRequest
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * @title: 分页查询脱敏后的用户信息列表
     * @author: darkecage
     * @date: 2025/5/3 3:22
     * @param: userQueryRequest
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.darkecage.dcpicturebackend.model.vo.UserVO>>
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> newPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        newPage.setRecords(userVOList);
        return ResultUtils.success(newPage);
    }
}
