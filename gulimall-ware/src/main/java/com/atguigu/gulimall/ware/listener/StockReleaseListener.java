package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.mq.OrderTo;
import com.atguigu.common.mq.StockLockedTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = {"stock.release.stock.queue"})
@Service
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;



    //此方法只是针对  库存锁定成功  但是父类调用模块后续的业务模块报错  回滚  然后去解锁库存
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {

        try {
            System.out.println("收到解锁库存的消息--------------------------");
            wareSkuService.unlockStock(stockLockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }


//        Long id = stockLockedTo.getId(); //库存工作单的id
//        StockDetailTo detail = stockLockedTo.getDetail();
//        //查询此id有没有锁定库存的信息
//        WareOrderTaskEntity byId = wareOrderTaskService.getById(id);
//        if(byId != null){
//            //需要解锁  证明库存锁定成功 了  到底要不要解锁
//            // 1 查询数据库 关于这个订单的锁定库存信息
//            // 有 证明库存锁定成功了
//            // 解锁 订单情况
//            // 1    没有这个订单  必须解锁
//            // 2    有这个订单  不是解锁库存
//            //          查看订单状态  订单状态如果已取消 解锁库存
//            //              没有取消  不能解锁库存
//            Long id1 = stockLockedTo.getId();
//            WareOrderTaskEntity byId1 = wareOrderTaskService.getById(id1);
//            String orderSn = byId1.getOrderSn();
//            //TODO  注意 此处远程调用  order服务的拦截器会拦截此请求 需要去order拦截器进行设置
//            R r = orderFeignService.getOrderStatus(orderSn);
//            if(r.getCode() == 0){
//                //订单数据返回成功
//                OrderVo data = r.getData(new TypeReference<OrderVo>() {});
//                if(data == null || data.getStatus() == 4){
//                    //订单已经被取消了  才能解锁库存
//                    unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),id);
//                    channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//                }
//            }else{
//                channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
//            }
//
//        }else{
//            //无须解锁
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//        }

    }





    //根据接收来的对象类型 进行区分
    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo,Message message,Channel channel) throws IOException {
        System.out.println("订单关闭 ，准备解锁库存");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);

        }

    }

}
