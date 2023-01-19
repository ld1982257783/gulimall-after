package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.gulimallElasticsearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;


        /**
         * Copyright 2021 bejson.com
         */

        /**
         * Auto-generated: 2021-09-24 23:34:58
         *
         * @author bejson.com (i@bejson.com)
         * @website http://www.bejson.com/java2pojo/
         */
        @Data
        @ToString
        public static class Account {

            private int account_number;
            private int balance;
            private String firstname;
            private String lastname;
            private int age;
            private String gender;
            private String address;
            private String employer;
            private String email;
            private String city;
            private String state;

        }




    @Test
    public void searchData() throws IOException {
        //1 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定DSL 检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //1)构建检索条件
//        sourceBuilder.query();
//        sourceBuilder.from();
//        sourceBuilder.size();
//        sourceBuilder.aggregation();
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        // 按照年龄的值分布进行聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);

        //计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);




        System.out.println("检索条件"+sourceBuilder.toString());

        searchRequest.source(sourceBuilder);

        //执行检索
        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        //分析结果 searchResponse
        System.out.println(search);
        //获取所有查到的数据
        SearchHits hits = search.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }

        //获取这次检索到的分析信息
        Aggregations aggregations = search.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("当前聚合的名字为"+aggregation.getName());
//        }
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄"+keyAsString+","+bucket.getDocCount());
        }

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均工资"+balanceAvg1.getValue());

    }











    @Test
    public void indexDate() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");//创建索引
        //设置索引id
        indexRequest.id("1");
        User user = new User();
        user.setAge(18);
        user.setGender("男");
        user.setUserName("张三");
        //将user转为json格式
        String s = JSON.toJSONString(user);
        //另外一种转换json的方法
//        ObjectMapper objectMapper = new ObjectMapper();
//        String s1 = objectMapper.writeValueAsString(user);
        indexRequest.source(s, XContentType.JSON);  //要保存的内容
        //执行操作   客户端保存
        IndexResponse index = client.index(indexRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        //提取有用的相应数据
        System.out.println(index);
    }



    @Test
    public void deleteIndex() throws IOException {
            //删除索引
        DeleteIndexRequest indexRequest = new DeleteIndexRequest("users");

        AcknowledgedResponse delete = client.indices().delete(indexRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("运行结果"+delete);


    }

    //修改索引数据
    @Test
    public void update() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("users").id("1");
        UpdateRequest doc = updateRequest.doc(XContentType.JSON, "gender", "女");
        UpdateResponse update = client.update(updateRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("输出更新结果"+update);

    }


    //查询数据
    @Test
    public void getIndex() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.index("users").id("1");
        GetResponse documentFields = client.get(getRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("查询结果为"+documentFields);
    }


    //删除数据
    @Test
    public void deleteData() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index("users").id("1");
        DeleteResponse delete = client.delete(deleteRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("删除成功"+delete);
    }





    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }





    @Test
    void contextLoads() {
        System.out.println(client);
    }





}
