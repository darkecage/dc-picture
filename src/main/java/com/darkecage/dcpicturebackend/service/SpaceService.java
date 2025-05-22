package com.darkecage.dcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.darkecage.dcpicturebackend.model.dto.space.SpaceAddRequest;
import com.darkecage.dcpicturebackend.model.dto.space.SpaceQueryRequest;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Cage
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-05-11 21:58:12
*/
public interface SpaceService extends IService<Space> {
    
    /**
     * @title: 校验空间
     * @author: darkecage
     * @date: 2025/5/11 22:46
     * @param: space
     * @param: add
     */
    void validSpace(Space space, boolean add);


    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/4 0:50
     * @param: spaceQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcspacebackend.model.entity.Space>
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * @title: 获取空间包装类（单条）
     * @author: darkecage
     * @date: 2025/5/4 1:39
     * @param: space
     * @param: request
     * @return: com.darkecage.dcspacebackend.model.vo.SpaceVO
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * @title: 获取空间包装类（分页）
     * @author: darkecage
     * @date: 2025/5/4 1:45
     * @param: spacePage
     * @param: request
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.darkecage.dcspacebackend.model.vo.SpaceVO>
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * @title: 根据空间级别填充空间对象
     * @author: darkecage
     * @date: 2025/5/11 23:44
     * @param: space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * @title: 创建空间
     * @author: darkecage
     * @date: 2025/5/12 0:41
     * @param: spaceAddRequest
     * @param: loginUser
     * @return: long
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * @title: 校验空间权限
     * @author: darkecage
     * @date: 2025/5/16 15:54
     * @param: loginUser
     * @param: space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
