package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.VO.SkuItemVo;
import com.atguigu.gulimall.product.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class itemController {

    @Autowired
    SkuInfoService skuInfoService;


    //查询当前商品的详细信息
    @GetMapping("/{skuId}.html")
    public String itemHtml(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {
        System.out.println("准备查询"+skuId);
        SkuItemVo vo = skuInfoService.item(skuId);
        System.out.println("============"+vo);
        model.addAttribute("item",vo);
        return "item";
    }
}
