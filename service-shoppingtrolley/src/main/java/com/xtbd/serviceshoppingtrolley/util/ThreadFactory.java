package com.xtbd.serviceshoppingtrolley.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * corePoolSize；核心线程数， 当任务数量超过了核心线程数，任务就会放入队列中
 * maximumPoolSize：最大线程数量, 当任务数量超过了最大线程数量，队列也满了， 就会执行拒绝策略，
 * keepAliveTime;存活时间， 当非核心线程没活干的时候， 还能活多久
 * unit：时间单位
 * workQueue：消息队列， 用来存任务， 一版有 ： ArrayBlockingQueue,LinkedBlockingQueue,SynchronousQueue一版使用	LinkedBlockingQueue
 * threadFactory:线程工厂， 用来创建线程池
 * handler： 处理器， 表示拒绝策略， 一般有四种：ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。

 */

@Component
public class ThreadFactory {

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            8,
            10L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    public void execute(Runnable runnable){
        threadPoolExecutor.execute(runnable);
    }

    public Future<?> submit(Runnable runnable){
         return threadPoolExecutor.submit(runnable);

    }

    public Future<?> submit( Callable<?> callable){
        return threadPoolExecutor.submit(callable);
    }



}
