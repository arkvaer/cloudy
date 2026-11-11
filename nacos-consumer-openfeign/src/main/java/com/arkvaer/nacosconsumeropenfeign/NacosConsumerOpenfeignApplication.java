package com.arkvaer.nacosconsumeropenfeign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.arkvaer.nacosconsumeropenfeign.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class NacosConsumerOpenfeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosConsumerOpenfeignApplication.class, args);
    }

}
