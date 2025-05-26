package com.darkecage.dcpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 空间成员权限
 * @author: darkecage
 * @date: 2025/5/23 18:47
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}
