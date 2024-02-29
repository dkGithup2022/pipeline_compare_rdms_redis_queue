package com.dk0124.couponconsumer;


import com.dk0124.couponcore.RedisKey;
import com.dk0124.couponcore.model.CouponIssue;
import com.dk0124.couponcore.repository.mysql.CouponIssueJpaRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.dk0124.couponcore.RedisKey.getCouponIssueSetKey;

@RequiredArgsConstructor
@EnableScheduling
@Component
@Slf4j
public class CouponListener {

    private final CouponIssueService couponIssueService;
    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final RedisRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final String queueName = RedisKey.getIssueRequestKey();

    static long begin = System.currentTimeMillis();
    static long fetched = 0;

    @Scheduled(fixedDelay = 500)
    public void issue() throws JsonProcessingException {
        log.info("listen .... ");
        CouponIssueRequest target = getIssueTarget();
        List<CouponIssue> toBePersisted = redisRepository.popItemsWithScript(queueName, 500)
                .stream().map(e -> {
                    try {
                        var issueReq = objectMapper.readValue(e, CouponIssueRequest.class);
                        return CouponIssue.builder().couponId(issueReq.couponId()).userId(issueReq.userId()).dateIssued(LocalDateTime.now()).build();
                    } catch (JsonProcessingException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toList();

        fetched += couponIssueJpaRepository.saveAll(toBePersisted).size();
        log.info("발급 완료 target " + target);
        log.info("FETCHED {} ROWS ..... TOTAL TIME TOOK : {}, ", fetched, System.currentTimeMillis() - begin);
        removeIssueTarget();
    }

    private void removeIssueTarget() {
        redisRepository.lPop(queueName);
    }

    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        return objectMapper.readValue(redisRepository.lIndex(queueName, 0), CouponIssueRequest.class);
    }

}
