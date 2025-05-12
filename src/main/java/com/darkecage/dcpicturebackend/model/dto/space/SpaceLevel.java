package com.darkecage.dcpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @title: 空间级别
 * @author: darkecage
 * @date: 2025/5/13 0:50
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 值
     */
    private int value;

    /**
     * 中文
     */
    private String text;

    /**
     * 最大数量
     */
    private long maxCount;
    /**
     * 最大容量
     */
    private long maxSize;
}
