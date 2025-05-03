package com.darkecage.dcpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @title: 图片标签分类列表视图
 * @author: darkecage
 * @date: 2025/5/4 3:13
 */
@Data
public class PictureTagCategory {
    /**
     * @title: 标签列表
     * @author: darkecage
     * @date: 2025/5/4 3:13
     */
    private List<String> tagList;
    /**
     * @title: 分类列表
     * @author: darkecage
     * @date: 2025/5/4 3:12
     */
    private List<String> categoryList;
}
