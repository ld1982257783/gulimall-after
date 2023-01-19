package com.atguigu.gulimall.product.VO;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SkuItemVo {
    //1  sku基本信息的获取  pms_sku_info
    private  SkuInfoEntity skuInfoEntity;


    //2  获取sku图片信息  pms_sku_images
    private List<SkuImagesEntity> imagesEntities;

    //3  获取spu的所有销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;



    //4  获取spu的介绍
    private SpuInfoDescEntity spuInfoDescEntity;

    //5  获取Spu规格参数组及组下的规格参数
    private List<SpuItemAttrGroupVo> groupAttrVos;

    //当前商品的秒杀优惠信息
    private SeckillInfoVo seckillInfoVo;

    //是否有货
    private Boolean hasStock;






}
