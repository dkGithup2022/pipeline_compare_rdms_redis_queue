package com.dk0124.couponcore.repository.mysql;

import com.dk0124.couponcore.model.CouponIssue;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.dk0124.couponcore.model.QCouponIssue.couponIssue;

@Repository
@RequiredArgsConstructor
public class CouponIssueRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public CouponIssue findFirstCouponIssue(long couponId, long userId){
        return jpaQueryFactory.selectFrom(couponIssue)
                .where(couponIssue.couponId.eq(couponId))
                .where(couponIssue.userId.eq(userId))
                .fetchFirst();
    }
}
