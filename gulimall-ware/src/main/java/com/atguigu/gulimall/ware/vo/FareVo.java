package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {

    private MemberAdressVo memberAdressVo;

    private BigDecimal fare;
}
