package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.TO.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.gulimallElasticsearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MailSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import com.atguigu.gulimall.search.vo.brandEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MailSearchServiceImpl implements MailSearchService {

    @Autowired
    private RestHighLevelClient client;


    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //动态构建出dsl语句


        SearchResult result = null;

        //准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);


        try {
            //2执行检索请求
            SearchResponse response = client.search(searchRequest, gulimallElasticsearchConfig.COMMON_OPTIONS);

            // 分析响应数据封装成我们需要的格式

            result = buildSearchResult(response,searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }



    //1 准备检索请求
    //  #模糊匹配  过滤(按照属性，分类，品牌，价格区间，库存)，排序分页 高亮聚合分析
    private SearchRequest buildSearchRequest(SearchParam param) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();   //构建dsl语句

        /**
         * 模糊匹配   过滤  （按照属性  分类  品牌  价格区间  库存）
         */
        //构建bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1  must的模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
        //1.2  bool  filter   按照三级分类id查询
        if(param.getCatalog3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }
        //1.3  bool  filter   按照品牌id查询
        if(param.getBrandId()!=null && param.getBrandId().size() > 0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
        //1.6  按照所有指定的属性进行查询
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            //attrs  1_5存:8存
            for (String attr : param.getAttrs()) {
                String[] s = attr.split("_");
                String attrId = s[0];  //检索的属性 ID
                String[] attrValue = s[1].split(":");  //这个属性检索用的值
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedboolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValue));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }

        }

        //1.4  bool  filter   按照库存是否进行查询
        if(param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock() == 1));
        }
        //1.5  按照价格区间进行传销  1_500 _500 500_
        if(!StringUtils.isEmpty(param.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if(s.length == 2){
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length == 1){
                if(param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if(param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        //把以前所有的条件拿来进行封装
        sourceBuilder.query(boolQuery);


        /**
         * 排序  分页  高亮
         */

        //2.1  排序
        if(!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc")? SortOrder.ASC:SortOrder.DESC;
            //那个字段  升序还是降序
            sourceBuilder.sort(s[0],order);
        }

        //2.2  分页
        if(param.getPageNum() != null){
            sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
            sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        }else{
            sourceBuilder.from(0);
            sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        }


        //2.3  高亮
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            //高亮那个属性
            builder.field("skuTitle");
            builder.preTags("<b style='color: red'>");
            builder.postTags("</b>");

            sourceBuilder.highlighter(builder);
        }


        /**
         * 聚合分析
         */
        //3.1  品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        //TODO 3.2  品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);
        //TODO 3.3分类聚合  catalogId_agg
        TermsAggregationBuilder catalogId_agg = AggregationBuilders.terms("catalogId_agg").field("catalogId").size(20);
        catalogId_agg.subAggregation(AggregationBuilders.terms("catalogId_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogId_agg);

        //TODO 3.4  属性聚合 attr_aggs    嵌入聚合
        NestedAggregationBuilder attr_aggs = AggregationBuilders.nested("attr_aggs", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(5);
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(5));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_aggs.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_aggs);

        String sss = sourceBuilder.toString();
        System.out.println("构建dsl语句"+sss);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX},sourceBuilder);



        return searchRequest;

    }


    /**
     *
     * 构建结果数据
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        System.out.println("封装对象信息"+param.getPageNum());
        SearchResult result = new SearchResult();
        //获取所有的命中记录
        SearchHits hits = response.getHits();

        //1  返回的所有查询到的商品
        //result.setProducts();
        List<SkuEsModel> esModels= new ArrayList<>();
        if(hits.getHits()!=null && hits.getHits().length>0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    //获取高亮字段
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    //获取高亮内容
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }

                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        //2  当前所有商品涉及到的分类信息
//          result.setCatalogs();
        ParsedLongTerms catalogId_agg = response.getAggregations().get("catalogId_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalogId_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            //创建catalog_id 对象
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类Id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            ParsedStringTerms catalogId_name_agg = bucket.getAggregations().get("catalogId_name_agg");
            List<? extends Terms.Bucket> buckets1 = catalogId_name_agg.getBuckets();
            String catalog_name = buckets1.get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }

        result.setCatalogs(catalogVos);

        //3  当前所有商品涉及到的品牌信息
//          result.setBrands();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        List<? extends Terms.Bucket> brand_aggBuckets = brand_agg.getBuckets();
        for (Terms.Bucket brand_aggBucket : brand_aggBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //获取品牌id
            String keyAsString = brand_aggBucket.getKeyAsString();
            brandVo.setBrandId(Long.parseLong(keyAsString));
            //封装品牌的图片
            ParsedStringTerms brand_img_agg = brand_aggBucket.getAggregations().get("brand_img_agg");
            List<? extends Terms.Bucket> buckets1 = brand_img_agg.getBuckets();
            String keyAsString1 = buckets.get(0).getKeyAsString();
            brandVo.setBrandImg(keyAsString1);
            //封装品牌的名字
            ParsedStringTerms brand_name_agg = brand_aggBucket.getAggregations().get("brand_name_agg");
            List<? extends Terms.Bucket> buckets2 = brand_name_agg.getBuckets();
            String keyAsString2 = buckets2.get(0).getKeyAsString();
            brandVo.setBrandName(keyAsString2);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);


        // 属性信息  --属性 attr
//        result.setAttrs();
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_aggs = response.getAggregations().get("attr_aggs");
        ParsedLongTerms attr_id_agg = attr_aggs.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> buckets1 = attr_id_agg.getBuckets();
        for (Terms.Bucket bucket : buckets1) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //封装attr id
            String keyAsString = bucket.getKeyAsString();
            attrVo.setAttrId(Long.parseLong(keyAsString));
            //封装attr name
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String keyAsString1 = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(keyAsString1);
            //封装attr value
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<? extends Terms.Bucket> buckets2 = attr_value_agg.getBuckets();
            List<String> searchResult_attrValue = new ArrayList<>();
            for (Terms.Bucket bucket1 : buckets2) {
                String keyAsString2 = bucket1.getKeyAsString();
                searchResult_attrValue.add(keyAsString2);
            }
            attrVo.setAttrValue(searchResult_attrValue);
            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);
        System.out.println("相关属性信息为"+attrVos);

        //4  分页信息 -- 页码
//            result.setPageNum();
        //5  分页信息  总记录数
//              result.setTotal();

        //
        //当前页码
        if(param.getPageNum() != null){
            result.setPageNum(param.getPageNum());
        }else{
            result.setPageNum(1);
        }

        long totalHits = hits.getTotalHits().value;
        result.setTotal(totalHits);
        //6  分页信息  总页码
//          result.setTotalPages();
        int totalPages = (int)totalHits%EsConstant.PRODUCT_PAGESIZE==0?(int)totalHits/EsConstant.PRODUCT_PAGESIZE:(int)totalHits/EsConstant.PRODUCT_PAGESIZE+1;
        result.setTotalPages(totalPages);


        //构建数字页码集合  方便前端遍历   上一页  1 2 3 ...  下一页
        List<Integer> pageNav = new ArrayList<>();
        for(int i=1;i<=totalPages;i++){
            pageNav.add(i);
        }
        result.setPageNav(pageNav);



        //构建面包屑导航功能
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //attrs = 1_5存:5存
                String[] s = attr.split("_");
                //调用远程服务获取属性名称
                navVo.setNvValue(s[1]);
                R info = productFeignService.info(Long.parseLong(s[0]));
                if(info.getCode() == 0){
                    AttrResponseVo data = info.getData("attr", new TypeReference<AttrResponseVo>() {});
                    navVo.setNavName(data.getAttrName());

                    result.getAttrIds().add(data.getAttrId());
                }else{
                    navVo.setNavName(s[0]);
                }



                //
                String replaces = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replaces);


                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);

            //给品牌设置面包屑导航
            if(param.getBrandId()!=null && param.getBrandId().size() > 0){
                List<SearchResult.NavVo> navs = result.getNavs();

                SearchResult.NavVo navVoz = new SearchResult.NavVo();
                navVoz.setNavName("品牌");
                //调用远程服务获取品牌基本信息  传入一个集合
                R r = productFeignService.brandInfo(param.getBrandId());
                if(r.getCode() == 0 ){  //远程查询成功
                    List<brandEntity> brand = r.getData("brand", new TypeReference<List<brandEntity>>() {});
                    StringBuffer stringBuffer = new StringBuffer();
                    String replaces = null;
                    for (brandEntity brandEntity : brand) {
                        stringBuffer.append(brandEntity.getName()+";");
                        replaces = replaceQueryString(param, brandEntity.getBrandId()+"","brandId");

                    }

                    navVoz.setNvValue(stringBuffer.toString());
                    navVoz.setLink("http://search.gulimall.com/list.html?"+replaces);

                }
                navs.add(navVoz);
                System.out.println("面包屑信息为"+navs);

            }



            result.setNavs(navVos);
        }





        //  TODO  分类  不需要导航取消



        return result;
    }

    private String replaceQueryString(SearchParam param, String attr,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(attr, "UTF-8");
            System.out.println(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        String replace = encode.replace("+", "%20");
        String replace1 = replace.replace("%28", "(");
        String replace2 = replace1.replace("%29", ")");
        //遍历所有属性  替换attr上的值    得到新的查询条件
        return param.get_queryString().replace("&"+key+"=" + replace2, "");
    }
}
