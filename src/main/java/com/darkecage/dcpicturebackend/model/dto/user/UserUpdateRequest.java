package com.darkecage.dcpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 更新用户请求
 * @author: darkecage
 * @date: 2025/5/3 1:41
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 5165660185327304110L;
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

}
