package com.atguigu.gulimall.search.vo;

import com.atguigu.common.TO.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    //查询到的所有商品信息
    private List<SkuEsModel> products;



    //以下是分页信息
    private Integer pageNum;   //当前页码
    private Long total;         //总记录数
    private Integer totalPages;  //总页码

    private List<BrandVo> brands;     //当前查询到的结果  所有涉及到的品牌
    private List<AttrVo> attrs;         // 当前查询到的结果 所涉及都的所有属性
    private List<CatalogVo> catalogs;   // 当前查询到的结果 所涉及都的所有分类属性

    //构建数字页码集合  方便前端遍历
    private List<Integer> pageNav;

    //以上是返回给页面的所有信息


    //面包屑导航数据
    private List<NavVo> navs;


    //属性计数  点击后  就取消  不显示
    private List<Long> attrIds = new ArrayList<>();



    @Data
    public static class NavVo{

        private String navName;

        private String nvValue;

        private String Link;


    }



    @Data
    public static class BrandVo{
        private Long  brandId;
        private String brandName;
        private String brandImg;

    }

    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String  catalogName;
    }


    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;


    }



}
