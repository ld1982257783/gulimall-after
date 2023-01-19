package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.gulimallElasticsearchConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


//batch  批量插入操作数据
@SpringBootTest
public class ElasticSearchBulkTest {

    @Autowired
    RestHighLevelClient client;


    //批量操作 其实就是往BulkRequest 添加修改 删除数据
    @Test
    public void bulkRequest() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        IndexRequest indexRequest = new IndexRequest();
        User user = new User();
        user.setAge(22);
        user.setGender("女");
        user.setUserName("大杀四方");
        bulkRequest.add(indexRequest.index("users").id("1010").source(JSON.toJSONString(user), XContentType.JSON));
        bulkRequest.add(new IndexRequest().index("users").id("1011").source(JSON.toJSONString(new User("鬼魅森林","男",26)),XContentType.JSON));
        bulkRequest.add(new IndexRequest().index("users").id("1012").source(JSON.toJSONString(new User("不闻不问","女",27)),XContentType.JSON));
        BulkResponse bulk = client.bulk(bulkRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("花费时间"+bulk.getTook());
        System.out.println("响应结果为"+bulk);

    }




    @Test
    public void searchIndices() throws IOException {
        //查询索引中的所有数据
        SearchRequest searchRequest = new SearchRequest();
        //构建查询条件
        searchRequest.indices("users");
        //查询所有
        searchRequest.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));

        //客户端操作
        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        //获取所有命中记录
        SearchHits hits = search.getHits();
        System.out.println("总记录数"+hits.getTotalHits());

        //输出查询结果
        System.out.println("查询结果为"+search);


    }






    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }








    //分页查询   条件查询  字段查询

    /**
     * {
     *     "query": {
     *         "bool": {
     *             "must": [
     *                 {
     *                     "term": {
     *                          "gender": "男"
     *                     }
     *                 }
     *             ]
     *         }
     *     },
     * }
     * @throws IOException
     */
    //条件查询
    @Test
    public void termQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        searchRequest.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("gender","男")));

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println("查询总记录数为"+search.getHits().getTotalHits());
        System.out.println("详细信息为"+search.getHits().getHits());

    }


    /**
     * {
     *     "query": {
     *         "bool": {
     *             "must": [
     *                 {
     *                     "match_all": {
     *                     }
     *                 }
     *             ]
     *         }
     *     },
     *     "from": 0,
     *     "size": 2
     * }
     * @throws IOException
     */
    //条件查询  需要知道其实位置  以及每页显示 条数
    @Test
    public void queryPageSearch() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        //查询下一页小算法  （当前页码-1）*每页显示的数据条数
        builder.from(2);
        //每页显示两条数据
        builder.size(2);
        searchRequest.source(builder);
        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("查询结果为"+search);


    }


    //查询排序
    @Test
    public void sortQuerySearch() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder query = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        query.sort("age", SortOrder.DESC);
        searchRequest.source(query);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("所有命中记录为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("遍历为"+hit.getSourceAsString());
        }

    }




    // 过滤字段  排除字段  包含字段
    @Test
    public void filterFieldsQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        String [] includes = {};
        String [] excludes = {"age"};
        builder.fetchSource(includes,excludes);
        searchRequest.source(builder);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("所有信息为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("逐个遍历"+hit.getSourceAsString());
        }

    }







    //组合查询   boolquery

    /**
     * {
     *     "query": {
     *         "bool": {
     *             "must": [
     *                 {
     *                     "match": {
     *                         "gender": "女"
     *                     }
     *                 },
     *                 {
     *                     "match": {
     *                         "age": 22
     *                     }
     *                 }
     *             ]
     *         }
     *     },
     *     "sort": {
     *         "age": "desc"
     *     }
     * }
     * @throws IOException
     */
    @Test
    public void boolQueryIndex() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("age",22));
        boolQuery.must(QueryBuilders.matchQuery("gender","女"));
        builder.query(boolQuery);
        searchRequest.source(builder);
        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println("查询结果为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("命中详细信息"+hit.getSourceAsString());
        }
    }





    //范围查询

    /**
     * {
     *     "query": {
     *         "bool": {
     *             "must": [
     *                 {
     *                     "match_all": {}
     *                 }
     *             ],
     *             "filter":{
     *                 "range": {
     *                     "age": {
     *                         "gte": 20,
     *                         "lte": 40
     *                     }
     *                 }
     *             }
     *         }
     *     },
     *     "sort": {
     *         "age": "desc"
     *     }
     * }
     * @throws IOException
     */
    @Test
    public void rangeQueryIndex() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("age");
        //查询年龄大于等于30  小于等于40
        rangeQuery.gte(30);   //e 代表=
        rangeQuery.lte(40);
        builder.query(rangeQuery);
        searchRequest.source(builder);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("查询结果为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("查询到的记录"+hit.getSourceAsString());
        }

    }







    //模糊查询
    @Test
    public void likeQueryIndex() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        //fuzziness(Fuzziness.ONE)代表字符相差一个可以匹配成功
        FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("userName", "wang").fuzziness(Fuzziness.ONE);
        builder.query(fuzzyQueryBuilder);

        searchRequest.source(builder);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println("查询结果为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("详细查询信息"+hit.getSourceAsString());
        }


    }




    //高亮查询
    @Test
    public void highlightQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        MatchPhraseQueryBuilder matchPhraseQueryBuilder = QueryBuilders.matchPhraseQuery("userName", "不闻不问wang");
        //给对应属性添加高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font color='red'>");
        highlightBuilder.postTags("</font>");
        //给那个字段高亮
        highlightBuilder.field("userName");

        builder.highlighter(highlightBuilder);
        builder.query(matchPhraseQueryBuilder);

        searchRequest.source(builder);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("查询结果为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("详细查询信息"+hit.getSourceAsString());
        }
    }


    //高级查询聚合查询
    @Test
    public void aggsQueryIndex() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        MaxAggregationBuilder aggregationBuilder = AggregationBuilders.max("maxAge").field("age");
        builder.aggregation(aggregationBuilder);

        searchRequest.source(builder);

        SearchResponse search = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);
        System.out.println("查询结果为"+search);
        for (SearchHit hit : search.getHits()) {
            System.out.println("查询的详细信息为"+hit.getSourceAsString());

        }




    }





}
