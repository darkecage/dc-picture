package com.darkecage.dcpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.darkecage.dcpicturebackend.model.dto.space.analyze.*;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
* @author Cage
* @description 空间分析Service
* @createDate 2025-05-11 21:58:12
*/
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * @title: 获取空间使用情况分析
     * @author: darkecage
     * @date: 2025/5/16 17:12
     * @param: spaceUsageAnalyzeRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * @title: 获取空间图片分类分析
     * @author: darkecage
     * @date: 2025/5/16 18:50
     * @param: spaceCategoryAnalyzeRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * @title: 获取空间图片标签分析
     * @author: darkecage
     * @date: 2025/5/16 19:16
     * @param: spaceTagAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse>
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * @title: 获取空间图片大小分析
     * @author: darkecage
     * @date: 2025/5/16 19:52
     * @param: spaceSizeAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * @title: 获取空间用户行为分析
     * @author: darkecage
     * @date: 2025/5/16 21:17
     * @param: spaceUserAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * @title: 获取空间使用排行分析（仅管理员）
     * @author: darkecage
     * @date: 2025/5/16 22:33
     * @param: spaceRankAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.entity.Space>
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
