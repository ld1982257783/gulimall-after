package com.atguigu.gulimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * 引入rabbitmq启动器  RabbitAutoConfiguration会自动生效
 * 给容器中自动配置了 RabbitTemplate   AmqpAdmin   CachingConnectionFactory   RabbitMessagingTemplate
 * @enablerabbit  enableXxx
 *
 * 所有的属性都是在这里进行配置  @ConfigurationProperties(prefix = "spring.rabbitmq")
 *
 * 3 给配置文件中配置相关信息
 *
 *
 * 4 开启@EnableRabbit  的相关配置
 *
 * 5  监听消息 使用@EnableRabbit必须有 @EnableRabbit
 *      @EnableRabbit： 类+方法上
 *      @RabbitHandler 标注在方法上 (重载区分不同的消息)
 *
 * 6 消费端确认  (保证每个消息被正确消费  此时才可以broker删除这个消息 )
 *      1 默认是自动确认的  只要消息 接收到   客户单会自动确认  服务端就会有移除这个消息
 *
 *
 *
 * 本地事务失效问题
 * 同一个 对象内事务方法互调默认失效  原因 绕过了代理对象 事务使用代理对象来控制的
 * 解决 使用代理对象来调用事务方法
 *      1） 引入aop-starter  aspectjweaver
 *      2） @EnableAspectJAutoProxy: 开启aspectjweaver动态代理  以后所有的动态代理都是aspectj创建（即使没有接口也可以创建动态dialing）
 *      3)  exposeProxy = true对外暴露代理对象
 *          本类互调用代理对象
 *
 *
 * Seata控制分布式事务
 *  1)  每一个微服务先必须创建undo_log表
 *  2） 安装事务协调器 seata-server  https://github.com/seata/seata/releases
 *  3)  导入依赖 spring-cloud-starter-alibaba-seata
 *          registry.conf  注册中心配置 修改registry type = nacos
 *          file.conf
 *          开启@GlobalTransactional
 *       3 所有想要用到分布式事务的微服务使用seata DataSourceProxy代理自己的数据源
 *       4 每个微服务，都必须导入file.conf registry.conf
 *  注入DataSourceProxy
 *      因为Seata通过代理数据源实现分支事务 如果没有注入  事务无法成功回滚
 *       让seata包装你自己的数据源
 *      5  所有想要使用分布式事务的微服务seata DataSourceProxy代理自己的数据源
 *      6 每个微服务都要导入 file.conf  和registry.conf
 *      7   给分布式大事务标注 @GlobalTransactional
 *
 *
 *
 *
 *
 *
 */

@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients
@EnableRedisHttpSession
@EnableRabbit
@SpringBootApplication
@MapperScan("com.atguigu.gulimall.order.dao")
@EnableDiscoveryClient
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
