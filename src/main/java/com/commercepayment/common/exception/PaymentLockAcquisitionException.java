package com.commercepayment.common.exception;

/**
 * 지정된 대기시간 내에 분산락을 획득하지 못했을 때 발생하는 예외
 */
public class PaymentLockAcquisitionException extends RuntimeException {

    public PaymentLockAcquisitionException(String lockKey) {
        super("분산락 획득에 실패했습니다. lockKey=" + lockKey);
    }
}
