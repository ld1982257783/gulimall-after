package com.atguigu.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对redisson的使用都是通过redissonClient
     * @return
     * @throws IOException
     */

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        //创建配置   redis://代表使用安全连接
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.43.2:6379");

        //2根据config创建出redissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
