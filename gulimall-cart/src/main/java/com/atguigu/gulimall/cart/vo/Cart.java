package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;


/**
 * 购物车  需要计算的属性  需要重新计算
 */
public class Cart {

    private List<CartItem> items;

    private Integer countNum;  //商品数量

    private Integer countType;  //商品有几种类型

    private BigDecimal totalAmount;  //商品总价

    private BigDecimal reduce = new BigDecimal("0.00");    //商品减免


    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        //计算当前购物车里面有多少购物项  包含  重复的商品
        int counts = 0;
        if(items!=null && items.size() > 0){
            for (CartItem item : items) {
                counts+=item.getCount();
            }
        }
        return counts;
    }


    public Integer getCountType() {
        int types = 0;
        if(items!=null && items.size() > 0){
            for (CartItem item : items) {
                types+=1;
            }
        }
        return types;
    }


    public BigDecimal getTotalAmount() {
        //计算总价
        BigDecimal amount = new BigDecimal("0");
        //计算购物项总价
        if(items!=null && items.size()>0){
            for (CartItem item : items) {
                if(item.getCheck()){
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }
        //前去优惠总价
        BigDecimal subtract = amount.subtract(this.getReduce());

        return subtract;

    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
