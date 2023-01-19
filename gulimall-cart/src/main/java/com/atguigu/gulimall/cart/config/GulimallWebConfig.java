package com.atguigu.gulimall.cart.config;

import com.atguigu.gulimall.cart.interceptor.CartInterptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    //注册拦截器

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //给注册列表里面添加
        registry.addInterceptor(new CartInterptor()).addPathPatterns("/**");

    }
}
