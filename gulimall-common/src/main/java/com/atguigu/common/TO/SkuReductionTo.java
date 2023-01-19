package com.atguigu.common.TO;

import com.atguigu.gulimall.product.VO.MemberPrice;
import lombok.Data;

import java.util.List;

@Data
public class SkuReductionTo {

    private Long skuId;
    private int fullCount;
    private double discount;
    private int countStatus;
    private int fullPrice;
    private int reducePrice;
    private int priceStatus;

    private List<MemberPrice> memberPrice;
}
