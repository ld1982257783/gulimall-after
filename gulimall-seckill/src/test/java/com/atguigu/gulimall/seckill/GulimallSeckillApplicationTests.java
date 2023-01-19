package com.atguigu.gulimall.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class GulimallSeckillApplicationTests {

    @Test
    void contextLoads() {

        LocalDate now = LocalDate.now();
        System.out.println(now);
    }

}
