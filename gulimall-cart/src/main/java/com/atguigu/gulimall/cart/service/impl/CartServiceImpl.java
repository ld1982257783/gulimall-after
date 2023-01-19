package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private final String CART_PREFIX = "gulimall:cart:";

    //注入远程服务
    @Autowired
    ProductFeignService productFeignService;

    //自动注入线程池
    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    CartInterptor cartInterptor;

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //获取当前userinfoto对象
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //判断当前购物车有没有此商品
        System.out.println(skuId.toString());
        String res = (String) cartOps.get(skuId.toString());
        if(Strings.isEmpty(res)){
            CartItem cartItem = new CartItem();
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //远程查询当前商品ID的信息
                R info = productFeignService.info(skuId);
                SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                //商品添加到购物车
                cartItem.setCheck(true);
                cartItem.setCount(num);   //指定几件
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setTittle(skuInfo.getSkuTitle());
                cartItem.setSkuId(skuInfo.getSkuId());
                cartItem.setPrice(skuInfo.getPrice());
            }, executor);

            //远程查询sku的组合信息  销售属性组合
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);


            //当所有方法执行完毕
            CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();

            //给redis里面保存当前sku的组合信息
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);


            return cartItem;
        }
        else{
            //有这个商品  更新数量就可以
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);

            //重新更新到redis
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);
            return cartItem;
        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {
        //刷新url只是在redis里面重新获取数据  并不会真的修改数据库数据
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(o, CartItem.class);

        return cartItem;
    }



    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = cartInterptor.threadLocal.get();

        String cartKey = "";  //购物车的key
        if(userInfoTo.getUserId()!=null){
            //登陆了
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else{
            cartKey = CART_PREFIX + "user-key";
        }

        //获取redis里面的数据
        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(cartKey);
        return operations;

    }


    /**
     * 获取购物车的所有选项
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        //首先判断是临时购物车还是登陆购物车
        //获取用户信息
        UserInfoTo userInfoTo = CartInterptor.threadLocal.get();
        Cart cart = new Cart();
        String key = CART_PREFIX+"user-key";
        if(userInfoTo.getUserId()!=null){
            //登陆用户
            //2  如果临时购物车的数据还没有进行合并
            List<CartItem> tempCartItems = getCartItems(key);
            if(tempCartItems!=null){
                //临时购物车有数据  需要进行合并  将临时购物车的所有数据
                for (CartItem tempCartItem : tempCartItems) {
                    Integer count = tempCartItem.getCount();
                    Long skuId = tempCartItem.getSkuId();
                    addToCart(skuId, count);
                }
            }

            //合并完毕  再来获取登录购物车的数据  [包含临时购物车的数据 和登录后的购物车的数据]
            String keySum = CART_PREFIX+userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(keySum);
            //合并完毕  删除 临时购物车数据
            clearCart(key);
            cart.setItems(cartItems);


        }else{
            //临时用户  获取临时购物车的所有项目
            String keyTemp = CART_PREFIX+"user-key";
            List<CartItem> cartItems = getCartItems(keyTemp);
            cart.setItems(cartItems);
        }


        return cart;
    }



    //获取当前购物车里面的所有购物项
    private List<CartItem> getCartItems(String key) {
        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(key);
        List<Object> values = operations.values();
        if(values!=null&&values.size()>0){
            List<CartItem> collect = values.stream().map((res) -> {
                String str = (String) res;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }else{
            return null;
        }
    }


    /**
     * 清空临时购物车
     * @param key
     */
    @Override
    public void clearCart(String key) {
        stringRedisTemplate.delete(key);

    }


    //勾选购物项目
    @Override
    public void checkItem(Long skuId, Integer check) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);


    }


    /**
     * 改变商品数量
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        //获取当前操作的是哪个购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);

    }

    @Override
    public void deleteItem(Long skuId) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterptor.threadLocal.get();
        //获取当前用户的购物车数据
        if(userInfoTo ==null){
            return null;
        }else{
            //获取登录后的购物车数据
            String cartKey = CART_PREFIX+userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            List<CartItem> collect = cartItems.stream().filter(item ->
                    item.getCheck())
                    .map(item->{
                        //TODO 远程查询最新价格 然后更新  查询商品服务
                        R price = productFeignService.getPrice(item.getSkuId());
                        String price_max = (String) price.get("price");
                        item.setPrice(new BigDecimal(price_max));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }


    }


}
