package com.darkecage.dcpicturebackend.controller;

import com.darkecage.dcpicturebackend.common.BaseResponse;
import com.darkecage.dcpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
