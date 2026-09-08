package com.commercepayment.application.dto;

import com.commercepayment.domain.payment.PgInquiryStatus;

/**
 * PG 거래결과 조회 API 호출 결과 값
 */
public record PgInquiryResult(
        PgInquiryStatus status,
        String pgTransactionId,
        String pgResultCode,
        String pgResultMessage
) {
}
