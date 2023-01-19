package com.atguigu.gulimall.product.Thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureText {

    //自己创建线程池
    private static ExecutorService executor1 = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {
       //使用异步编排执行任务   CompletableFuture
//        CompletableFuture.runAsync(()->{
//            System.out.println("当前线程"+Thread.currentThread().getId());
//            int i = 10/2;
//            System.out.println("运行结果"+i);
//        },executor1);


        //1.2 有返回值  异步任务
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果" + i);
//            return i;
//        }, executor1);
//        Integer integer = integerCompletableFuture.get();
//        System.out.println("返回结果为"+integer);


        //1.3  2  计算完成时回调方法
//        CompletableFuture<Integer> result1 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("计算完成");
//            int o = 10 / 2;
//            System.out.println("结果为" + o);
//            return o;
//        }, executor1).whenComplete((res, exception) -> {
//            //虽然能得到异常信息  但是没法修改返回数据
//            System.out.println("异步任务完成了,结果是" + res + "异常是," + exception);
//
//        }).exceptionally(throwable -> {
//                    //可以感知异常 同时返回默认值
//                    return 10;
//                }
//        );
//        System.out.println(result1.get());



        // 1.4  handle()方法执行完成后的处理
//        CompletableFuture<Integer> handle = CompletableFuture.supplyAsync(() -> {
//            System.out.println("1234");
//            int p = 10 / 0;
//            System.out.println("输出结果为" + p);
//            return p;
//        }, executor1).handle((res, thr) -> {
//            if (res != null) {
//                return res * 2;
//            }
//            if (thr != null) {
//                return 0;
//            }
//            return 0;
//        });
//        System.out.println("输出结果为"+handle.get());


        //1.5  线程串行化方法
        //thenRunAsync  无法获取上一步的感知结果 无返回值
        //thenAcceptAsync  可以获取上一步的结果  没有返回值
        //thenApplyAsync    既可以获取上一步的结果  也可以有返回值
//        CompletableFuture.supplyAsync(()->{
//            System.out.println("线程1启动了");
//            int i = 10/2;
//            System.out.println("输出结果为"+i);
//            return i;
//        },executor1).thenRunAsync(()->{
//            //感知上一步的结果 但是没有返回值
//            System.out.println("线程2启动了");
//            System.out.println("无法获取上一步的感知结果");
//        },executor1);

//        CompletableFuture.supplyAsync(()->{
//            System.out.println("线程1启动了");
//            int i = 10/2;
//            System.out.println("输出结果为"+i);
//            return i;
//        },executor1).thenAcceptAsync((res)->{
//            //感知上一步的结果 但是没有返回值
//            System.out.println("线程2启动了");
//            System.out.println("可以获取线程1的返回值"+res);
//        },executor1);

//        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("线程1启动了");
//            int i = 10 / 2;
//            System.out.println("输出结果为" + i);
//            return i;
//        }, executor1).thenApplyAsync((res) -> {
//            //感知上一步的结果 但是没有返回值
//            System.out.println("线程2启动了");
//            System.out.println("可以获取线程1的返回值" + res);
//            int l = 12 / 2;
//            return l;
//        }, executor1);
//        System.out.println("我不但可以接受上一步的返回值结果，我还有返回值哦"+completableFuture.get());




        //1.6  两个任务都必须完成  合并两个异步任务
        //runAfterBothAsync   无法获取前两个任务的返回值   自己无返回值
        //thenAcceptBothAsync   可以获取前两步的返回值结果  自己无返回值
        //thenCombineAsync      可以获取前两步的返回值结果   自己有返回值

        //runAfterEitherAsync  两个任务有一个运行就会运行3   无法获取前两个任务的返回值   自己无返回值
        //acceptEitherAsync   两个任务有一个运行就会执行3    可以回去其中一个返回值  前提是前两个任务的返回值类型需要一直  自己无返回值
        //applyToEitherAsync   两个任务有一个运行就会执行3    可以回去其中一个返回值
        // 前提是前两个任务的返回值类型需要一直  自己有返回值
//        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1开始启动了");
//            int i = 10 / 2;
//            System.out.println("任务1执行结果为" + i);
//            return i;
//        }, executor1);
//        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2开始启动了");
//            int i = 12 / 2;
//            System.out.println("任务2执行结果为" + i);
//            return i;
//        }, executor1);
////
//        System.out.println("开始准备第三个任务");
//        future1.runAfterBothAsync(future2,()->{
//            //第三个异步任务
//            System.out.println("任务3开始启动了");
//        },executor1);

//        future1.thenAcceptBothAsync(future2,(res1,res2)->{
//            System.out.println("前两个任务的结果为"+res1+"---"+res2);
//            System.out.println("第三个任务结束了");
//        },executor1);


//        CompletableFuture<Integer> completableFuture = future1.thenCombineAsync(future2, (res1, res2) -> {
//            System.out.println("前两步的返回结果和为" + (res1 + res2));
//            return res1 + res2;
//        }, executor1);
//        System.out.println("第三部的返回值结果为"+completableFuture.get());


//        future1.runAfterEitherAsync(future2,()->{
//            System.out.println("任务3开始运行了");
//        },executor1);

//        future1.acceptEitherAsync(future2,(res1)->{
//            System.out.println("任务3开始运行了，前两部的其中一个返回值为"+res1);
//        },executor1);

//        CompletableFuture<Integer> completableFuture = future1.applyToEitherAsync(future2, (res2) -> {
//            System.out.println("感知结果为" + res2);
//            int i = 10 / 1;
//            return i;
//        }, executor1);
//        System.out.println("任务3的返回值为"+completableFuture.get());




        //多任务组合  多个异步任务
        CompletableFuture<Long> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("异步任务1开始运行了");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Thread.currentThread().getId();
        }, executor1);

        CompletableFuture<Long> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("异步任务2开始运行了");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Thread.currentThread().getId();
        }, executor1);

        CompletableFuture<Long> future3 = CompletableFuture.supplyAsync(() -> {
            System.out.println("异步任务3开始运行了");
            return Thread.currentThread().getId();
        }, executor1);

        //三个任务都要运行
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(future1, future2, future3);
//        //等待三个任务都运行完
//        voidCompletableFuture.get();
//        System.out.println("三个任务已经运行完毕,,end");

        //三个任务有一个运行就可以
        CompletableFuture<Object> objectCompletableFuture = CompletableFuture.anyOf(future1, future2, future3);
        objectCompletableFuture.get();
        System.out.println("三个任务已经有一个运行完毕");

    }
}
