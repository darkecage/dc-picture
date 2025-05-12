package com.darkecage.dcpicturebackend.mapper;

import com.darkecage.dcpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Cage
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2025-05-11 21:58:12
* @Entity com.darkecage.dcpicturebackend.model.entity.Space
*/
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {

}




