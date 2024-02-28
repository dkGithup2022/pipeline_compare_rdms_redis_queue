package com.dk0124.couponcore.model;

import com.dk0124.couponcore.exception.CouponIssueException;
import com.dk0124.couponcore.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "coupon")
public class Coupon {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType type;

    private Integer totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int discountAmount;

    @Column(nullable = false)
    private int minAvailableAmount;

    @Column(nullable = false)
    private LocalDateTime dateIssueStarted;

    @Column(nullable = false)
    private LocalDateTime dateIssueEnded;


    public boolean availableIssueQuantity() {
        if (totalQuantity == null)
            return true;

        return totalQuantity > issuedQuantity;
    }

    public boolean availableIssueDate() {
        var now = LocalDateTime.now();
        return dateIssueStarted.isBefore(now) && dateIssueEnded.isAfter(now);
    }

    public void issue() {
        if (!availableIssueDate())
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_EXCEPTION, "일자에 맞지 않음");
        if (!availableIssueQuantity())
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_COUNT_EXCEPTION, "갯수에 맞지 않음");

        issuedQuantity++;
    }


}
