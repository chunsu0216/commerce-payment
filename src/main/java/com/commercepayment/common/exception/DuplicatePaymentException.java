package com.commercepayment.common.exception;

/**
 * 동일 주문/회원에 대해 이미 진행 중이거나 처리 완료된 결제가 존재할 때 발생하는 예외
 */
public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String orderId, Long memberId) {
        super("이미 진행 중이거나 처리된 결제가 존재합니다. orderId=" + orderId + ", memberId=" + memberId);
    }
}
