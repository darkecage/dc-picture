package com.darkecage.dcpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.darkecage.dcpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * @title: 将本地文件上传到 COS
     * @author: darkecage
     * @date: 2025/5/3 19:15
     * @param: key
     * @param: file
     * @return: com.qcloud.cos.model.PutObjectResult
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * @title: 获取COS的文件
     * @author: darkecage
     * @date: 2025/5/3 19:14
     * @param: key
     * @return: com.qcloud.cos.model.COSObject
     */
     public COSObject getObject(String key) {
         GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
         return cosClient.getObject(getObjectRequest);
     }

     /**
      * @title: 上传对象（附带图片信息）
      * @author: darkecage
      * @date: 2025/5/3 20:53
      * @param: key
      * @param: file
      * @return: com.qcloud.cos.model.PutObjectResult
      */
     public PutObjectResult putPictureObject(String key, File file) {
         PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
         //对图片进行处理(获取基本信息也被视作为一种图片的处理)
         PicOperations picOperations = new PicOperations();
         //设置为1表示返回原图信息
         picOperations.setIsPicInfo(1);
         //图片处理规则列表
         List<PicOperations.Rule> rules = new ArrayList<>();
         //0.图片压缩（转成webp格式）
         String webpKey = FileUtil.mainName(key) + ".webp";
         PicOperations.Rule compressRule = new PicOperations.Rule();
         compressRule.setFileId(webpKey);
         compressRule.setBucket(cosClientConfig.getBucket());
         compressRule.setRule("imageMogr2/format/webp");
         rules.add(compressRule);
         //1.缩略图处理, 仅对>20KB的图片生成缩略图
         if (file.length() > 20 * 1024) {
             PicOperations.Rule thumbnailRule = new PicOperations.Rule();
             //拼接缩略图的路径
             String suffix = FileUtil.getSuffix(key);
             if (StrUtil.isBlank(suffix)) {
                 suffix = "png";
             }
             String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + suffix;
             thumbnailRule.setFileId(thumbnailKey);
             thumbnailRule.setBucket(cosClientConfig.getBucket());
             //缩放规则
             thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
             rules.add(thumbnailRule);
         }
         //构造处理参数
         picOperations.setRules(rules);
         putObjectRequest.setPicOperations(picOperations);
         return cosClient.putObject(putObjectRequest);
     }

     /**
      * @title: 删除对象
      * @author: darkecage
      * @date: 2025/5/10 22:25
      * @param: key
      */
     public void deleteObject(String key) {
         cosClient.deleteObject(cosClientConfig.getBucket(), key);
     }
}
