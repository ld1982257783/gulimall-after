package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {


    @Autowired
    CartService cartService;
    /**
     * 浏览器有一个cookie ： user-Key 表示用户身份  一个月后过期
     * 如果第一次使用jd的购物车功能 都会给一个临时的用户身份
     * 浏览器以后会保存  每次访问都会带上这个cookie
     *
     * 登录  session有
     * 没有登录  按照cookie里面带来的user-key来做
     * 第一次 如果没有临时的用户 帮忙创建一个临时用户
     * @param
     * @return
     */
//    @GetMapping("/cart.html")
//    public String cartHttpSession(){
////        ThreadLocal 同一个线程共享数据    核心原理  map
//        //拦截器 -》 controller -》 service -》 dao    同一线程
//        //拿到拦截器方法拦截  并放入的数据
//        UserInfoTo userInfoTo = CartInterptor.threadLocal.get();
//
//
//
//        return "cartList";
//
//    }


    /**
     * /订单服务查询所有购物项选项
     * @return
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItem(){

        return cartService.getUserCartItems();
    }








    //加入购物车
    @GetMapping("/addToCart")
    public String addTo(@RequestParam("skuId") Long skuId , @RequestParam("num") Integer num, RedirectAttributes redirectAttributes){

        //创建一个方法 获取该商品的相关信息
        CartItem cartItem = null;
        try {
            cartItem = cartService.addToCart(skuId,num);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        redirectAttributes.addAttribute("skuId",skuId);
        //返回购物车列表页面
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }




    //避免重复刷新造成购物车数量累加
    @GetMapping("/addToCartSuccess.html")
    public String  addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model){
        //重定向到成功页面  再次查询购物车的数据即可
        CartItem item = cartService.getCartItem(skuId);
        model.addAttribute("item",item);
        return "success";
    }


    /**
     * 获取购物车的所有购物项
     * @param model
     * @return
     */
    @RequestMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //快速获取用户信息  id user-key
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";

    }





    @RequestMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("check") Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/cart.html";

    }



    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);

        return "redirect:http://cart.gulimall.com/cart.html";

    }


    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }




}
