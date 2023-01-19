package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.VO.Catelog2Vo;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class indexController {

    @Autowired
    CategoryService categoryService;

//    注入redis的Bean 自动配置
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;


    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        //视图解析器进行拼串
        //classpath /templates/  + 返回值+ .html
        model.addAttribute("categorys",categoryEntities);
        return "index";
    }


    //index/catalog.json

    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String,List<Catelog2Vo>> getCatalogJson(){

        Map<String,List<Catelog2Vo>> catalogJson = categoryService.getCatelogJson();
        return catalogJson;
    }


    @GetMapping("/index")
    @ResponseBody
    public String hello(){
//        获取一把锁
        RLock lock = redisson.getLock("my-lock");

        //加锁
        lock.lock();   //阻塞式等待，默认加的锁都是30s时间
        //  1) 锁的自动续期，如果业务超长，运行期间自动给锁加上新的30s 不用担心业务时间过长  锁自动过期被删除
        //  2） 加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁。锁默认在30s以后自动删除

        //设置锁的自动解锁时间
        //lock.lock(10, TimeUnit.SECONDS); //10秒自动解锁，自动解锁时间一定要大于业务执行时间
        //问题 lock.lock(1,TimeUnit.SECONDS)不会设置锁的自动续期
        try {
            System.out.println("加锁成功。。。。执行业务"+Thread.currentThread().getId());
            Thread.sleep(10000);
        }catch (Exception e){

        }finally {
            System.out.println("释放锁"+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }



    //读写锁  保证一定能读到最新数据 修改期间 写锁是一个排它锁（互斥锁）  读锁是一个共享锁
    //写锁没释放读就必须等待
    //读+读  相当于无锁  并发读 只会在redis中记录好  所有当前的读锁  他们都会同时加锁成功
    //写+读  等待写锁释放
    //写+写  阻塞模式
    //读+写  有读锁  写也需要等待
    //只要有写的存在  都必须等待
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.writeLock();
        String s = "";
        try {
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(10000);
            stringRedisTemplate.opsForValue().set("writeValue",s);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;
    }



    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.readLock();
        String s = "";
        rLock.lock();
        try {
            s = stringRedisTemplate.opsForValue().get("writeValue");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;

    }






    //闭锁
    //放假  锁门  五个班级全部走完  我们才可以锁大门
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        //开始等待
        door.await();
        return  "放假了";
    }


    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id){
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();  //技术减一

        return id+"班的人都走了。。。";

    }




    //车库停车  占位
    //信号量也可以做分布式限流

    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        //尝试获取车位  没有就算了
        boolean b = park.tryAcquire();
        if (b){
            //执行业务
            return "ok";
        }else{
            return "error"+b;
        }

    }


    @GetMapping("/go")
    @ResponseBody
    public String go(){
        RSemaphore park = redisson.getSemaphore("park");
        //减去一个车位
        park.release();
        return "ok";
    }




}
