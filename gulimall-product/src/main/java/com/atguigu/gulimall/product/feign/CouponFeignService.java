package com.atguigu.gulimall.product.feign;

import com.atguigu.common.TO.SkuBoundTo;
import com.atguigu.common.TO.SkuReductionTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    //CouponFeignService.saveBounds(SpuBoundTo)
        //1 @RequestBody将这个对象转为json
        //2 找到gulimall-coupon 服务  给/coupon/spubounds/save发送请求
        //3 对方服务收到请求  请求体里面有json数据
        //4 RequestBody SpuBoundsEntity spuBounds    将请求体的json转换为spuboundsentity
    //只要json数据模型是兼容的  双方服务无须使用同一个TO


    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SkuBoundTo skuBoundTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody  SkuReductionTo skuReductionTo);


}
