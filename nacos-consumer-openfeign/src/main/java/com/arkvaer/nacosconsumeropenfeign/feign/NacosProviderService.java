package com.arkvaer.nacosconsumeropenfeign.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author zhangchengtao
 * @date 2026/11/11 21:46
 */
@FeignClient("nacos-provider")
public interface NacosProviderService {

    @GetMapping("/user/hello")
    String hello();
}
