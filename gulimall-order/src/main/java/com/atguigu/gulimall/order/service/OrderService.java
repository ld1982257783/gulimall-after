package com.atguigu.gulimall.order.service;

import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.Vo.OrderConfirmVo;
import com.atguigu.gulimall.order.Vo.OrderSubmitVo;
import com.atguigu.gulimall.order.Vo.PayVo;
import com.atguigu.gulimall.order.Vo.SubmitOrderResponseVo;
import com.atguigu.gulimall.order.entity.PayAsyncVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:16:19
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页返回所需要的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    //查询订单相关信息
    PayVo getOrderPay(String orderSn);

    PageUtils  queryPageWithItem(Map<String, Object> params);

    /**
     * 处理支付宝的返回数据
     * @param vo
     * @return
     */
    String handlePayResult(PayAsyncVo vo);

    /**
     * 创建秒杀哦单
     * @param seckillOrderTo
     */
    void createSeckillOrder(SeckillOrderTo seckillOrderTo);
}

