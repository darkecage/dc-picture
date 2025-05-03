package com.darkecage.dcpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @title: 用户角色枚举
 * @author: darkecage
 * @date: 2025/4/30 22:30
 */
@Getter
public enum UserRoleEnum {

    User("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * @title: 根据值获取枚举
     * @author: darkecage
     * @date: 2025/4/30 22:35
     * @param: value
     * @return: com.darkecage.dcpicturebackend.model.enums.UserRoleEnum
     */
    public static UserRoleEnum getEnumByValue(String value){
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.getValue().equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }
}
