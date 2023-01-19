package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {

    private Long itemId;   //序号
    private Integer status;     //状态
    private String reason;      //原因
}
