package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.VO.MemberEntity;
import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    //泛型  只能保存string
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;


    //注入rabbitmq
    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";  //+商品随机码

    /**
     * 上架商品
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        //去数据库扫描最近三天所有需要秒杀的商品  远程调用coupon
        R r = couponFeignService.getLates3DaysSession();
        if(r.getCode() == 0){
            //上架商品
            List<SeckillSessionWithSkus> data = r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {});
            System.out.println("返回的数据为"+data);
            //缓存到redis
            //1 缓存活动信息
            saveSessionInfos(data);
            //2 缓存活动的关联商品信息
            saveSessionSkuInfos(data);
        }
    }






    private void saveSessionInfos(List<SeckillSessionWithSkus> sessions){
        //给redis封装场次 + 场次秒杀商品id的详细信息
        if(sessions!=null && sessions.size()>0){
            sessions.stream().forEach(session ->{
                long start_time = session.getStartTime().getTime();
                long end_time = session.getEndTime().getTime();
                String key = SESSIONS_CACHE_PREFIX+start_time+"_"+end_time;

                Boolean hasKey = redisTemplate.hasKey(key);
                if(!hasKey){
                    List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId().toString()+"_"+item.getSkuId().toString()).collect(Collectors.toList());
                    //缓存活动信息
                    System.out.println(collect);
                    redisTemplate.opsForList().leftPushAll(key,collect);
                }

            });
        }

    }


    private void saveSessionSkuInfos(List<SeckillSessionWithSkus> sessions){
        //给redis里面封装每个id对应的商品信息
        if(sessions!=null && sessions.size()>0){
            sessions.stream().forEach(session -> {
                //准备hash操作
                BoundHashOperations<String,Object,Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                //获取每一个的商品id
                session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                    //判断此商品 库存是否已经上架过
                    Boolean hasKey = ops.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString());
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();

                    //TODO 4 设置商品的随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);
                    if(!hasKey){
                        //每遍历一次给我redis插入一条记录
                        //缓存商品
                        //TODO 远程调用商品服务 1 sku的基本数据  查询商品服务
                        R r = productFeignService.SkuInfo(seckillSkuVo.getSkuId());
                        if(r.getCode() == 0){
                            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
                            redisTo.setSkuInfo(skuInfo);
                        }

                        //TODO 2 sku的秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo,redisTo);

                        //TODO 3 设置上当前商品的秒杀时间信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());
                        String s = JSON.toJSONString(redisTo);
                        ops.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString(),s);
                        //设置商品信号量  限流
                        redisTo.setRandomCode(token);
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        //商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                    }
                });

            });
        }

    }


    /**
     *
     * blockHandler 函数会在原方法被限流/降级/系统保护的时候调用  而fallback函数会针对所有类型的异常  可以写在其他列  必须是静态方法
     * @return
     */
    //获取当前时间可以参与秒杀的商品
    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler") //value设定资源名称  blockHandler降级以后调用哪个方法来处理
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        //设置资源名称
        try(Entry entry = SphU.entry("SeckillSkus")){
            Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
            for (String key : keys) {
                String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
                if(time>=start && time<=end){
                    //2 获取这个秒杀场次需要的所有商品信息
                    //      根据key 和下标索引获取当前所有场次信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    //row key value
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    //根据所有的key得到所有的value
                    List<String> list = hashOps.multiGet(range);
                    if(list!=null && list.size()>0){
                        List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                            SeckillSkuRedisTo redisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
                            return redisTo;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }

            }
        }catch (BlockException e){
            log.error("资源被限流,{}",e.getMessage());
        }
        return null;
    }


    public List<SeckillSkuRedisTo> blockHandler(BlockException e){
        log.error("getCurrentSeckillSkusResource被限流了");
        return null;
    }




    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //找到所有参与秒杀的key信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys!=null && keys.size()>0){
            //无论哪个场次  只要包含skuId即可
            String regx = "\\d_"+skuId;
            for (String key : keys) {
                //传入正则 和 key进行匹配
                if (Pattern.matches(regx, key)){
                    //如果匹配  根据key获取秒杀哦商品
                    String json = hashOps.get(key);
                    //获取返回数据
                    SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    //随机码
                    //判断当前时间是不是在秒杀时间内  如果是返回随机码  不是则不返回
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    long current = new Date().getTime();
                    if(current>=startTime && current<=endTime){

                    }else{
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }


    //TODO 上架秒杀哦商品的时候  每一个数据都有数据时间
    //TODO 秒杀后续的流程 简化了收货地址等信息
    @Override
    public String kill(String killId, String key, Integer num) {

        MemberEntity respVo = LoginUserInterceptor.loginUser.get();
        // 首先拦截器拦截 是否登录
        //秒杀哦
        //获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        //获取秒杀商品的信息
        String s = hashOps.get(killId);
        if(StringUtils.isEmpty(s)){
            return null;
        }else{
            SeckillSkuRedisTo redisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
            //校验时间合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime - time;
            if(time>=startTime && time<=endTime){
                //2 校验随机码和商品id是否正确
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() +"_"+redisTo.getSkuId();
                if(randomCode.equals(key) && killId.equals(skuId)){
                    //校验通过
                    //验证购物数量是否合法
                    if(num<=redisTo.getSeckillLimit().intValue()){
                        //验证找个人是否已经购买过 幂等性处理  如果秒杀成功 就去redis进行占位 userid + skuid +sessionID
                        //TODO SETNX 商品不存在的时候才占位
                        String redisKey = respVo.getId() + "_" + skuId;
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if(aBoolean){
                            //占位成功  证明从来没有买过
                            //获取分布式信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                                //从信号量获取一个  semaphore.acquire(num);方法是阻塞的
                                //100 毫秒  尝试获取信号量
//                                boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                                boolean b = semaphore.tryAcquire(num);
                                if(b){
                                    //秒杀成功
                                    //快速下单
                                    String timeId = IdWorker.getTimeId();
                                    //说明秒杀成功
                                    //TODO  快速下单 给MQ发送消息 10ms
                                    SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                    seckillOrderTo.setOrderSn(timeId);
                                    seckillOrderTo.setSkuId(redisTo.getSkuId());
                                    seckillOrderTo.setMemberId(respVo.getId());
                                    seckillOrderTo.setNum(num);
                                    seckillOrderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                    seckillOrderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                    rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",seckillOrderTo);
                                    return timeId;
                                }else{
                                    return null;
                                }

                        }else{
                            //说明已经买过了
                            return null;
                        }
                    }

                }else{
                    return null;
                }
            }else{
                return null;
            }

        }


        return null;
    }

}
