package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResult {

    //锁定商品id
    private Long skuId;

    //锁定数量
    private Integer num;

    //是否成功
    private Boolean locked;


}
