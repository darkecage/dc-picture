package com.darkecage.dcpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 查询空间成员请求（不分页）
 * @author: darkecage
 * @date: 2025/5/18 0:48
 */
@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
