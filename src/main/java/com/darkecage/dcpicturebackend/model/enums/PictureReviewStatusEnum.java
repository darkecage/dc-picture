package com.darkecage.dcpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @title: 图片审核状态枚举
 * @author: darkecage
 * @date: 2025/4/30 22:30
 */
@Getter
public enum PictureReviewStatusEnum {

    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
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
    public static PictureReviewStatusEnum getEnumByValue(Integer value){
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getValue() == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
