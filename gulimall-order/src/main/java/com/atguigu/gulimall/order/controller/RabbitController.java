package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 设置 new CorrelationData(UUID.randomUUID().toString() 消息的唯一id
     * @param num
     * @return
     */
    @GetMapping("/sendMq")
    public String sendMessage(@RequestParam(value = "num",defaultValue = "10") Integer num){
        //发送一个对象
        for(int i = 1;i<=10;i++){
            if(i%2==0){
                OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
                orderReturnReasonEntity.setId(1L);
                orderReturnReasonEntity.setCreateTime(new Date());
                orderReturnReasonEntity.setName("哈哈");
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderReturnReasonEntity,new CorrelationData(UUID.randomUUID().toString()));
                log.info("消息发送完成"+orderReturnReasonEntity);

            }else{
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity,new CorrelationData(UUID.randomUUID().toString()));
                log.info("消息发送完成"+orderEntity);

            }
        }
        return "ok";
    }

}
