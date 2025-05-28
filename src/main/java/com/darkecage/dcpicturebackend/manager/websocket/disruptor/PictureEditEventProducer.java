package com.darkecage.dcpicturebackend.manager.websocket.disruptor;

import com.darkecage.dcpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.darkecage.dcpicturebackend.model.entity.User;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @title: 图片编辑事件生产者
 * @author: darkecage
 * @date: 2025/5/28 21:45
 */
@Component
@Slf4j
public class PictureEditEventProducer {
    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * @title: 生产事件
     * @author: darkecage
     * @date: 2025/5/28 21:51
     * @param: pictureEditRequestMessage
     * @param: session
     * @param: user
     * @param: pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        //获取到可以放置事件的位置
        long next = ringBuffer.next();
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        //发布事件
        ringBuffer.publish(next);
    }

    /**
     * @title: 优雅停机
     * @author: darkecage
     * @date: 2025/5/28 21:53
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
