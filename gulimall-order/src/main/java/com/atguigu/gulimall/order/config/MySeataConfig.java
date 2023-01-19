package com.atguigu.gulimall.order.config;


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





//    @Autowired
//    private DataSourceProperties dataSourceProperties;
//
//    @Primary
//    @Bean
//    public DruidDataSource druidDataSource(){
//        DruidDataSource druidDataSource = new DruidDataSource();
//        druidDataSource.setUrl(dataSourceProperties.getUrl());
//        druidDataSource.setUsername(dataSourceProperties.getUsername());
//        druidDataSource.setPassword(dataSourceProperties.getPassword());
//        druidDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
//        druidDataSource.setInitialSize(0);
//        druidDataSource.setMaxActive(180);
//        druidDataSource.setMaxWait(60000);
//        druidDataSource.setMinIdle(0);
//        druidDataSource.setValidationQuery("Select 1 from DUAL");
//        druidDataSource.setTestOnBorrow(false);
//        druidDataSource.setTestOnReturn(false);
//        druidDataSource.setTestWhileIdle(true);
//        druidDataSource.setTimeBetweenEvictionRunsMillis(60000);
//        druidDataSource.setMinEvictableIdleTimeMillis(25200000);
//        druidDataSource.setRemoveAbandoned(true);
//        druidDataSource.setRemoveAbandonedTimeout(1800);
//        druidDataSource.setLogAbandoned(true);
//        return druidDataSource;
//    }
//
//
//    @Bean
//    public DataSourceProxy dataSourceProxy(DataSource hikariDataSource){
//        return new DataSourceProxy(hikariDataSource);
//
//    }
//
//
//    @Bean
//    public SqlSessionFactory sqlSessionFactory(DataSourceProxy dataSourceProxy) throws Exception {
//        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(dataSourceProxy);
//        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
//                    .getResources("classpath*:/mapper/*.xml")
//                    );
//        sqlSessionFactoryBean.setTransactionFactory(new SpringManagedTransactionFactory());
//
//        return sqlSessionFactoryBean.getObject();
//
//    }


//    @Bean
//    public GlobalTransactionScanner globalTransactionScanner(){
//        return new GlobalTransactionScanner("gulimall-order","gulimall-order");
//    }


}
