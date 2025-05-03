package com.darkecage.dcpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 用户注册请求
 * @author: darkecage
 * @date: 2025/4/30 22:38
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -2260949533138405815L;
    //账号
    private String userAccount;

    //登录密码
    private String userPassword;

}
