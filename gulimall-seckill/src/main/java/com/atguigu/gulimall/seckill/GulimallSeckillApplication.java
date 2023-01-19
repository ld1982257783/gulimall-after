package com.atguigu.gulimall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 整合 sentinel
 *    1 导入依赖服务限流 熔断 降级  引入spring-cloud sentinel
 *     <dependency>
 *         <groupId>com.alibaba.cloud</groupId>
 *         <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
 *     </dependency>
 *    2 下载sentinel的控制台
 *    3 配置sentinel控制台地址信息
 *    4 在控制台调整参数 默认所有的流控规则保存在内存中 重启失效
 *
 * 2 每一个微服务都导入信息审计模块  actuator spring-boot的监控状态信息
 *      并配置 management.endpoints.web.exposure.include=*
 *
 * 3 自定义sentinel的流控返回
 *
 * 4 使用sentinel来保护feign的远程调用 熔断 降级
 *      1) 调用方的熔断保护，feign.sentinel.enableed=true
 *      2) 调用方手动指定远程服务的降级策略  远程服务被降级处理 默认触发我们的熔断回调方法
 *      3) 超大流量的时候，必须牺牲一些远程服务，在服务的提供方(远程服务指定)降级策略
 *          提供方是在运行 但是不运行自己的业务逻辑，返回的是默认的降级数据（限流的数据）
 *
 * 5 自定义受保护的资源
 *      1) try(Entry entry = SphU.entry("SeckillSkus")){
 *          业务逻辑
 *      }
 *      2) 基于注解
 *      @SentinelResource
 *      *
 *      * blockHandler 函数会在原方法被限流/降级/系统保护的时候调用  而fallback函数会针对所有类型的异常  可以写在其他列  必须是静态方法
 *      * @return
 *      无论是1 还是2 一定要配置限流后的默认返回
 *      url请求可以设置统一返回
 *
 */



@EnableRedisHttpSession
@EnableFeignClients
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableDiscoveryClient
public class GulimallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillApplication.class, args);
    }

}
