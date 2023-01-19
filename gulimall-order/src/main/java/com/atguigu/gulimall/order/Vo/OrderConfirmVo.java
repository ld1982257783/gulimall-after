package com.atguigu.gulimall.order.Vo;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页要用的数据
public class OrderConfirmVo {


    ///收货地址   ums_member_receive_address 表
    @Setter @Getter
    List<MemberAdressVo> adress;


    //送货清单  商品信息  选中的商品
    @Setter @Getter
    List<OrderItemVo> items;


    //订单防重复令牌
    @Setter @Getter
    String orderToken;


    //判断商品一共几件   不是几种类型哦
    private Integer count;

    @Setter @Getter
    Map<Long,Boolean> stocks;

    public Integer getCount() {
        Integer size = 0;
        if(items!=null){
            for (OrderItemVo item : items) {
                size+=item.getCount();
            }
        }
        return size;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    //发票记录 ...


    //优惠券信息
    @Setter @Getter
    Integer integeration;

    BigDecimal total;  //订单总额

    BigDecimal payPrice;  //应付价格

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setPayPrice(BigDecimal payPrice) {
        this.payPrice = payPrice;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        for (OrderItemVo item : items) {
            BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
            sum = sum.add(multiply);
        }
        return sum;
    }

    public BigDecimal getPayPrice() {
        return getTotal() ;
    }


}
