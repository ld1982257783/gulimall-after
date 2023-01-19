package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 *  每天晚上3点  上架最近三天需要秒杀的商品
 *  当天晚上00:00:00  - 23:59:59
 *  明天00:00:00 - 23:59:59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";


    //TODO 幂等性处理  使用分布式锁锁住当前任务
    @Scheduled(cron = "0/10 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){
        //重复上架  无须处理
        log.info("商品开始上架");
        //设置一把锁
        RLock lock = redissonClient.getLock(upload_lock);
        //设置锁的过期时间
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }



    }

}
