package com.atguigu.gulimall.cart.vo;


import java.math.BigDecimal;
import java.util.List;

/**
 * 封装购物项的bean
 */
public class CartItem {

    private Long skuId;
    private Boolean check = true;
    private String tittle;
    private String image;
    private List<String> skuAttr;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;


    public CartItem(){


    }
    @Override
    public String toString() {
        return "CartItem{" +
                "skuId=" + skuId +
                ", check=" + check +
                ", tittle='" + tittle + '\'' +
                ", image='" + image + '\'' +
                ", skuAttr=" + skuAttr +
                ", price=" + price +
                ", count=" + count +
                ", totalPrice=" + totalPrice +
                '}';
    }

    public CartItem(Long skuId, Boolean check, String tittle, String image, List<String> skuAttr, BigDecimal price, Integer count, BigDecimal totalPrice) {
        this.skuId = skuId;
        this.check = check;
        this.tittle = tittle;
        this.image = image;
        this.skuAttr = skuAttr;
        this.price = price;
        this.count = count;
        this.totalPrice = totalPrice;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Boolean getCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getTittle() {
        return tittle;
    }

    public void setTittle(String tittle) {
        this.tittle = tittle;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSkuAttr() {
        return skuAttr;
    }

    public void setSkuAttr(List<String> skuAttr) {
        this.skuAttr = skuAttr;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getTotalPrice() {
        //计算 单价*总价
        BigDecimal multiply = this.price.multiply(new BigDecimal("" + this.count));
        return multiply;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
