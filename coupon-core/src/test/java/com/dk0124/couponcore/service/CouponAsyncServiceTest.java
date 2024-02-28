package com.dk0124.couponcore.service;

import com.dk0124.couponcore.RedisKey;
import com.dk0124.couponcore.TestConfig;
import com.dk0124.couponcore.model.Coupon;
import com.dk0124.couponcore.model.CouponType;
import com.dk0124.couponcore.repository.mysql.CouponJpaRepository;
import com.dk0124.couponcore.repository.redis.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


class CouponAsyncServiceTest extends TestConfig {

    @Autowired
    private CouponAsyncService couponAsyncService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    public void clear() {
        var keys = redisTemplate.keys("*");
        redisTemplate.delete(keys);
    }

    @Test
    void 쿠폰_수량_검증_성공_발급_가능한_수량() {

        long couponId = 1L;
        long userId = 1L;

        couponAsyncService.asyncIssue(couponId, userId);
        Boolean isSaved = redisTemplate.opsForSet().isMember(RedisKey.getAsyncCouponIssueKey(couponId), String.valueOf(userId));

        assertTrue(isSaved);
    }


    @Test
    void 쿠폰_발급_큐_적재_성공() throws JsonProcessingException {
        long couponId = 1L;
        long userId = 1L;

        CouponIssueRequest couponIssueRequest = new CouponIssueRequest(couponId, userId);
        couponAsyncService.asyncIssue(couponId, userId);

        String savedIssueRequest = redisTemplate.opsForList().leftPop(RedisKey.getIssueRequest());
        assertEquals(savedIssueRequest, objectMapper.writeValueAsString(couponIssueRequest));
    }

    @Test
    void 쿠폰_발급_큐_적재_실패_갯수_제한_초과() {
        int totalQuantity = 10 ;

        var coupon = readyCoupon(totalQuantity);
        long couponId = coupon.getId();


        IntStream.range(0, totalQuantity)
                .forEach(e -> couponAsyncService.asyncIssue(couponId, e));


        assertThrows(RuntimeException.class, () -> couponAsyncService.asyncIssue(couponId, totalQuantity + 1));
    }


    @Test
    void 스크립트_쿼리_쿠폰_등록_성공(){
        int totalQuantity = 10 ;

        var coupon = readyCoupon(totalQuantity);
        long couponId = coupon.getId();

        assertDoesNotThrow(()->IntStream.range(0, totalQuantity)
                .forEach(e -> couponAsyncService.issueRequestWithScript(couponId, e)) );
    }

    @Test
    void 스크립트_쿼리_쿠폰_등록_실패_갯수_초과(){
        int totalQuantity = 10 ;

        var coupon = readyCoupon(totalQuantity);
        long couponId = coupon.getId();

        IntStream.range(0, totalQuantity)
                .forEach(e -> couponAsyncService.issueRequestWithScript(couponId, e));

        assertThrows(RuntimeException.class , ()-> couponAsyncService.issueRequestWithScript(couponId, 11));
    }

    @Test
    void 스크립트_쿼리_쿠폰_등록_실패_중복_유저(){
        int totalQuantity = 10 ;
        long duplicatedUser = 1;

        var coupon = readyCoupon(totalQuantity);
        long couponId = coupon.getId();

        couponAsyncService.issueRequestWithScript(couponId, duplicatedUser);

        assertThrows(RuntimeException.class , ()-> couponAsyncService.issueRequestWithScript(couponId, duplicatedUser));
    }

    private Coupon readyCoupon(int totalQuantity){

        var coupon = Coupon.builder()
                .id(null)
                .dateIssueStarted(LocalDateTime.now().minusDays(10))
                .dateIssueEnded(LocalDateTime.now().plusDays(10))
                .issuedQuantity(0)
                .totalQuantity(totalQuantity)
                .type(CouponType.FIRST_COME_FIRST_SERVED)
                .discountAmount(0)
                .minAvailableAmount(0)
                .title("coupon FOR TEST ")
                .build();

        return couponJpaRepository.saveAndFlush(coupon);
    }
}