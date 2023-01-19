package com.atguigu.gulimall.order.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAdressVo memberAdressVo;

    private BigDecimal fare;

}
