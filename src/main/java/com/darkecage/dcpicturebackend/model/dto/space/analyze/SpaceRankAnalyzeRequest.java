package com.darkecage.dcpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @title: 空间使用排行分析（仅管理员）
 * @author: darkecage
 * @date: 2025/5/16 22:31
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
