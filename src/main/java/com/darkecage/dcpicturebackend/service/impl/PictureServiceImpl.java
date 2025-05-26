package com.darkecage.dcpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.darkecage.dcpicturebackend.api.aliyunai.AliYunApi;
import com.darkecage.dcpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.darkecage.dcpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.manager.CosManager;
import com.darkecage.dcpicturebackend.manager.FileManager;
import com.darkecage.dcpicturebackend.manager.upload.FilePictureUpload;
import com.darkecage.dcpicturebackend.manager.upload.PictureUploadTemplate;
import com.darkecage.dcpicturebackend.manager.upload.UrlPictureUpload;
import com.darkecage.dcpicturebackend.mapper.PictureMapper;
import com.darkecage.dcpicturebackend.model.dto.file.UploadPictureResult;
import com.darkecage.dcpicturebackend.model.dto.picture.*;
import com.darkecage.dcpicturebackend.model.entity.Picture;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.enums.PictureReviewStatusEnum;
import com.darkecage.dcpicturebackend.model.enums.SpaceLevelEnum;
import com.darkecage.dcpicturebackend.model.vo.PictureVO;
import com.darkecage.dcpicturebackend.model.vo.UserVO;
import com.darkecage.dcpicturebackend.service.PictureService;
import com.darkecage.dcpicturebackend.service.SpaceService;
import com.darkecage.dcpicturebackend.service.UserService;
import com.darkecage.dcpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
* @author Cage
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-05-03 20:27:19
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunApi aliYunApi;

    /**
     * @title: 校验图片
     * @author: darkecage
     * @date: 2025/5/4 2:40
     * @param: picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时，id不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 512, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 512, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * @title: 上传图片
     * @author: darkecage
     * @date: 2025/5/6 2:12
     * @param: inputSource 文件输入源
     * @param: pictureUploadRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.PictureVO
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            //校验是否有空间的权限, 仅空间管理员才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            //校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        //判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //仅管理员或本人可编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //校验空间是否一致
            //没传spaceId则复用原本图片的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                //传了spaceId，必须和原图片的spaceId一致
                ThrowUtils.throwIf(ObjUtil.notEqual(spaceId, oldPicture.getSpaceId()), ErrorCode.PARAMS_ERROR, "空间id不一致");
            }
        }
        //上传图片，得到图片信息
        //按照用户ID划分目录
        String uploadPathPrefix = null;
        if (spaceId == null) {
            //公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            //空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //根据inputSource的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        this.fillReviewRarams(picture, loginUser);
        //操作数据库
        //如果pictureId不为空，表示更新，否则是新增
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
//            this.clearPictureFile(picture);
        }
        Long finalSpaceId = spaceId;
        if (spaceId == null) {
            //插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作异常");
        } else {
            transactionTemplate.execute(status -> {
                //插入数据
                boolean result = this.saveOrUpdate(picture);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作异常");
                //更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败，数据库操作异常");
                return picture;
            });
        }
        //创建
        return PictureVO.objToVo(picture);
    }

    /**
     * @title: 删除图片
     * @author: darkecage
     * @date: 2025/5/12 22:08
     * @param: pictureId
     * @param: loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        if (pictureId <= 0 || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限（已经改为使用注解鉴权）
//        this.checkPictureAuth(loginUser, oldPicture);
        //开启事务
        Long spaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            //更新空间的使用额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, spaceId)
                    .setSql("totalSize = totalSize + " + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount + 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            return true;
        });
        //清理图片资源
        this.clearPictureFile(oldPicture);
    }

    /**
     * @title: 编辑图片
     * @author: darkecage
     * @date: 2025/5/12 22:14
     * @param: pictureEditRequest
     * @param: loginUser
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限（已经改为使用注解鉴权）
//        this.checkPictureAuth(loginUser, oldPicture);
        //补充审核参数
        this.fillReviewRarams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/4 0:50
     * @param: pictureQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.Picture>
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            //and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @title: 获取图片包装类（单条）
     * @author: darkecage
     * @date: 2025/5/4 1:39
     * @param: picture
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.vo.PictureVO
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * @title: 获取图片包装类（分页）
     * @author: darkecage
     * @date: 2025/5/4 1:45
     * @param: picturePage
     * @param: request
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.darkecage.dcpicturebackend.model.vo.PictureVO>
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //关联查询用户信息
        //userId去重
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //根据userId进行User对象分组
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            UserVO user = null;
            if (userIdListMap.containsKey(userId)){
                user = userService.getUserVO(userIdListMap.get(userId).get(0));
            }
            pictureVO.setUser(user);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * @title: 图片审核
     * @author: darkecage
     * @date: 2025/5/4 19:16
     * @param: pictureReviewRequest
     * @param: loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验审核状态是否已是旧图片状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //操作数据库
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * @title: 填充审核参数
     * @author: darkecage
     * @date: 2025/5/5 1:39
     * @param: picture
     * @param: loginUser
     */
    @Override
    public void fillReviewRarams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            //管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            //非管理员 无论是编辑还是创建都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * @title: 批量抓取图片
     * @author: darkecage
     * @date: 2025/5/6 23:27
     * @param: pictureUploadByBatchRequest
     * @param: loginUser
     * @return: java.lang.Integer
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        //名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        //抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        //解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        //遍历元素依次上传
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //处理图片的地址，防止转义或者和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try{
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * @title: 清理图片
     * @author: darkecage
     * @date: 2025/5/10 22:26
     * @param: oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //判断该图片是否呗多条记录使用（前提是开启了秒传）
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        //删除图片
        cosManager.deleteObject(pictureUrl);
        //删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * @title: 校验空间图片的权限
     * @author: darkecage
     * @date: 2025/5/12 21:59
     * @param: loginUser
     * @param: picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            //公共图库，仅本人和管理员可以操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
            }
        } else {
            //私有空间，只有空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
            }
        }
    }

    /**
     * @title: 根据颜色搜索图片
     * @author: darkecage
     * @date: 2025/5/14 1:19
     * @param: spaceId
     * @param: picColor
     * @param: loginUser
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.PictureVO>
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //2校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //3查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        //如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        //将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        //4计算相似度并排序
        List<PictureVO> sortedPictureVOList = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    //没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    //计算相似度
                    return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
                }))
                .limit(12) //取前12条
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //5返回结果
        return sortedPictureVOList;
    }

    /**
     * @title: 批量编辑图片
     * @author: darkecage
     * @date: 2025/5/14 19:50
     * @param: pictureEditByBatchRequest
     * @param: loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //空间权限校验
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return;
        }
        //更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        //操作数据库,进行批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");
    }

    /**
     * @title: 创建扩图任务
     * @author: darkecage
     * @date: 2025/5/15 19:09
     * @param: createPictureOutPaintingTaskRequest
     * @param: loginUser
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        //获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureId), ErrorCode.PARAMS_ERROR);
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        //校验权限（已经改为使用注解鉴权）
//        checkPictureAuth(loginUser, picture);
        //创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        String url = picture.getUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR);
        input.setImageUrl(url);
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        return aliYunApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    /**
     * @title: 格式：图片{序号}
     * @author: darkecage
     * @date: 2025/5/14 22:14
     * @param: pictureList
     * @param: nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析失败");
        }
    }

//    @Resource
//    private ThreadPoolExecutor customExecutor;
//
//    /**
//     * 批量编辑图片分类和标签
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public void batchEditPictureMetadata(PictureEditByBatchRequest request, Long spaceId, Long loginUserId) {
//        // 参数校验
//        List<Long> pictureIdList = request.getPictureIdList();
//        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
//        ThrowUtils.throwIf(spaceId <= 0 || spaceId == null, ErrorCode.PARAMS_ERROR);
//        ThrowUtils.throwIf(loginUserId <= 0 || loginUserId == null, ErrorCode.NOT_LOGIN_ERROR);
//        // 查询空间下的图片
//        List<Picture> pictureList = this.lambdaQuery()
//                .eq(Picture::getSpaceId, spaceId)
//                .in(Picture::getId, pictureIdList)
//                .list();
//
//        if (pictureList.isEmpty()) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "指定的图片不存在或不属于该空间");
//        }
//
//        // 分批处理避免长事务
//        int batchSize = 100;
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        for (int i = 0; i < pictureList.size(); i += batchSize) {
//            List<Picture> batch = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));
//
//            // 异步处理每批数据
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                batch.forEach(picture -> {
//                    // 编辑分类和标签
//                    if (request.getCategory() != null) {
//                        picture.setCategory(request.getCategory());
//                    }
//                    if (request.getTags() != null) {
//                        picture.setTags(String.join(",", request.getTags()));
//                    }
//                });
//                boolean result = this.updateBatchById(batch);
//                if (!result) {
//                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量更新图片失败");
//                }
//            }, customExecutor);
//
//            futures.add(future);
//        }
//
//        // 等待所有任务完成
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//    }

}




