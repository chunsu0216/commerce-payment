package com.commercepayment.application.dto;

import com.commercepayment.domain.payment.PgCancelStatus;

/**
 * PG 취소(망취소) API 호출 결과 값
 */
public record PgCancelResult(
        PgCancelStatus status,
        String pgCancelId,
        String pgResultCode,
        String pgResultMessage
) {
}
