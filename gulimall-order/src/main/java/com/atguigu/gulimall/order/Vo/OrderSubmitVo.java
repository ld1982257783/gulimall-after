package com.atguigu.gulimall.order.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    //订单提交数据的Vo

    //收货地址的id
    private Long addrId;

    //支付方式
    private Integer payType;

    //无需提交需要购买的商品 去购物车再获取一遍



    //放重令牌
    private String orderToken;

    //应付价格 比价
    private BigDecimal payPrice;


    //String类型的备注
    private String note;

    //用户相关信息  直接去session中获取



}
