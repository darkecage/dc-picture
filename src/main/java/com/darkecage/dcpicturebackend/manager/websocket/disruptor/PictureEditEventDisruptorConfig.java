package com.darkecage.dcpicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @title: 图片编辑事件 Disruptor 配置
 * @author: darkecage
 * @date: 2025/5/28 21:32
 */
@Configuration
public class PictureEditEventDisruptorConfig {
    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer(){
        //定义 RingBuffer 的大小
        int bufferSize = 1024 * 256;
        //创建disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create()
                        .setNamePrefix("pictureEditEventDisruptor")
                        .build()
        );
        //绑定消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        //启动 disruptor
        disruptor.start();
        return disruptor;
    }
}
