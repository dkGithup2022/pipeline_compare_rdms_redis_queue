package com.dk0124.couponcore.repository.mysql;

import com.dk0124.couponcore.model.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueJpaRepository  extends JpaRepository<CouponIssue, Long> {
}
