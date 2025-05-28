package com.darkecage.dcpicturebackend.manager.websocket.disruptor;

import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.darkecage.dcpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * @title: 图片编辑事件
 * @author: darkecage
 * @date: 2025/5/28 21:22
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
