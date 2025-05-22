package com.darkecage.dcpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 通用空间分析请求
 * @author: darkecage
 * @date: 2025/5/16 15:44
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
