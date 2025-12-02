package com.zc.anti.auto.controller;

import com.zc.anti.auto.redisson.Demo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedissonController {

    @GetMapping("/test/redisson")
    public void testRedisson() throws InterruptedException {
        Demo demo = new Demo();
        demo.produce();
    }
}
