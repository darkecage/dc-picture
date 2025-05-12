package com.darkecage.dcpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.mapper.SpaceMapper;
import com.darkecage.dcpicturebackend.model.dto.space.SpaceAddRequest;
import com.darkecage.dcpicturebackend.model.dto.space.SpaceQueryRequest;
import com.darkecage.dcpicturebackend.model.entity.Space;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.enums.SpaceLevelEnum;
import com.darkecage.dcpicturebackend.model.vo.SpaceVO;
import com.darkecage.dcpicturebackend.model.vo.UserVO;
import com.darkecage.dcpicturebackend.service.SpaceService;
import com.darkecage.dcpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author Cage
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-05-11 21:58:12
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * @title: 校验空间
     * @author: darkecage
     * @date: 2025/5/11 22:18 
     * @param: space
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        //修改数据时校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        //修改数据时空间级别校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }

    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/4 0:50
     * @param: spaceQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.Space>
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(StrUtil.isNotBlank(spaceName), "spaceName", spaceName)
                .eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @title: 获取空间包装类（单条）
     * @author: darkecage
     * @date: 2025/5/4 1:39
     * @param: space
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.vo.SpaceVO
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * @title: 获取空间包装类（分页）
     * @author: darkecage
     * @date: 2025/5/4 1:45
     * @param: spacePage
     * @param: request
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.darkecage.dcpicturebackend.model.vo.SpaceVO>
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        //关联查询用户信息
        //userId去重
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        //根据userId进行User对象分组
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            UserVO user = null;
            if (userIdListMap.containsKey(userId)){
                user = userService.getUserVO(userIdListMap.get(userId).get(0));
            }
            spaceVO.setUser(user);
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * @title: 根据空间级别填充空间对象
     * @author: darkecage
     * @date: 2025/5/11 23:44
     * @param: space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() != null) {
                space.setMaxCount(maxCount);
            }
        }

    }

    /**
     * @title: 创建空间
     * @author: darkecage
     * @date: 2025/5/12 0:41
     * @param: spaceAddRequest
     * @param: loginUser
     * @return: long
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1校验参数默认值
        //转换实体类 和 DTO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        //2校验参数
        this.validSpace(space, true);
        //3校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        //4控制同一用户只能创建一个私有空间
        Map<Long, Object> lockMap = new ConcurrentHashMap<>();
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try{
                //编程式事务
                Long newSpaceId = transactionTemplate.execute(status -> {
                    //是否已有空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .exists();
                    //如果已有空间，就不能再创建
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有一个私有空间");
                    //创建
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    //返回新写入的数据id
                    return space.getId();
                });
                return newSpaceId;
            } finally {
                lockMap.remove(userId);
            }

        }
    }
}




