package com.arkvaer.nacosconsumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author zhangchengtao
 * @date 2026/11/10 17:04
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Value("${service-url.nacos-user-service}")
    private String severURL;

    private final RestTemplate restTemplate;

    public UserController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/hello")
    public String helloConsumer() {
        return restTemplate.getForObject(severURL + "/user/hello", String.class);
    }
}
