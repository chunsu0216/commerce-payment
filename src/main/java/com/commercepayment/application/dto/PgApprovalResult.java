package com.commercepayment.application.dto;

import com.commercepayment.domain.payment.PgApprovalStatus;

import java.time.LocalDateTime;

/**
 * PG 승인 API 호출 결과 값
 */
public record PgApprovalResult(
        PgApprovalStatus status,
        String pgTransactionId,
        Long approvedAmount,
        String pgResultCode,
        String pgResultMessage,
        LocalDateTime approvedAt
) {
}
