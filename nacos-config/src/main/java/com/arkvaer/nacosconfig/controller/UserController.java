package com.arkvaer.nacosconfig.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.util.JSONPObject;

import java.util.Map;

/**
 *
 * @author zhangchengtao
 * @date 2026/11/12 08:15
 */
@RestController
@RefreshScope
public class UserController {

    @Value("${config.info}")
    private String msg;


    @Value("${cfg.version: 1111}")
    private String version;

    @GetMapping("/config/hello")
    public Map<String, String> hello() {
        System.out.println(msg);
        System.out.println(version);
        return Map.of("msg", msg, "version", version);
    }
}
