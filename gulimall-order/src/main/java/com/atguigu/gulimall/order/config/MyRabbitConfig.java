package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    //放一个消息类型转换器
    @Bean
    public MessageConverter messageConverter(){
       return new Jackson2JsonMessageConverter();
    }




    /**   为了确保发送  message -》 broker    中途服务宕机 引发的一系列异常  能够保证消息不丢死  相当于sql事务
     *          1 定制rabbitTemplate
     *          2 设置确认回调  spring.rabbitmq.publisher-confirm-type=correlated
     *    可靠抵达 returnCallback
     *          spring.rabbitmq.publisher-returns=true
     *          #只要抵达队列 以异步方式优先回调我们这个returnconfigrm
     *          spring.rabbitmq.template.mandatory=true
     *
     *
     *     消费端确认(保证每个消息被正确消费 此时才可以broker删除这个消息)
     *          1 默认是自动确认的 只要消息接收到客户端会自动确认 服务端会移除这个消息
     *            问题
     *                  我们收到很多消息 自动回复给服务器ack 只有一个消息处理成功 宕机了 发生消息丢失
     *                  手动确认模式  没有明确告诉mq 货物被签收  没有ack消息一直是unacked状态  即使Consumer宕机  下次也不会丢失 会重现变为ready状态 下一次有新的Consumber进来会发给他
     *
     *
     *
     */
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


}
