package com.commercepayment.common.exception;

/**
 * paymentId 로 결제 정보를 조회할 수 없을 때 발생하는 예외
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("결제 정보를 찾을 수 없습니다. paymentId=" + paymentId);
    }
}
