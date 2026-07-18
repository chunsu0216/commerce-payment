package com.commercepayment.adapter.in.web.card;

import com.commercepayment.common.exception.DuplicatePaymentAuthException;
import com.commercepayment.common.exception.DuplicatePaymentException;
import com.commercepayment.common.exception.PaymentCompensatedException;
import com.commercepayment.common.exception.PaymentLockAcquisitionException;
import com.commercepayment.common.exception.PaymentNotFoundException;
import com.commercepayment.common.exception.PgCommunicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 카드 결제 콜백 처리 중 발생하는 예외를 PG 가 이해할 수 있는 공통 응답 포맷으로 변환하는 예외 처리기
 */
@RestControllerAdvice(basePackages = "com.commercepayment.adapter.in.web.card")
@Slf4j
public class CardAuthExceptionHandler {

    /**
     * 인증 콜백 중복 수신 예외를 처리한다
     */
    @ExceptionHandler(DuplicatePaymentAuthException.class)
    public PgCallbackResponse handleDuplicateAuth(DuplicatePaymentAuthException e) {
        log.warn("중복된 인증 콜백입니다.", e);
        return PgCallbackResponse.of("DUPLICATE_AUTH", e.getMessage());
    }

    /**
     * 결제 중복 요청 예외를 처리한다
     */
    @ExceptionHandler(DuplicatePaymentException.class)
    public PgCallbackResponse handleDuplicatePayment(DuplicatePaymentException e) {
        log.warn("중복된 결제 요청입니다.", e);
        return PgCallbackResponse.of("DUPLICATE_PAYMENT", e.getMessage());
    }

    /**
     * 결제 정보 조회 실패 예외를 처리한다
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public PgCallbackResponse handlePaymentNotFound(PaymentNotFoundException e) {
        log.error("결제 정보를 찾을 수 없습니다.", e);
        return PgCallbackResponse.of("PAYMENT_NOT_FOUND", e.getMessage());
    }

    /**
     * 분산락 획득 실패 예외를 처리한다
     */
    @ExceptionHandler(PaymentLockAcquisitionException.class)
    public PgCallbackResponse handleLockAcquisition(PaymentLockAcquisitionException e) {
        log.warn("락 획득에 실패했습니다.", e);
        return PgCallbackResponse.of("LOCK_TIMEOUT", e.getMessage());
    }

    /**
     * PG 통신 실패 예외를 처리한다
     */
    @ExceptionHandler(PgCommunicationException.class)
    public PgCallbackResponse handlePgCommunication(PgCommunicationException e) {
        log.error("PG 통신 중 오류가 발생했습니다.", e);
        return PgCallbackResponse.of("PG_COMMUNICATION_ERROR", e.getMessage());
    }

    /**
     * TX2 실패 후 보상 처리까지 완료된 예외를 처리한다
     */
    @ExceptionHandler(PaymentCompensatedException.class)
    public PgCallbackResponse handleCompensated(PaymentCompensatedException e) {
        log.error("결제 결과 반영 실패로 보상 처리가 수행되었습니다.", e);
        return PgCallbackResponse.of("COMPENSATED", e.getMessage());
    }

    /**
     * 위에서 명시적으로 처리하지 않은 예기치 못한 예외를 처리한다(catch-all)
     */
    @ExceptionHandler(Exception.class)
    public PgCallbackResponse handleUnexpected(Exception e) {
        log.error("예기치 못한 오류가 발생했습니다.", e);
        return PgCallbackResponse.of("INTERNAL_ERROR", "일시적인 오류가 발생했습니다.");
    }
}
