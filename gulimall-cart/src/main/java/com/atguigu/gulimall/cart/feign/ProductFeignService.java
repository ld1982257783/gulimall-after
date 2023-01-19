package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    //远程调用商品服务

    @RequestMapping("/product/skuinfo/info/{skuId}")
    //@RequiresPermissions("product:skuinfo:info")
    R info(@PathVariable("skuId") Long skuId);



    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable Long skuId);


    //查询当前sku商品的价格
    @GetMapping("/product/skuinfo/{skuId}/price")
    R getPrice(@PathVariable("skuId") Long skuId);
}
