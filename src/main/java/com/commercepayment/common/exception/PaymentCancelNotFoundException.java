package com.commercepayment.common.exception;

/**
 * cancelId 로 결제 취소 정보를 조회할 수 없을 때 발생하는 예외
 */
public class PaymentCancelNotFoundException extends RuntimeException {

    public PaymentCancelNotFoundException(String cancelId) {
        super("결제 취소 정보를 찾을 수 없습니다. cancelId=" + cancelId);
    }
}
