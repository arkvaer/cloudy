package com.arkvaer.nacosconsumeropenfeign.controller;

import com.arkvaer.nacosconsumeropenfeign.feign.NacosProviderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author zhangchengtao
 * @date 2026/11/10 17:04
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final NacosProviderService nacosProviderService;

    public UserController(NacosProviderService nacosProviderService) {
        this.nacosProviderService = nacosProviderService;
    }

    @GetMapping("/helloOpenFeign")
    public String helloConsumer() {
        return nacosProviderService.hello();
    }
}
