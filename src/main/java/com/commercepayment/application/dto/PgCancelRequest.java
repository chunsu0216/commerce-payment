package com.commercepayment.application.dto;

/**
 * PG 취소(망취소) API 호출 요청 값
 */
public record PgCancelRequest(
        String mId,
        String pgAuthKey,
        Long cancelAmount,
        String cancelReason
) {
}
