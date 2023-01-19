package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/*
*   整合Mbatis-plus
*   1 导入依赖
*   2 配置
*     1） 配置数据源  连向那些数据库
*         1 导入数据库的驱动
*         2 在application.yml配置数据源相关信息
*     2）配置MYbatis-plus
*         1 使用MapperScan
*         2 告诉Mybatis-plus
*
* 2 逻辑删除
*   配置全局的逻辑删除规则   （省略）
*   配置逻辑删除的组件Bean （省略）
*   加上逻辑删除注解  TableLogic
*
*
* 5  模板引擎   引入thymeleaf的stater
*   1)  关闭缓存  静态资源都放在static文件夹下面  按照路径直接访问
*   2）  页面放到template页面下面  也可以直接访问
*           springboot访问项目的实收  默认会找 index
*
*
* 6  页面修改不重启服务器实时更新
*      1） 引入dev-tools
*      2） 修改完成  重新自动编译页面   ctrl shiift F9  前提要关闭thymeleaf缓存  代码配置  推荐重启
*
*
* 7  整合redis
*   1)、引入data-redis-starter
*   2）、简单配置redis的host port等信息
*   3）、使用redis自动配置的 stringRedisTemplate进行相关操作  哪里使用 哪里注入
*
*
*  8  redisson  整合分布式锁
*
* <!-- https://mvnrepository.com/artifact/org.redisson/redisson -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>3.12.0</version>
        </dependency>
    1）  创建redisson的单redis节点模式   需要注入 redissonClient
*
*
*
*
*
* 9  整合springcache 简化开发
*   1）引入依赖  spring-boot-starter-cache   spring-boot-starter-redis
*   2)  写配置
*       （1） 自动配置了那些
*           Cacheautoconfiguration 会导入  redisCacheConfiguration
*           自动配置好了缓存管理器  redisCacheManager
*       （3） 测试使用缓存
*           @Cacheable   将数据保存到缓存的操作
*           @CacheEvict  触发将数据从缓存中删除的操作
*           @CachePut    不影响方法执行更新缓存
*           @Caching     组合以上多个操作
*           @CacheConfig 在类级别共享缓存的相同配置
*
*           注意  ！！！  一定要开启缓存功能   @EnableCaching
*           spring.cache.type=redis
*       (4) 默认行为
*           1）如果缓存中有，方法不用调用 ！
*           2）key默认自动生成  缓存的名字 ：：SimpleKey[]（自动生成的key）
*           3）缓存的value值  默认使用jdk序列化机制  将序列化后的数据存到redis
*           4）默认ttl时间  -1
*           自定义
*               1）  指定生成的缓存的key  key属性指定 spel
*
*               2）  指定缓存的存活时间
*               3）  将数据保存为json模式
*
*
*
*   spring cache 的不足
*       1） 读模式 ：
*           缓存穿透 查询一个null数据  解决缓存空数据 ache-null-values=true
*           缓存击穿 大量并发请求查询一个刚好过期的数据加锁 默认是无加锁的  sync =true
*           缓存雪崩 大量的key同时过期  解决 加随机时间
*       2） 写模式
*           引入canal  感知到mysql的更新去更新数据库
*           读多写多   直接去数据库查询
*  总结
*       常规数据  读多写少 即时性  一致性要求不高的数据 完全可以使用spring cache
*       特殊数据  特殊设计
*
*
*
*   注解条件筛选
*   当容器中有tomm2组件的时候  相对应的配置才会生效
*   @ConditionalOnBean(name = "tomm2")
*
*   引入资源文件
*   @ImportResource("classpath:beans.xml")
*
*
*   与配置文件的属性进行绑定  属性必须要有getter setter方法
*   ConfigurationProperties+component配合使用  如果只加 ConfigurationProperties  相当于只是进行属性绑定 但是没有添加到容器中
*   @ConfigurationProperties(prefix = )
*
*   此注解是ConfigurationProperties是容器中没有component的时候
*   @EnableConfigurationProperties(mybatisConfig.class)
*
*
*   xxxAutoConfiguration----->组件----->xxxxproperties里面拿值  ----->application.properties
* */
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")   //开启远程调用功能
@SpringBootApplication
@MapperScan("com.atguigu.gulimall.product.dao")
@EnableDiscoveryClient  //开启服务的注册发现功能
public class GulimallProductApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(GulimallProductApplication.class, args);
//        String[] beanDefinitionNames = run.getBeanDefinitionNames();
//        for (String beanDefinitionName : beanDefinitionNames) {
//            System.out.println(beanDefinitionName);
//
//        }

    }

}
