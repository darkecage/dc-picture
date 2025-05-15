package com.darkecage.dcpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 以图搜图请求
 * @author: darkecage
 * @date: 2025/5/13 23:09
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
