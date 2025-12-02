package com.zc.anti.auto.redisson;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author zhangcheng
 * @Description
 * @Date 2025/12/2 下午12:18
 */
@Component
public class Demo {

    public static String RESOURCE_KEY = "resource:batch";

    /**
     * 最大重试次数
     */
    public static int MAX_RETRY_COUNT = 10;

    // 定义500个资源
    public static int count = 100;

    RedissonClient redissonClient;

    /**
     * 执行
     */
    public void produce() throws InterruptedException {

        long startTime = System.currentTimeMillis();

        CountDownLatch countDownLatch = new CountDownLatch(100);

        // 定义五个线程来模拟5个用户访问
        int threadSize = 5;

        for (int i = 0; i < threadSize; i++) {
            Thread thread = new Thread(() -> {
                // 当前线程获取到的资源总数
                AtomicInteger acquiredCount = new AtomicInteger(0);
                // 每个线程要获取20个资源
                for (int j = 0; j < 20; j++) {
                    try {
                        request(acquiredCount);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
                System.out.println("当前线程：" + Thread.currentThread().getName() + "，获取到的资源总数：" + acquiredCount);
            }, "线程" + i);
            thread.start();
        }
        countDownLatch.await();

        System.out.println("资源还剩：" + count);

        System.out.println("耗时：" + (System.currentTimeMillis() - startTime));
    }

    /**
     * 模拟一次请求
     */
    public void request(AtomicInteger acquiredCount) {
        // 重试次数
        int retryCount = 0;
        // 是否获取到资源
        boolean success = false;
        RLock lock = redissonClient.getLock(RESOURCE_KEY);
        try {
            do {
                // 尝试获取锁
                if (lock.tryLock(1, 3, TimeUnit.MICROSECONDS)) {
                    try {
                        // 扣减一个资源
                        count--;
                        System.out.println("当前线程：" + Thread.currentThread().getName() + "，获取到一个资源！");
                        acquiredCount.incrementAndGet();
                        success = true;
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    System.out.println("当前线程：" + Thread.currentThread().getName() + ",获取锁失败！");
                    // 休眠一秒，再次竞争
                    TimeUnit.SECONDS.sleep(1);
                    retryCount++;
                }
            } while (!success && retryCount < MAX_RETRY_COUNT);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}