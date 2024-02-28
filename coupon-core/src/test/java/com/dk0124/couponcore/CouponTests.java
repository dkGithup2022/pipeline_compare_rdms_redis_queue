package com.dk0124.couponcore;

import com.dk0124.couponcore.model.Coupon;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CouponTests {

    @Test
    public void 쿠폰_발급_성공_일자_갯수_통과() {
        var coupon = Coupon.builder()
                .totalQuantity(1000)
                .issuedQuantity(0)
                .dateIssueStarted(LocalDateTime.now().minusDays(1))
                .dateIssueEnded(LocalDateTime.now().plusDays(1))
                .build();

        var result = coupon.availableIssueDate() && coupon.availableIssueQuantity();
        assertTrue(result);
    }

    @Test
    public void 쿠폰_발급_실패_갯수_초과() {
        var coupon = Coupon.builder()
                .totalQuantity(1000)
                .issuedQuantity(1000)
                .dateIssueStarted(LocalDateTime.now().minusDays(1))
                .dateIssueEnded(LocalDateTime.now().plusDays(1))
                .build();
        assertThrows(RuntimeException.class, () -> coupon.issue());
    }

    @Test
    public void 쿠폰_발급_실패_일자_초과() {
        var coupon = Coupon.builder()
                .dateIssueStarted(LocalDateTime.now().minusDays(1))
                .dateIssueEnded(LocalDateTime.now().minusDays(1))
                .build();
        assertThrows(RuntimeException.class, () -> coupon.issue());
    }

}
