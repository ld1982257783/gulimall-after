package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfig {


    @Autowired
    RabbitTemplate rabbitTemplate;


    //放一个消息类型转换器
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }


    @PostConstruct   //MyRabbitConfig对象创建完毕后  执行这个方法
    public void initRabbitTemplate(){
        //设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData  当前消息的唯一关联数据 (这个是消息的唯一id)
             * @param ack   消息是否成功收到
             * @param cause  失败的原因
             */
            @Override  //从生产者到broker
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+ack+"]==>{"+cause+"]");
            }
        });

        //设置消息抵达队列的确认回调
        //Message message, int replyCode, String replyText, String exchange, String routingKey
        //只要消息没有投递给指定的队列 就触发这个回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback(){

            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("Fail Message["+message+"]==>replyCode["+replyCode+"]==>replyText"+replyText+"]==>exchange"+exchange+"]==>router-key["+routingKey);

            }
        });


    }


    //创建队列 交换机
    //这些组件默认  要在第一次连接rabbitmq的时候才会创建 需要写个监听队列的方法
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(){
//
//    }


    @Bean
    public Exchange stockEventExchange(){
        return new TopicExchange("stock-event-exchange",true,false);
    }



    @Bean
    public Queue stockReleaseStockQueue(){
        return new Queue("stock.release.stock.queue",true,false,false);
    }



    @Bean
    public Queue stockDelayQueue(){
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange","stock-event-exchange");
        args.put("x-dead-letter-routing-key","stock.release");
        args.put("x-message-ttl",120000);
        return new Queue("stock.delay.queue",true,false,false,args);
    }


    @Bean
    public Binding stockReleaseBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",null);
    }


    @Bean
    public Binding stockLockedBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",null);
    }






}
