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
        //1 ??????????????????
        SearchRequest searchRequest = new SearchRequest();
        //????????????
        searchRequest.indices("bank");
        //??????DSL ????????????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //1)??????????????????
//        sourceBuilder.query();
//        sourceBuilder.from();
//        sourceBuilder.size();
//        sourceBuilder.aggregation();
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        // ????????????????????????????????????
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);

        //??????????????????
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);




        System.out.println("????????????"+sourceBuilder.toString());

        searchRequest.source(sourceBuilder);

        //????????????
        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        //???????????? searchResponse
        System.out.println(search);
        //???????????????????????????
        SearchHits hits = search.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }

        //????????????????????????????????????
        Aggregations aggregations = search.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("????????????????????????"+aggregation.getName());
//        }
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("??????"+keyAsString+","+bucket.getDocCount());
        }

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("????????????"+balanceAvg1.getValue());

    }











    @Test
    public void indexDate() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");//????????????
        //????????????id
        indexRequest.id("1");
        User user = new User();
        user.setAge(18);
        user.setGender("???");
        user.setUserName("??????");
        //???user??????json??????
        String s = JSON.toJSONString(user);
        //??????????????????json?????????
//        ObjectMapper objectMapper = new ObjectMapper();
//        String s1 = objectMapper.writeValueAsString(user);
        indexRequest.source(s, XContentType.JSON);  //??????????????????
        //????????????   ???????????????
        IndexResponse index = client.index(indexRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        //???????????????????????????
        System.out.println(index);
    }



    @Test
    public void deleteIndex() throws IOException {
            //????????????
        DeleteIndexRequest indexRequest = new DeleteIndexRequest("users");

        AcknowledgedResponse delete = client.indices().delete(indexRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("????????????"+delete);


    }

    //??????????????????
    @Test
    public void update() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("users").id("1");
        UpdateRequest doc = updateRequest.doc(XContentType.JSON, "gender", "???");
        UpdateResponse update = client.update(updateRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("??????????????????"+update);

    }


    //????????????
    @Test
    public void getIndex() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.index("users").id("1");
        GetResponse documentFields = client.get(getRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("???????????????"+documentFields);
    }


    //????????????
    @Test
    public void deleteData() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index("users").id("1");
        DeleteResponse delete = client.delete(deleteRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("????????????"+delete);
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
