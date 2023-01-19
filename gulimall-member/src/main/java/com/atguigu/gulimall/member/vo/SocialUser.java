package com.atguigu.gulimall.member.vo;

import lombok.Data;

//社交登录返回信息
@Data
public class SocialUser {

    private String access_token;
    private String remind_in;
    private String expires_in;
    private String uid;
    private String isRealName;
}
