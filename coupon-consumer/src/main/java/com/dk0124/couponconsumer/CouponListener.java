package com.dk0124.couponconsumer;


import com.dk0124.couponcore.RedisKey;
import com.dk0124.couponcore.repository.redis.CouponIssueRequest;
import com.dk0124.couponcore.repository.redis.RedisRepository;
import com.dk0124.couponcore.service.CouponIssueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.dk0124.couponcore.RedisKey.getCouponIssueSetKey;

@RequiredArgsConstructor
@EnableScheduling
@Component
@Slf4j
public class CouponListener {

    private final CouponIssueService couponIssueService;
    private final RedisRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final String queueName = RedisKey.getIssueRequestKey();


    @Scheduled(fixedDelay = 1000)
    public void issue() throws JsonProcessingException {


        log.info("listen .... ");
        CouponIssueRequest target  = getIssueTarget();
        log.info("발급 시작 : " + target);
        couponIssueService.issue(target.couponId(), target.userId());
        log.info("발급 완료 target " + target);
        removeIssueTarget();
        
    }

    private void removeIssueTarget() {
        redisRepository.lPop(queueName);
    }

    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        return objectMapper.readValue(redisRepository.lIndex(queueName, 0), CouponIssueRequest.class);
    }

}
