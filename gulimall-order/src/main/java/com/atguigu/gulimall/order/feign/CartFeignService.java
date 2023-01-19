package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.Vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    /**
     * /订单服务查询所有购物项选项
     * @return
     */
    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItem();
}
