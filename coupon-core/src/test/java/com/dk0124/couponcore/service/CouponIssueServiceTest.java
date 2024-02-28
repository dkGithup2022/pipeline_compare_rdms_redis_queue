package com.dk0124.couponcore.service;

import com.dk0124.couponcore.TestConfig;
import com.dk0124.couponcore.exception.CouponIssueException;
import com.dk0124.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.dk0124.couponcore.repository.mysql.CouponIssueRepository;
import com.dk0124.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;


class CouponIssueServiceTest extends TestConfig {
    @Autowired
    CouponIssueService couponIssueService;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @BeforeEach
    void clean() {
        couponJpaRepository.deleteAllInBatch();
        couponIssueJpaRepository.deleteAllInBatch();
    }

    @Test
    void 쿠폰발급_실패_동일한_쿠폰_존재() {
        var couponId = 1L;
        var userId = 1L;

        couponIssueService.issue(couponId, userId);
        assertThrows(CouponIssueException.class, () -> couponIssueService.issue(couponId, userId));
    }

    @Test
    void 쿠폰발급_성공() {
        var couponId = 1L;
        var userId = 1L;

        couponIssueService.issue(couponId, userId);

        var issued = couponIssueRepository.findFirstCouponIssue(couponId,userId);
        assertNotNull(issued);
        assertEquals(1L, issued.getCouponId());
        assertEquals(1L, issued.getUserId());
    }



}