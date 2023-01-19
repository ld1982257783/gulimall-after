package com.atguigu.common.TO.es;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * {
 *   "mappings":{
 *     "properties":{
 *
 *
 *
 *
 *       }
 *     }
 *   }
 * }
 *
 *
 */


@Data
public class SkuEsModel {

    //"skuId":{
    // *         "type": "long"
    // *       },
    private Long skuId;


    /**
     * "spuId":{
     *  *         "type": "keyword"
     *  *       },
     *  *       "skuTitle":{
     *  *         "type": "text",
     *  *         "analyzer": "ik_smart"
     *  *       },
     *  *       "skuPrice":{
     *  *         "type": "keyword"
     *  *       },
     *  *       "skuImg":{
     *  *         "type": "keyword",
     *  *         "index": false,
     *  *         "doc_values": false
     *  *       },
     */

    private Long spuId;

    private String skuTitle;

    private BigDecimal skuPrice;

    private String skuImg;

    /**
     *
     *  "saleCount":{
     *  *         "type": "long"
     *  *       },
     *  *       "hasStock":{
     *  *         "type": "boolean"
     *  *       },
     *  *       "hotScore":{
     *  *         "type": "long"
     *  *       },
     *  *       "brandId":{
     *  *         "type": "long"
     *  *       },
     *  *       "catalogId":{
     *  *         "type": "long"
     *  *       },
     *  *       "brandName":{
     *  *         "type": "keyword",
     *  *         "index": false,
     *  *         "doc_values": false
     *  *       },
     *  *       "brandImg":{
     *  *         "type": "keyword",
     *  *         "index": false,
     *  *         "doc_values": false
     *  *       },
     *  *       "catalogName":{
     *  *         "type": "keyword",
     *  *         "index":false,
     *  *         "doc_values": false
     *  *       },
     */

    private Long saleCount;

    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private Long catalogId;

    private String brandName;

    private String brandImg;

    private String catalogName;


    /**
     *
     * "attrs":{
     *  *         "type": "nested",
     *  *         "properties":{
     *  *           "attrId":{
     *  *             "type": "long"
     *  *           },
     *  *           "attrName":{
     *  *             "type": "keyword",
     *  *             "index": false,
     *  *             "doc_values": false
     *  *           },
     *  *           "attrValue":{
     *  *             "type": "keyword"
     *  *           }
     *  *         }
     */

    private List<Attrs> attrs;

    @Data
    public static class Attrs{
        private Long attrId;

        private String attrName;

        private String attrValue;

    }


}
