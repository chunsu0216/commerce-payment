package com.commercepayment.application.dto;

import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;

/**
 * 카드 결제 승인 플로우 전체의 최종 처리 결과
 */
public record PaymentApprovalResult(
        boolean success,
        String paymentId,
        String authId,
        PaymentStatus paymentStatus,
        String message
) {
}
