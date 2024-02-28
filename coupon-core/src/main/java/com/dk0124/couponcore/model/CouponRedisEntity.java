package com.dk0124.couponcore.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CouponRedisEntity {

    private Long id;

    private CouponType type;

    private Integer totalQuantity;


    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dateIssueStarted;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dateIssueEnded;


    public static CouponRedisEntity fromCoupon(Coupon coupon) {
        return new CouponRedisEntity(
                coupon.getId(),
                coupon.getType(),
                coupon.getTotalQuantity(),
                coupon.getDateIssueStarted(),
                coupon.getDateIssueEnded()
        );
    }

    public boolean availableIssueDate() {
        return dateIssueStarted.isBefore(LocalDateTime.now()) && dateIssueEnded.isAfter(LocalDateTime.now());
    }

    public void checkIssuable(Long currentCount) {
        if (!availableIssueDate())
            throw new RuntimeException("발급 가능 기간 아님 ");

        if (totalQuantity != null && currentCount >= totalQuantity)
            throw new RuntimeException("발급 가능 갯수 추가 ");


    }
}
