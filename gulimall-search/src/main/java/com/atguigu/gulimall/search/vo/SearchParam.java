package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;


/**
 * 封装页面所有可能传递过来的条件
 */
@Data
public class SearchParam {

    private String keyword;  //页面传来的全文匹配关键子  v
    private String catalog3Id;  //三级分类id        v

    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;  //排序               v

    /**
     * 好多过滤条件
     * hasStock（是否有货） skuPrice区间 brandId  catalog3Id attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     *
     * attrs=2_5寸
     */

    private Integer hasStock;  //显示是否有货            v
    private String skuPrice;    //价格区间查询           v
    private List<Long> brandId;       //按照品牌进行查询 可以多选           v   1,2
    private List<String> attrs;   //按照属性进行删选             v

    private Integer pageNum;    //页码

    private String _queryString;  //原生的所有查询条件



}
