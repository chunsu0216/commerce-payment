package com.commercepayment.application.dto;

/**
 * TX1(인증/결제 등록) 처리 결과
 */
public record PaymentAuthRegistrationResult(
        String paymentId,
        String authId,
        Long requestAmount
) {
}
