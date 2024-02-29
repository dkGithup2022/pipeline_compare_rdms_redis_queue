package com.dk0124.couponcore.service;


import com.dk0124.couponcore.RedisKey;
import com.dk0124.couponcore.component.RedisDistributeLockManager;
import com.dk0124.couponcore.repository.redis.CouponIssueRequest;
import com.dk0124.couponcore.repository.redis.RedisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponAsyncService {
    private final RedisRepository redisRepository;
    private final CouponCacheService couponCacheService;
    private final RedisDistributeLockManager redisDistributeLockManager;
    private final ObjectMapper objectMapper;

    public void asyncIssue(long couponId, long userId) {
        // coupon 을 로컬 캐시 & redis cache
        var cachedCoupon = couponCacheService.getCouponCache(couponId);

        cachedCoupon.checkIssuable(
                redisRepository.sCard(
                        RedisKey.getAsyncCouponIssueKey(couponId)));

        redisDistributeLockManager.proceed(RedisKey.ISSUE_REQUEST_KEY.getIssueRequestLockKey(String.valueOf(couponId)), 100000L, () -> {
            issueRequest(couponId, userId);
        });
    }

    public void asyncIssueWithoutLock(long couponId, long userId) {
        var cachedCoupon = couponCacheService.getCouponCache(couponId);

        cachedCoupon.checkIssuable(
                redisRepository.sCard(
                        RedisKey.getAsyncCouponIssueKey(couponId)));

        issueRequestWithScript(couponId, userId);
    }

    public void issueRequestWithScript(long couponId, long userId) {
        var cachedCoupon = couponCacheService.getCouponCache(couponId); // 쿠폰 메타 데이터에 대한 로컬 & redis cache
        redisRepository.issueRequestWithScript(couponId,userId, cachedCoupon.getTotalQuantity());
    }

    private void issueRequest(long couponId, long userId) {
        CouponIssueRequest couponIssueRequest = new CouponIssueRequest(couponId, userId);
        try {
            String value = objectMapper.writeValueAsString(couponIssueRequest);
            redisRepository.sAdd(RedisKey.getAsyncCouponIssueKey(couponId), String.valueOf(userId));
            redisRepository.rPush(RedisKey.getIssueRequest(), value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
