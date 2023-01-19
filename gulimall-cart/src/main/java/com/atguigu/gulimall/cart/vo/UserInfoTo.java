package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {
    private Long userId;         //登录用户的userid
    private String userKey;             //临时用户的userkey
    private Boolean TempUser = false;   //判断有没有临时用户
}
