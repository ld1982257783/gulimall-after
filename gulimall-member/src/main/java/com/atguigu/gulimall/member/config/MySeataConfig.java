package com.atguigu.gulimall.member.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MySeataConfig {

//    @Autowired
//    DataSourceProperties dataSourceProperties;
//
//    //让Seata包装自己的数据源
//    @Bean
//    public DataSource dataSource(DataSourceProperties dataSourceProperties){
//        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
//        if(StringUtils.hasText(dataSourceProperties.getName())){
//            dataSource.setPoolName(dataSourceProperties.getName());
//        }
//
//        return new DataSourceProxy(dataSource);
//    }
}
