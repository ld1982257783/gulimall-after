package com.atguigu.common.exception;


public class NoStockException extends RuntimeException{
    private Long skuId;

    public NoStockException(Long skuId){
        super("商品id"+skuId+"没有足够的库存了");
    }

    public NoStockException(String msg){
        super(msg);
    }



    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }
}
