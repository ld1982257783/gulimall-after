package com.atguigu.gulimall.product.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

//开启属性配置的绑定功能    缓存相关配置
@EnableConfigurationProperties(CacheProperties.class)
@Configuration
@EnableCaching
public class MyCacheConfig {
    //自定义配置 解决redis的序列化问题

    /**
     *   配置文件中的东西没有用上
     * @configurationProperties(prefix = "spring.cache")
     * public class CacheProperties
     *
     * 2要让他生效
     * @EnableConfigurationProperties(CacheProperties.class)
     * @return
     *
     *
     */


    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties){
        //首先获取默认配置
        // 配置序列化（解决乱码的问题）
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        //entryTtl(Duration.ofHours(1)); // 设置缓存有效期一小时
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 配置序列化（解决乱码的问题）
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        CacheProperties.Redis redis = cacheProperties.getRedis();
        if (redis.getTimeToLive() != null) {
            config = config.entryTtl(redis.getTimeToLive());
        }
        if (redis.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redis.getKeyPrefix());
        }
        if (!redis.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redis.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }



}
