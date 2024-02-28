package com.dk0124.couponcore;

public enum RedisKey {

    ISSUE_REQUEST_KEY,

    COUPON_ISSUE_REQUEST_PREFIX,

    ASYNC_COUPON_ISSUE_KEY,

    COUPON_ISSUE_SET_KEY_PREFIX,

    ISSUE_REQUEST;


    public static String getIssueRequestKey() {
        return ISSUE_REQUEST_KEY.ISSUE_REQUEST_KEY.name();
    }

    public static String getIssueRequestLockKey(String couponId) {
        if (couponId == null)
            throw new RuntimeException("Coupon id is null");

        return COUPON_ISSUE_REQUEST_PREFIX.name() + "_" + couponId;
    }

    public static String getAsyncCouponIssueKey(long couponId) {
        return ASYNC_COUPON_ISSUE_KEY.name() + "_" + couponId;
    }

    public static String getIssueRequest() {
        return ISSUE_REQUEST_KEY.name();
    }

    public static String getCouponIssueSetKey(long couponId) {
        return COUPON_ISSUE_SET_KEY_PREFIX.name() + "_" + couponId;
    }

}
