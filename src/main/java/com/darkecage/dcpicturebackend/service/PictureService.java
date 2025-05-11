package com.darkecage.dcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.darkecage.dcpicturebackend.model.dto.picture.PictureQueryRequest;
import com.darkecage.dcpicturebackend.model.dto.picture.PictureReviewRequest;
import com.darkecage.dcpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.darkecage.dcpicturebackend.model.dto.picture.PictureUploadRequest;
import com.darkecage.dcpicturebackend.model.entity.Picture;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author Cage
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-05-03 20:27:19
*/
public interface PictureService extends IService<Picture> {

    /**
     * @title: 校验图片
     * @author: darkecage
     * @date: 2025/5/4 2:40
     * @param: picture
     */
    void validPicture(Picture picture);

    /**
     * @title: 上传图片
     * @author: darkecage
     * @date: 2025/5/6 2:12
     * @param: inputSource
     * @param: pictureUploadRequest
     * @param: loginUser
     * @return: com.darkecage.dcpicturebackend.model.vo.PictureVO
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/4 0:50
     * @param: pictureQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.Picture>
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * @title: 获取图片包装类（单条）
     * @author: darkecage
     * @date: 2025/5/4 1:39
     * @param: picture
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.vo.PictureVO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * @title: 获取图片包装类（分页）
     * @author: darkecage
     * @date: 2025/5/4 1:45
     * @param: picturePage
     * @param: request
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.darkecage.dcpicturebackend.model.vo.PictureVO>
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * @title: 图片审核
     * @author: darkecage
     * @date: 2025/5/4 19:16
     * @param: pictureReviewRequest
     * @param: loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * @title: 填充审核参数
     * @author: darkecage
     * @date: 2025/5/5 1:39
     * @param: picture
     * @param: loginUser
     */
    void fillReviewRarams(Picture picture, User loginUser);

    /**
     * @title: 批量抓取图片
     * @author: darkecage
     * @date: 2025/5/6 23:27
     * @param: pictureUploadByBatchRequest
     * @param: loginUser
     * @return: java.lang.Integer
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * @title: 清理图片
     * @author: darkecage
     * @date: 2025/5/10 22:26
     * @param: oldPicture
     */
    void clearPictureFile(Picture oldPicture);
}
