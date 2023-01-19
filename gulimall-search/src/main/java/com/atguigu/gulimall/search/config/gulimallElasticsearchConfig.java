package com.atguigu.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 导入依赖  编写配置  给容器中注入一个resthignclient
 */
@Configuration
public class gulimallElasticsearchConfig {


    public static final RequestOptions COMMON_OPTIONS;

    static{
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();

        COMMON_OPTIONS = builder.build();
    }





    //ES的相关部署操作  ip  端口 协议
    @Bean
    public RestHighLevelClient esRestClient(){
        RestClientBuilder builder = null;
        builder = RestClient.builder(new HttpHost("192.168.43.2",9200,"http"));

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

}
