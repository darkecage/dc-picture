package com.darkecage.dcpicturebackend.mapper;

import com.darkecage.dcpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Cage
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-05-03 20:27:19
* @Entity com.darkecage.dcpicturebackend.model.entity.Picture
*/
@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

}




