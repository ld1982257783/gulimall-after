package com.atguigu.gulimall.member.web;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                  Model model){
        //调用远程服务 查询当前用户的所有订单
        Map<String,Object> page = new HashMap<>();
        page.put("page",pageNum.toString());
        R r = orderFeignService.listWithItem(page);
        //查找当前用户的所有订单列表数据
        System.out.println("返回结果为"+r);
        model.addAttribute("orders",r);
        return "orderList";
    }

}
