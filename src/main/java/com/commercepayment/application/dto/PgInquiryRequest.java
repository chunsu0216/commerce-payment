package com.commercepayment.application.dto;

/**
 * PG 거래결과 조회 API 호출 요청 값.
 * referenceId 는 로그 추적용 식별자로, 망취소 복구 흐름에서는 PaymentCancel 의 cancelId 를 전달한다.
 */
public record PgInquiryRequest(
        String mId,
        String pgAuthKey,
        String referenceId
) {
}
