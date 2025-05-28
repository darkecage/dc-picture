package com.darkecage.dcpicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.darkecage.dcpicturebackend.manager.websocket.PictureEditHandler;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * @title: 图片编辑事件处理器（消费者）
 * @author: darkecage
 * @date: 2025/5/28 21:25
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {
    @Resource
    private UserService userService;

    @Resource
    private PictureEditHandler pictureEditHandler;
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        //根据消息类型处理消息
        switch (messageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                //其他消息类型，参数有误，返回错误提示
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
                break;
        }
    }
}
