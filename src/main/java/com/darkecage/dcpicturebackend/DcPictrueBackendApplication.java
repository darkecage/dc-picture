package com.darkecage.dcpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.darkecage.dcpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class DcPictrueBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DcPictrueBackendApplication.class, args);
    }

}
