package com.darkecage.dcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.darkecage.dcpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.darkecage.dcpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.darkecage.dcpicturebackend.model.entity.SpaceUser;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Cage
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-05-17 23:33:16
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * @title: 校验空间成员
     * @author: darkecage
     * @date: 2025/5/20 14:08
     * @param: spaceUser
     * @param: add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);


    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/20 14:08
     * @param: spaceUserQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.SpaceUser>
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * @title: 获取空间成员包装类（单条）
     * @author: darkecage
     * @date: 2025/5/20 14:07
     * @param: spaceUser
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.vo.SpaceUserVO
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * @title: 获取空间成员包装类
     * @author: darkecage
     * @date: 2025/5/20 14:08
     * @param: spaceUserList
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.SpaceUserVO>
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);


    /**
     * @title: 创建空间成员
     * @author: darkecage
     * @date: 2025/5/20 14:08
     * @param: spaceUserAddRequest
     * @return: long
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

}
