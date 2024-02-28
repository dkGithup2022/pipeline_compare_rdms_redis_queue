package com.dk0124.couponcore.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributeLockManager {

    private final RedissonClient redissonClient;

    public void proceed(String key, Long millisInWait, Runnable runnable){

        RLock lock = redissonClient.getLock(key);
        try{
            var available = lock.tryLock(millisInWait, millisInWait, TimeUnit.MILLISECONDS);
            if(available){
                runnable.run();
            }
            else{
                log.error("CANNOT GET DISTRIBUTED LOCK KEY : {}" , key);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
}
