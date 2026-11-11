package com.arkvaer.nacosconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class NacosConsumerApplication {

    static void main(String[] args) {
        SpringApplication.run(NacosConsumerApplication.class, args);
    }

}
