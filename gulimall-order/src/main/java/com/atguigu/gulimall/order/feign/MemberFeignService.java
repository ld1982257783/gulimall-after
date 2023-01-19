package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.Vo.MemberAdressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberFeignService {


    //远程查询所有的收货列表
    @GetMapping("/member/memberreceiveaddress/{memberId}/adresses")
    List<MemberAdressVo> getAddress(@PathVariable("memberId") Long memberId);

}
