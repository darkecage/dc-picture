package com.darkecage.dcpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 编辑空间成员请求
 * @author: darkecage
 * @date: 2025/5/18 0:29
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
