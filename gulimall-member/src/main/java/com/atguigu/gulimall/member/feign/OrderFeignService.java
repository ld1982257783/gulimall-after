package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    //调用订单服务   RequestBody可以以json方式传递
    @PostMapping("/order/order/listWithItem")
    //@RequiresPermissions("order:order:list")
    R listWithItem(@RequestBody Map<String, Object> params);

}
