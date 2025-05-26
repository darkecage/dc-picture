package com.darkecage.dcpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.darkecage.dcpicturebackend.constant.UserConstant;
import com.darkecage.dcpicturebackend.exception.BusinessException;
import com.darkecage.dcpicturebackend.exception.ErrorCode;
import com.darkecage.dcpicturebackend.exception.ThrowUtils;
import com.darkecage.dcpicturebackend.manager.auth.StpKit;
import com.darkecage.dcpicturebackend.model.dto.user.UserQueryRequest;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.model.enums.UserRoleEnum;
import com.darkecage.dcpicturebackend.model.vo.LoginUserVO;
import com.darkecage.dcpicturebackend.model.vo.UserVO;
import com.darkecage.dcpicturebackend.service.UserService;
import com.darkecage.dcpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Cage
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-30 21:58:53
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * @title: 用户注册
     * @author: darkecage
     * @date: 2025/4/30 23:41
     * @param: userAccount
     * @param: userPassword
     * @param: checkPassword
     * @return: long
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }

        //检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        //密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        //插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("noName");
        user.setUserRole(UserRoleEnum.User.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * @title: 用户登录
     * @author: darkecage
     * @date: 2025/5/1 3:41
     * @param: userAccount
     * @param: userPassword
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.vo.LoginUserVO
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }
        //对用户传递的密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);

        //查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        //不存在，抛异常
        if (user == null) {
            log.error("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        //保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * @title: 获取脱媒类的用户信息
     * @author: darkecage
     * @date: 2025/5/1 3:40
     * @param: user
     * @return: com.darkecage.dcpicturebackend.model.vo.LoginUserVO
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * @title: 获取脱敏后的用户信息
     * @author: darkecage
     * @date: 2025/5/3 2:27
     * @param: user
     * @return: com.darkecage.dcpicturebackend.model.vo.UserVO
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * @title: 获取脱敏后的用户列表
     * @author: darkecage
     * @date: 2025/5/3 2:27
     * @param: userList
     * @return: java.util.List<com.darkecage.dcpicturebackend.model.vo.UserVO>
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //从数据库中查询
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * @title: 获取加密后的密码
     * @author: darkecage
     * @date: 2025/5/1 1:53
     * @param: userPassword
     * @return: java.lang.String
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        //混淆 加盐
        final String SALT = "darkecage";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * @title: 用户注销
     * @author: darkecage
     * @date: 2025/5/3 0:15
     * @param: request
     * @return: com.darkecage.dcpicturebackend.model.entity.User
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未登录");
        }
        //移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * @title: 获取查询条件
     * @author: darkecage
     * @date: 2025/5/3 2:37
     * @param: userQueryRequest
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.darkecage.dcpicturebackend.model.entity.User>
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @title: 判断是否为管理员
     * @author: darkecage
     * @date: 2025/5/4 0:01
     * @param: user
     * @return: boolean
     */
    @Override
    public boolean isAdmin(User user) {
//        if (user == null) {
//            return false;
//        }
//        if (!user.getUserRole().equals(UserRoleEnum.ADMIN.getValue())) {
//            return false;
//        }
//        return true;
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




