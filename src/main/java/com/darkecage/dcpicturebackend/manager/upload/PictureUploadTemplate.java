package com.darkecage.dcpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.darkecage.dcpicturebackend.config.CosClientConfig;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.manager.CosManager;
import com.darkecage.dcpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @title: 图片上传模板
 * @author: darkecage
 * @date: 2025/5/6 0:59
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * @title: 上传图片
     * @author: darkecage
     * @date: 2025/5/3 21:26
     * @param: multipartFile
     * @param: UploadPrefix
     * @return: com.darkecage.dcpicturebackend.model.dto.file.UploadPictureResult
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPrefix) {
        //1.校验图片
        validPicture(inputSource);
        //2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        //自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPrefix, uploadFileName);
        File file = null;
        try {
            //3.创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, file);
            //4.上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //5.获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                //获取压缩之后得到的文件信息
                CIObject compressCiObject = objectList.get(0);
                //缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图的返回结果
                return buildResult(originalFilename, compressCiObject, thumbnailCiObject);
            }
            return buildResult(originalFilename, uploadPath, file, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败" + e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //6.清理临时文件
            deleteTempFile(file);
        }

    }

    /**
     * @title: 处理输入源并生成本地临时文件
     * @author: darkecage
     * @date: 2025/5/6 1:20
     * @param: inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * @title: 获取输入源的原始文件名
     * @author: darkecage
     * @date: 2025/5/6 1:20
     * @param: inputSource
     * @return: java.lang.String
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * @title: 校验输入源（本地文件或者URL）
     * @author: darkecage
     * @date: 2025/5/6 1:20
     * @param: inputSource
     */
    protected abstract void validPicture(Object inputSource);


    /**
     * @title: 封装返回结果
     * @author: darkecage
     * @date: 2025/5/10 18:02
     * @param: originalFilename
     * @param: compressCiObject
     * @param: thumbnailCiObject
     * @return: com.darkecage.dcpicturebackend.model.dto.file.UploadPictureResult
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressCiObject, CIObject thumbnailCiObject) {
        //计算宽高
        String format = compressCiObject.getFormat();
        int picWidth = compressCiObject.getWidth();
        int picHeight = compressCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0/picHeight, 2).doubleValue();
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        //缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        //返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * @title: 封装返回结果
     * @author: darkecage
     * @date: 2025/5/6 1:31
     * @param: originalFilename
     * @param: uploadPath
     * @param: file
     * @param: imageInfo
     * @return: com.darkecage.dcpicturebackend.model.dto.file.UploadPictureResult
     */
    private UploadPictureResult buildResult(String originalFilename, String uploadPath, File file, ImageInfo imageInfo) {
        //计算宽高
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0/picHeight, 2).doubleValue();
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        //返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * @title: 清理临时文件
     * @author: darkecage
     * @date: 2025/5/3 22:04
     * @param: file
     */
    public static void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        if (file != null) {
            //删除临时文件
            boolean deleteResult = file.delete();
            if (!deleteResult) {
                log.error("file delete error, filepath = {}", file.getAbsolutePath());
            }
        }
    }
}
