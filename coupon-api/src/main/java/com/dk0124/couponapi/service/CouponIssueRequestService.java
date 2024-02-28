package com.dk0124.couponapi.service;

import com.dk0124.couponapi.dto.CouponIssueRequestDto;
import com.dk0124.couponcore.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class CouponIssueRequestService {

    private final CouponIssueService couponIssueService;

    public synchronized void issueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info("쿠폰 발급 완료 couponId : %s , userId : %s ", requestDto.couponId(), requestDto.userId());
    }

    public void issueRequestV2(CouponIssueRequestDto requestDto) {
        couponIssueService.issueWithLock(requestDto.couponId(), requestDto.userId());
        log.info("쿠폰 발급 완료 couponId : %s , userId : %s ", requestDto.couponId(), requestDto.userId());
    }

    public void issueRequestNeedRedis(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info("쿠폰 발급 완료 couponId : %s , userId : %s ", requestDto.couponId(), requestDto.userId());
    }
}
