package com.atguigu.gulimall.order.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {

    //项目启动自动去rabbitmq进行创建对应组件  queue exchange binding





    @Bean
    public Queue orderDelayQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange"); //到期后以该路由键转发交换机
        arguments.put("x-dead-letter-routing-key","order.release.order"); //到期后以该路由键转发队列
        arguments.put("x-message-ttl",60000);

//        String name, boolean durable, boolean exclusive, boolean autoDelete) {
//		this(name, durable, exclusive, autoDelete, null
        //  持久化   是否排他 很多人都难连接   自动删除
        Queue queue = new Queue("order.delay.queue",true,false,false,arguments);
        return queue;
    }


    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }


    /**
     * 既当爹又当妈
     * @return
     */
    @Bean
    public Exchange orderEventExchange(){
        return new TopicExchange("order-event-exchange",true,false);
    }



    @Bean
    public Binding orderCreateOrderBinding(){

       return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",null);
    }


    @Bean
    public Binding orderReleaseOrderBinding(){

        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",null);
    }


    /**
     * 订单释放直接和库存释放进行绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",null);
    }


    //创建订单秒杀单的队列
    @Bean
    public Queue orderSeckillOrderQueue(){
        return new Queue("order.seckill.order.queue",true,false,false);
    }


    @Bean
    public Binding orderSeckillOrderQueueBinding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);
    }




}
