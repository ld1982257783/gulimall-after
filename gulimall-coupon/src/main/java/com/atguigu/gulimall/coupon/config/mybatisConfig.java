package com.atguigu.gulimall.coupon.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 *  配置类  默认都是单实例
 *  proxyBeanMethods  代理bean的方法
 *  full(proxyBeanMethods=true)
 *  lite(proxyBeanMethods=false)
 *
 *  @Import({DBHelper.class})
 *  也是给容器中导入组件的一种方法
 *
 */

//如果不涉及组件依赖  可以设置代理对象为false 加载会加快

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement  //开启事务
@MapperScan("com.atguigu.gulimall.coupon.dao")
public class mybatisConfig {

    //开启mybatis的分页功能
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }
}
