package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {


    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 如何创建 exchange  queue  binding
     * 如何收发消息
     *  1）  使用amqpadmin进行创建
     */
    @Test
    void contextLoads() {
        //String name, boolean durable, boolean autoDelete
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
        System.out.println("Exchange[]创建成功，hello-java-exchange");

    }


    //创建队列
    @Test
    void queueText(){

        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("hello-java-queue队列创建成功");


    }

    //创建绑定
    @Test
    void create(){
        //destination 目的地   destinationType目的地类型
        //exchange 交换机      routingKey路由键
        //arguments 自定义参数
//        (String destination, Binding.DestinationType destinationType, String exchange, String routingKey,
//                @Nullable Map<String, Object> arguments) {
        //将exchange指定的交换机和目的地进行绑定   routingKey路由键 作为指定的路由键
        Binding binding = new Binding("hello-java-queue",Binding.DestinationType.QUEUE
                ,"hello-java-exchange","hello.java",null);
        amqpAdmin.declareBinding(binding);
        log.info("binding创建成功","hello-java-binding");
    }



    //测试发送消息
    @Test
    void sendMessageText(){
        //1 发送消息
        //如果发送的消息是个对象  我们会使用序列化的机制发送出去  所以发送的对象必须实现序列化接口

        //如果发送是对象类型消息可以是一个json
        String message = "hello word";

        //发送一个对象
        for(int i = 1;i<=10;i++){
            if(i%2==0){
                OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
                orderReturnReasonEntity.setId(1L);
                orderReturnReasonEntity.setCreateTime(new Date());
                orderReturnReasonEntity.setName("哈哈");
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderReturnReasonEntity);
                log.info("消息发送完成"+orderReturnReasonEntity);

            }else{
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity);
                log.info("消息发送完成"+orderEntity);

            }
        }

    }



}
