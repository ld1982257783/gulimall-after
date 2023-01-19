package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.VO.MemberEntity;
import com.atguigu.common.constant.AuthServerConstand;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */

@Slf4j
@Controller
public class Oauth2Controller {

    @Autowired
    MemberFeignService memberFeignService;


    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        //1 换取accesstoken
        Map<String,String> map = new HashMap<>();
        map.put("client_id","1983025817");
        map.put("client_secret","04d1f5b59f7100c65c820d2e49acdeb7");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code",code);
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<String, String>(), new HashMap<String, String>(), map);
        if(response.getStatusLine().getStatusCode() == 200){
            //成功获取到了access_token
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //知道当前是哪个社交用户登录
            // 1）当前用户如果是第一次进网站，自动注册进来(为当前社交用户生成一个会员信息账号，以后这个账号就对应指定的会员)
            //TODO  登录或者注册这个社交用户
            R r = memberFeignService.SocialLogin(socialUser);
            if(r.getCode()==0){
                //登录成功
                MemberEntity data = r.getData(new TypeReference<MemberEntity>() {});
                session.setAttribute(AuthServerConstand.LOGIN_USER,data);
//                log.info("用户信息为"+data);
                return "redirect:http://gulimall.com";
            }

        }else{
            return "redirect:http://auth.gulimall.com/login.html";
        }
        //登录成功就跳转到http://gulimall.com
        return "redirect:http://gulimall.com";
    }

}
