package com.darkecage.dcpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.mapper.SpaceMapper;
import com.darkecage.dcpicturebackend.model.dto.space.analyze.*;
import com.darkecage.dcpicturebackend.model.entity.Picture;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.space.analyze.*;
import com.darkecage.dcpicturebackend.service.PictureService;
import com.darkecage.dcpicturebackend.service.SpaceAnalyzeService;
import com.darkecage.dcpicturebackend.service.SpaceService;
import com.darkecage.dcpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author Cage
* @description 空间分析Service实现
* @createDate 2025-05-11 21:58:12
*/
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService{

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * @title: 获取空间使用情况分析
     * @author: darkecage
     * @date: 2025/5/16 17:12
     * @param: spaceUsageAnalyzeRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //全空间或公共图库，需要从Picture表查询
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            //权限校验，仅管理员可以访问
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
            pictureQueryWrapper.select("picSize");
            //自动填充查询范围
            this.fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, pictureQueryWrapper);
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(obj -> (long) obj).sum();
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库（或者全部空间）无数量和容量限制，也没有比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            //特定空间可以直接从Space表查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null ||spaceId <= 0, ErrorCode.PARAMS_ERROR);
            //获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            //权限校验，特定空间仅本人和管理员可以访问
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * @title: 获取空间图片分类分析
     * @author: darkecage
     * @date: 2025/5/16 18:50
     * @param: spaceCategoryAnalyzeRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, pictureQueryWrapper);
        //使用Mybatis Plus分组查询
        pictureQueryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        //查询
        return pictureService.getBaseMapper().selectMaps(pictureQueryWrapper)
                .stream().map(result -> {
                    String category = (String) result.get("category");
                    Long count = Long.valueOf(result.get("count").toString());
                    Long totalSize = Long.valueOf(result.get("totalSize").toString());
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * @title: 获取空间图片标签分析
     * @author: darkecage
     * @date: 2025/5/16 19:16
     * @param: spaceTagAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse>
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, pictureQueryWrapper);
        // 查询所有符合条件的标签
        pictureQueryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        //解析标签并统计
        Map<String, Long> tagsCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //转换为响应对象，按照使用次数进行排序
        return tagsCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) //降序排序
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * @title: 获取空间图片大小分析
     * @author: darkecage
     * @date: 2025/5/16 19:52
     * @param: spaceSizeAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse>
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //检查权限
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, pictureQueryWrapper);
        // 查询所有符合条件的大小
        pictureQueryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(size -> (Long) size)
                .collect(Collectors.toList());
        //定义分段范围，注意使用有序Map
        LinkedHashMap<String, Long> sizeRangeMap = new LinkedHashMap<>();
        sizeRangeMap.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRangeMap.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRangeMap.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRangeMap.put(">1MB", picSizeList.stream().filter(size -> size >=1 * 1024 * 1024).count());
        //将Map结果转换为响应数组
        return sizeRangeMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * @title: 获取空间用户行为分析
     * @author: darkecage
     * @date: 2025/5/16 21:17
     * @param: spaceUserAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse>
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //检查权限
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, pictureQueryWrapper);
        //补充用户id查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        pictureQueryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        //分析维度：日，周，月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                pictureQueryWrapper.select("CAST(YEARWEEK(createTime) as char) as period", "count(*) as count");
                break;
            case "mouth":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }
        //分组排序
        pictureQueryWrapper.groupBy("period").orderByAsc("period");
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        //转换
        return queryResult.stream()
                .map(entry -> {
                    String period = (String) entry.get("period");
                    Long count = (Long) entry.get("count");
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * @title: 获取空间使用排行分析（仅管理员）
     * @author: darkecage
     * @date: 2025/5/16 22:33
     * @param: spaceRankAnalyzeRequest
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.entity.Space>
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //检查权限，仅管理员可以查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        //构造查询条件
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getTopN()); //取前N名
        return spaceService.list(spaceQueryWrapper);
    }

    /**
     * @title: 校验空间分析权限
     * @author: darkecage
     * @date: 2025/5/16 15:48
     * @param: spaceAnalyzeRequest
     * @param: loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser){
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //全空间分析或者公共图库权限校验，仅管理员可以访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            //分析特定空间，仅本人和管理员可以访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * @title: 根据请求对象封装查询条件
     * @author: darkecage
     * @date: 2025/5/16 15:59
     * @param: spaceAnalyzeRequest
     * @param: queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper){
        //全空间分析
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        //公共图库
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
        }
        //指定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null){
            queryWrapper.eq("spaceId", spaceId);
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}




