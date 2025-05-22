package com.darkecage.dcpicturebackend.controller;

import com.darkecage.dcpicturebackend.annotation.AuthCheck;
import com.darkecage.dcpicturebackend.common.BaseResponse;
import com.darkecage.dcpicturebackend.common.ResultUtils;
import com.darkecage.dcpicturebackend.constant.UserConstant;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.model.dto.space.analyze.*;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.enums.UserRoleEnum;
import com.darkecage.dcpicturebackend.model.vo.space.analyze.*;
import com.darkecage.dcpicturebackend.service.SpaceAnalyzeService;
import com.darkecage.dcpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.RIPEMD128;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
@Slf4j
public class SpaceAnalyzeController {
    @Resource
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * @title: 获取空间使用状态
     * @author: darkecage
     * @date: 2025/5/16 18:43
     * @param: spaceUsageAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse>
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse analyzeResponse = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(analyzeResponse);
    }


    /**
     * @title: 获取空间图片分类分析
     * @author: darkecage
     * @date: 2025/5/16 19:47
     * @param: spaceCategoryAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse>>
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeList = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeList);
    }

    /**
     * @title: 获取空间图片标签分析
     * @author: darkecage
     * @date: 2025/5/16 19:49
     * @param: spaceTagAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse>>
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeList = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyzeList);
    }

    /**
     * @title: 获取空间图片大小分析
     * @author: darkecage
     * @date: 2025/5/16 21:12
     * @param: spaceSizeAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse>>
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeList);
    }

    /**
     * @title: 获取空间用户上传行为分析
     * @author: darkecage
     * @date: 2025/5/16 22:10
     * @param: spaceUserAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse>>
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> responseList = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(responseList);
    }

    /**
     * @title: 获取空间使用排行分析
     * @author: darkecage
     * @date: 2025/5/16 22:40
     * @param: spaceRankAnalyzeRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.entity.Space>>
     */
    @PostMapping("/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<Space> rankAnalyzeList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(rankAnalyzeList);
    }
}
