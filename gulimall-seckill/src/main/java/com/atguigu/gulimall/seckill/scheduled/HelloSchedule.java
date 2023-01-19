package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务自动配置类  TaskSchedulingAutoConfiguration
 *
 * 异步任务自动配置类  TaskExecutionAutoConfiguration
 *
 * 定时任务表达式  cron
 * (cron = "* * * * * *")  秒分时 日月周
 * spring原生的异步任务
 * 1 @EnableAsync 开启异步任务功能
 * 2
 */
//(cron = "*/5 * * * * 7")  每五秒执行一次

//@EnableAsync
//@EnableScheduling  //开启定时任务
@Component
@Slf4j
public class HelloSchedule {


    /**
     * 1 spring中由六位组成 不允许第七位的年
     * 2 在周几 1-7代表周一到周日 mon-sun
     * 3 定时任务不应该阻塞  默认是阻塞的
     *      1) 可以让业务运行以异步的方式 自己提交到线程池
     *         CompletableFuture.runAsync(() -> {
     *             xxxservice.hello();
     *         },executor);
     *      2) 支持定时任务线程池 设置TaskSchedulingProperties
     *              #设置定时任务线程数
     *              spring.task.scheduling.pool.size=5
     *      3) 让定时任务异步执行
     *          异步任务、
     *          解决使用异步任务+定时任务不阻塞的功能
     */
    @Async
    @Scheduled(cron = "* * * ? * 7")
    public void hello() throws InterruptedException {
        Thread.sleep(3000);
        log.info("hello");

    }
}
