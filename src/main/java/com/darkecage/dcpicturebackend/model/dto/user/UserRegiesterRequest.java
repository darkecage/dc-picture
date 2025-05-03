package com.darkecage.dcpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 用户注册请求
 * @author: darkecage
 * @date: 2025/4/30 22:38
 */
@Data
public class UserRegiesterRequest implements Serializable {
    private static final long serialVersionUID = 6131172326816621417L;

    //账号
    private String userAccount;

    //登录密码
    private String userPassword;

    //确认密码
    private String checkPassword;
}
