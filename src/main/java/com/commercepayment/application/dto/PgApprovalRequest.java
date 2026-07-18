package com.commercepayment.application.dto;

/**
 * PG 승인 API 호출 요청 값
 */
public record PgApprovalRequest(
        String mId,
        String pgAuthKey,
        Long approvedAmount
) {
}
