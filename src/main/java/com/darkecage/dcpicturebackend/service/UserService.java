package com.darkecage.dcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkecage.dcpicturebackend.model.dto.user.UserQueryRequest;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.darkecage.dcpicturebackend.model.vo.LoginUserVO;
import com.darkecage.dcpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Cage
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-04-30 21:58:53
*/
public interface UserService extends IService<User> {
    /**
     * @title: 用户注册
     * @author: darkecage
     * @date: 2025/4/30 23:40
     * @param: userAccount
     * @param: userPassword
     * @param: checkPassword
     * @return: long
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * @title: 用户登录
     * @author: darkecage
     * @date: 2025/5/1 3:08
     * @param: userAccount
     * @param: userPassword
     * @param: request
     * @return: LoginUserVO 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * @title: 获取脱媒类的用户信息
     * @author: darkecage
     * @date: 2025/5/1 3:40
     * @param: user
     * @return: com.darkecage.dcpicturebackend.model.vo.LoginUserVO
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * @title: 获取脱媒后的用户信息
     * @author: darkecage
     * @date: 2025/5/1 3:40
     * @param: user
     * @return: com.darkecage.dcpicturebackend.model.vo.LoginUserVO
     */
    UserVO getUserVO(User user);

    /**
     * @title: 获取脱媒后的用户信息列表
     * @author: darkecage
     * @date: 2025/5/3 2:09
     * @param: userList
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.UserVO>
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * @title: 获取当前登录用户的方法
     * @author: darkecage
     * @date: 2025/5/1 18:46
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.entity.User
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * @title: 获取加密后的密码
     * @author: darkecage
     * @date: 2025/5/1 1:51
     * @param: userPassword
     * @return: java.lang.String
     */
    String getEncryptPassword(String userPassword);

    /**
     * @title: 用户注销
     * @author: darkecage
     * @date: 2025/5/3 0:14
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.entity.User
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/3 2:37
     * @param: userQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.User>
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
