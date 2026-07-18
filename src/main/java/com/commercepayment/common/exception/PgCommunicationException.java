package com.commercepayment.common.exception;

/**
 * PG사와의 통신(승인/취소 API 호출)에 실패했을 때 발생하는 예외
 */
public class PgCommunicationException extends RuntimeException {

    public PgCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
