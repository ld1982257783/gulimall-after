package com.atguigu.gulimall.seckill.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    //返回当前时间可以参与的秒杀商品信息
    @GetMapping("/currentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus(){
        log.info("currentSeckillSkus正在执行");
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }




    //与gulimall-product关联 获取当前商品的秒杀信息
    @GetMapping("/sku/seckill/{skuId}")
    @ResponseBody
    public R skuSeckillInfo(@PathVariable("skuId") Long skuId){
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }



    //秒杀商品流程  立即抢购
    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model){
         String orderSn = seckillService.kill(killId,key,num);
         model.addAttribute("orderSn",orderSn);
         //放置订单号
        return "success";

    }

}
