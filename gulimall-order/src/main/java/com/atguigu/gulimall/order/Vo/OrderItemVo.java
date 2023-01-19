package com.atguigu.gulimall.order.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {
    private Long skuId;
    private Boolean check = true;
    private String tittle;
    private String image;
    private List<String> skuAttr;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;


    //TODO  查询库存状态
    private boolean hasStock;
    private BigDecimal weight;


}
