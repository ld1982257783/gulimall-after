package com.atguigu.gulimall.product.Thread;

import java.util.concurrent.*;

public class ThreadText {


//  业务处理上  前三种方法都不使用  耗费资源 内存  将所有的多线程异步任务都交给线程池进行执行
//当前系统中线程池只有一两个
//ExecutorService executorService = Executors.newFixedThreadPool(10);





    //

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //调用此线程  1     //  初始化线程的四种方法
        //    //1 继承thread class
//        thread1 thread1 = new thread1();
//        thread1.start();
//        System.out.println("调用线程完毕");



        //2 实现Runnable接口
//        thread2 thread2 = new thread2();
//        new Thread(thread2).start();
//        System.out.println("调用Runnable接口");



        //3 实现callable接口  +futuretask  （可以拿到返回结果 可以处理异常）
        //可以返回返回值
        FutureTask<Integer> integerFutureTask = new FutureTask<>(new thread3());
        new Thread(integerFutureTask).start();
        //等待整个线程执行完成  获取返回结果
        Integer integer = integerFutureTask.get();
        System.out.println("调用callable接口");
        System.out.println(integer);








        //原生创建线程池的方法
//        int corePoolSize,   核心线程数
//        int maximumPoolSize,      最大线程数
//        long keepAliveTime,         线程最大存活时间
//        TimeUnit unit,                时间单位
//        BlockingQueue<Runnable> workQueue,   阻塞队列数量  如果任务很多
//        ThreadFactory threadFactory,             线程创建工厂  一般默认
//        RejectedExecutionHandler handler              线程执行协议

        //基本  执行顺序   例如
        //1.1线程池创建  准备好core数量的核心线程  准备接受任务
        //1.2core满了  就会再进来的任务放到阻塞队列  空闲core会去阻塞队列里面自动获取任务执行
        //1.3阻塞队列满了  就会创建  （最大线程数-核心线程数） 的线程
        //1.4最大线程数也满了  再进来的任务就会被拒绝


        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );


    }




    //  初始化线程的四种方法
    //1 继承thread class
    public static class  thread1 extends Thread{

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("数字"+i);
        }
    }



    //2 实现Runnable接口
    public static class thread2 implements Runnable{
        @Override
        public void run() {
            System.out.println(Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("数字"+i);
        }
    }



    //3  实现callable接口
    public static class thread3 implements Callable<Integer> {


        @Override
        public Integer call() throws Exception {
            System.out.println(Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("数字"+i);
            return i;
        }
    }



}
