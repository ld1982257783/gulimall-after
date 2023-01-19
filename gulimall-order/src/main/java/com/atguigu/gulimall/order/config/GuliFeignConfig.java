package com.atguigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 使用feign进行远程调用时  默认是不带cookie的
 * 此配置类的作用就是增加一个请求拦截器 用来获取上次请求跳转过来的cookie相关信息
 */
@Configuration
public class GuliFeignConfig {


    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){

        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //1 RequestContextHolder拿到刚进来的这个请求
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(requestAttributes!=null){
                    HttpServletRequest request = requestAttributes.getRequest();
                    if(request!=null){
                        //同步请求头数据  同步cookie
                        String cookie = request.getHeader("Cookie");
                        //给新请求同步了老请求的cookie
                        template.header("Cookie",cookie);
                        //System.out.println("feign远程调用之前先进行requestInterceptor.apply方法");
                    }
                }

            }
        };
    }

}
