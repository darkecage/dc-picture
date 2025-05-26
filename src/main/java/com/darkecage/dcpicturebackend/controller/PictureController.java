package com.darkecage.dcpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkecage.dcpicturebackend.annotation.AuthCheck;
import com.darkecage.dcpicturebackend.api.aliyunai.AliYunApi;
import com.darkecage.dcpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.darkecage.dcpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.darkecage.dcpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.darkecage.dcpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.darkecage.dcpicturebackend.common.BaseResponse;
import com.darkecage.dcpicturebackend.common.DeleteRequest;
import com.darkecage.dcpicturebackend.common.ResultUtils;
import com.darkecage.dcpicturebackend.constant.UserConstant;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.manager.auth.SpaceUserAuthManager;
import com.darkecage.dcpicturebackend.manager.auth.StpKit;
import com.darkecage.dcpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.darkecage.dcpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.darkecage.dcpicturebackend.model.dto.picture.*;
import com.darkecage.dcpicturebackend.model.entity.Picture;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.enums.PictureReviewStatusEnum;
import com.darkecage.dcpicturebackend.model.vo.PictureTagCategory;
import com.darkecage.dcpicturebackend.model.vo.PictureVO;
import com.darkecage.dcpicturebackend.service.PictureService;
import com.darkecage.dcpicturebackend.service.SpaceService;
import com.darkecage.dcpicturebackend.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunApi aliYunApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    //本地缓存
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过url上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * @title: 删除图片
     * @author: darkecage
     * @date: 2025/5/4 2:59
     * @param: deleteRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * @title: 更新图片（仅管理员可用）
     * @author: dark ecage
     * @date: 2025/5/5 1:55
     * @param: pictureUpdateRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewRarams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            //已经改为使用saToken编程式鉴权
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(loginUser, picture);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, userService.getLoginUser(request));
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //查询公开图库
            //普通用户默认只能看到审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            //已经改为使用saToken编程式鉴权
            //私有空间
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
//            User loginUser = userService.getLoginUser(request);
//            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类,有缓存）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //普通用户默认只能看到审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //先查询缓存，若没有再查询数据库
        //构建缓存的key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("yupicture:listPictureVOByPage:%s", hashKey);
        //先查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //如果本地缓存命中，直接返回结果
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        //本地缓存未命中，查询redis分布式缓存
        cachedValue = opsForValue.get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //缓存命中，更新本地缓存，返回结果
            LOCAL_CACHE.put(cacheKey ,cachedValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //存入redis缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //设置缓存的过期时间， 5-10分钟过期, 防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        //redis 写缓存
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        //写本地缓存
        LOCAL_CACHE.put(cacheKey ,cacheValue);
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * @title: 审核图片
     * @author: darkecage
     * @date: 2025/5/5 0:51
     * @param: pictureReviewRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * @title: 批量抓取并创建图片
     * @author: darkecage
     * @date: 2025/5/7 0:41
     * @param: pictureUploadByBatchRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * @title: 以图搜图
     * @author: darkecage
     * @date: 2025/5/13 23:10
     * @param: searchPictureByPictureRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.api.imagesearch.model.ImageSearchResult>
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        String imageUrl = picture.getThumbnailUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(imageUrl), ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(imageUrl);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * @title: 按照颜色搜索图片
     * @author: darkecage
     * @date: 2025/5/14 18:15
     * @param: searchPictureByColorRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.util.List<com.darkecage.dcpicturebackend.model.vo.PictureVO>>
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(searchPictureByColorRequest.getSpaceId(), searchPictureByColorRequest.getPicColor(), loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * @title: 图片批量编辑
     * @author: darkecage
     * @date: 2025/5/14 20:09
     * @param: pictureEditByBatchRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<java.lang.Boolean>
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * @title: 创建AI扩图任务
     * @author: darkecage
     * @date: 2025/5/15 19:37
     * @param: createPictureOutPaintingTaskRequest
     * @param: request
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse>
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask (@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * @title: 查询 AI 扩图任务
     * @author: darkecage
     * @date: 2025/5/15 19:45
     * @param: taskId
     * @return: com.darkecage.dcpicturebackend.common.BaseResponse<com.darkecage.dcpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse>
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse response = aliYunApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }
}
