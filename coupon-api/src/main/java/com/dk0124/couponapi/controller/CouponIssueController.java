package com.dk0124.couponapi.controller;


import com.dk0124.couponapi.dto.CouponIssueRequestDto;
import com.dk0124.couponapi.service.CouponIssueRequestService;
import com.dk0124.couponcore.component.RedisDistributeLockManager;
import com.dk0124.couponcore.service.CouponAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupon/issue")
public class CouponIssueController {


    private final RedisDistributeLockManager lockManager;

    private final CouponIssueRequestService couponIssueRequestService;

    private final CouponAsyncService couponAsyncService;


    private static final String LOCK_PREFIX_ISSUE_COUPON = "LOCK_PREFIX_ISSUE_COUPON_";

    @PostMapping
    public ResponseEntity issue(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {
        couponIssueRequestService.issueRequestV1(couponIssueRequestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withLock")
    public ResponseEntity issueWithLock(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {
        couponIssueRequestService.issueRequestV2(couponIssueRequestDto);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/withRedisLock")
    public ResponseEntity issueWithRedisLock(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {

        lockManager.proceed(LOCK_PREFIX_ISSUE_COUPON + couponIssueRequestDto.couponId(), 300000L,
                () -> couponIssueRequestService.issueRequestNeedRedis(couponIssueRequestDto)
        );

        return ResponseEntity.ok().build();
    }


    @PostMapping("/async")
    public ResponseEntity issueAsync(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {
        couponAsyncService.asyncIssue(couponIssueRequestDto.couponId(), couponIssueRequestDto.userId());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/asyncWithScript")
    public ResponseEntity issueAsyncWithScript(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {
        couponAsyncService.issueRequestWithScript(couponIssueRequestDto.couponId(), couponIssueRequestDto.userId());
        return ResponseEntity.ok().build();
    }

}
