package com.atguigu.gulimall.order.Vo;

import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockVo {

    //给哪个订单锁库存  订单号
    private String orderSn;

    private List<OrderItemVo> locks;  //需要锁住的所有库存信息




}
