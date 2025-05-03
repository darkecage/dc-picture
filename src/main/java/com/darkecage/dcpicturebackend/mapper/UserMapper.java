package com.darkecage.dcpicturebackend.mapper;

import com.darkecage.dcpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Cage
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2025-04-30 21:58:53
* @Entity com.darkecage.dcpicturebackend.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




