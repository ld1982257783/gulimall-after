package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

//超时三十分钟的订单  进行解锁库存
@RabbitListener(queues = {"order.release.order.queue"})
@Service
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    //监听消息的获取
    @RabbitHandler
    public void Listener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        String orderSn = orderEntity.getOrderSn();
        System.out.println("拿到消息，一分钟后监听到了"+orderSn+message+"-------"+message.getMessageProperties().getConsumerTag());
        try {
            orderService.closeOrder(orderEntity);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }


}
