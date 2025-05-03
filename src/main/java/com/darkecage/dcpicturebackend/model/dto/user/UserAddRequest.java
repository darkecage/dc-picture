package com.darkecage.dcpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 创建用户dto
 * @author: darkecage
 * @date: 2025/5/3 1:41
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 972247560577215343L;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

}
