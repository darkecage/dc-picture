package com.darkecage.dcpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 编辑空间请求
 * @author: darkecage
 * @date: 2025/5/11 22:04
 * @param: null
 * @return: null
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
