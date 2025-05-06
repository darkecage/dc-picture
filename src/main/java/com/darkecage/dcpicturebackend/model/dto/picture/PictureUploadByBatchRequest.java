package com.darkecage.dcpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {
    private static final long serialVersionUID = -5151404538625609197L;
    /**
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
