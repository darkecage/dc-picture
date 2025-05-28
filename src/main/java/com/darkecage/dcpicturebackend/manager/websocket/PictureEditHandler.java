package com.darkecage.dcpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.darkecage.dcpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.darkecage.dcpicturebackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @title: 图片编辑 WebSocket处理器
 * @author: darkecage
 * @date: 2025/5/27 19:58
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    //每张图片的编辑状态，Key：pictureId，value：当前正在编辑的用户的ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    //保存所有连接的会话，Key：pictureId，value：用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * @title: 连接建立成功
     * @author: darkecage
     * @date: 2025/5/28 1:21
     * @param: session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);//保留父类逻辑
        //保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        //构造响应，发送加入编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        //广播给所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * @title: 收到前端发送的消息，根据消息类别处理消息
     * @author: darkecage
     * @date: 2025/5/28 1:39
     * @param: session
     * @param: message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);//保留父类逻辑
        //获取消息内容，将JSON转换为PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        //从Session属性中获取公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        //根据消息类型处理消息(生产消息到 Distuptor 队列中)
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * @title: 进入编辑状态
     * @author: darkecage
     * @date: 2025/5/28 2:04
     * @param: pictureEditRequestMessage
     * @param: session
     * @param: user
     * @param: pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            //设置用户正在编辑该图片
            pictureEditingUsers.put(pictureId, user.getId());
            //构造响应，发送开始编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            //广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }

    }

    /**
     * @title: 退出编辑状态
     * @author: darkecage
     * @date: 2025/5/28 2:04
     * @param: pictureEditRequestMessage
     * @param: session
     * @param: user
     * @param: pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //当前编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        //确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //移除用户正在编辑图片
            pictureEditingUsers.remove(pictureId);
            //构造响应，发送开始编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            //广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * @title: 处理编辑状态
     * @author: darkecage
     * @date: 2025/5/28 2:05
     * @param: pictureEditRequestMessage
     * @param: session
     * @param: user
     * @param: pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //正在编辑的用户ID
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        // 确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            //构造响应，发送具体操作通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s 执行了 %s 操作", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            //广播给除了当前用户的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * @title: 关闭连接
     * @author: darkecage
     * @date: 2025/5/28 18:35
     * @param: session
     * @param: status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);//保留父类逻辑
        //从Session属性中获取公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        //移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        //删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        //构造响应，发送具体操作通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        //广播给所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * @title: 广播给该图片的所有用户(支持排除掉某个Session)
     * @author: darkecage
     * @date: 2025/5/27 20:09
     * @param: pictureId
     * @param: pictureEditResponseMessage
     * @Param: excludeSession
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            //解决Long类型精度丢失
            //创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            //配置序列化，将Long类型转为String
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            //发送消息,序列化成JSON字符串
            String messageStr = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(messageStr);
            for (WebSocketSession session : webSocketSessions) {
                //排除掉的Session不发送
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * @title: 广播给该图片的所有用户
     * @author: darkecage
     * @date: 2025/5/27 20:09
     * @param: pictureId
     * @param: pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
