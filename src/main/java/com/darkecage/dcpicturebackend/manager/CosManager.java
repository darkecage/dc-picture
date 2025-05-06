package com.darkecage.dcpicturebackend.manager;

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
         //构造处理参数
         putObjectRequest.setPicOperations(picOperations);
         return cosClient.putObject(putObjectRequest);
     }
}
