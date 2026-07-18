package com.commercepayment.common.exception;

/**
 * 동일한 PG 인증키로 이미 처리된 인증 콜백이 재수신되었을 때 발생하는 예외
 */
public class DuplicatePaymentAuthException extends RuntimeException {

    public DuplicatePaymentAuthException(String pgAuthKey) {
        super("이미 처리된 인증 콜백입니다. pgAuthKey=" + pgAuthKey);
    }
}
