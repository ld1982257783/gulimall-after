package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.Vo.OrderConfirmVo;
import com.atguigu.gulimall.order.Vo.OrderSubmitVo;
import com.atguigu.gulimall.order.Vo.SubmitOrderResponseVo;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;


    //订单确认页面
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }


    //订单支付页面
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){

        SubmitOrderResponseVo responseVo = null;
        try {
            responseVo = orderService.submitOrder(vo);

            //下单去创建订单  校验令牌  校验价格  锁库存
            //下单成功来到支付选择页
            //下单失败回到订单确认页重新确认订单信息
            System.out.println("订单提交的数据"+vo);
            //成功  来到支付选择页面
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }catch (Exception e){
            String msg = "订单发送错误";
            redirectAttributes.addFlashAttribute("msg",msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }


//        if(responseVo.getCode() == 0){
//            //成功  来到支付选择页面
//            model.addAttribute("submitOrderResp",responseVo);
//            return "pay";
//        }else{
//            //失败
//            String msg = "下单失败：";
//            switch (responseVo.getCode()){
//                case 1: msg+="订单信息过期，请刷新再次提交"; break;
//                case 2: msg+="订单商品价格发生变化，请确认后再次提交";break;
//                case 3: msg+="库存锁定失败,商品库存不足";break;
//            }
//
//            redirectAttributes.addFlashAttribute("msg",msg);
//            return "redirect:http://order.gulimall.com/toTrade";
//
//        }

    }

}
