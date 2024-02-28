package com.dk0124.couponcore.service;

import com.dk0124.couponcore.exception.CouponIssueException;
import com.dk0124.couponcore.exception.ErrorCode;
import com.dk0124.couponcore.model.Coupon;
import com.dk0124.couponcore.model.CouponIssue;
import com.dk0124.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.dk0124.couponcore.repository.mysql.CouponIssueRepository;
import com.dk0124.couponcore.repository.mysql.CouponJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.dk0124.couponcore.exception.ErrorCode.COUPON_NOT_EXIST;

@RequiredArgsConstructor
@Service
public class CouponIssueService {

    private final CouponJpaRepository couponJpaRepository;
    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public synchronized void issue(long couponId, long userId) {
        var coupon = findCoupon(couponId);
        coupon.issue();
        saveCouponIssue(couponId, userId);
    }


    @Transactional(readOnly = true)
    public Coupon findCoupon(long couponId) {
        return couponJpaRepository.findById(couponId)
                .orElseThrow(() -> new CouponIssueException(COUPON_NOT_EXIST, "ID 에 해당하는 쿠폰이 없습니다."));
    }


    @Transactional
    public synchronized void issueWithLock(long couponId, long userId) {
        var coupon = findCouponWithLock(couponId);
        coupon.issue();
        saveCouponIssue(couponId, userId);
    }



    @Transactional
    public Coupon findCouponWithLock(long couponId) {
        return couponJpaRepository.findCouponWithLock(couponId).orElseThrow(() -> {
            throw new CouponIssueException(COUPON_NOT_EXIST, "ID 에 해당하는 쿠폰이 없습니다.".formatted(couponId));
        });
    }

    private void saveCouponIssue(long couponId, long userId) {
        checkCouponIssueExist(couponId, userId);
        couponIssueJpaRepository.save(
                CouponIssue.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .dateIssued(LocalDateTime.now())
                        .build()
        );
    }

    private void checkCouponIssueExist(long couponId, long userId) {
        if (couponIssueRepository.findFirstCouponIssue(couponId, userId) != null)
            throw new CouponIssueException(ErrorCode.COUPON_ISSUE_ALREADY_EXIST, "쿠폰 번호와 유저가 이미 존재합니다");
    }
}
