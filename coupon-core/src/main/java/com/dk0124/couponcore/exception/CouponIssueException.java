package com.dk0124.couponcore.exception;

public class CouponIssueException extends RuntimeException{
    private final ErrorCode errorCode;
    private final String message;

    public CouponIssueException(ErrorCode errorCode, String message1) {
        this.errorCode = errorCode;
        this.message = message1;
    }

}
