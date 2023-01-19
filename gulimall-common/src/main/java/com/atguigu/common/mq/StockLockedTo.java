package com.atguigu.common.mq;


import lombok.Data;

@Data
public class StockLockedTo {

    private Long id;  //锁定工作单的id
    private StockDetailTo detail;  //工作单详情的id


}
