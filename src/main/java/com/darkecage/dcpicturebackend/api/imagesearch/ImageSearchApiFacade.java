package com.darkecage.dcpicturebackend.api.imagesearch;

import com.darkecage.dcpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.darkecage.dcpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.darkecage.dcpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.darkecage.dcpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {
    /**
     * @title: 搜索图片
     * @author: darkecage
     * @date: 2025/5/13 22:52
     * @param: imageUrl
     * @return: java.util.List<com.darkecage.dcpicturebackend.api.imagesearch.model.ImageSearchResult>
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageSearchResults = searchImage("https://www.codefather.cn/logo.png");
        System.out.println("结果列表" + imageSearchResults);
    }
}
