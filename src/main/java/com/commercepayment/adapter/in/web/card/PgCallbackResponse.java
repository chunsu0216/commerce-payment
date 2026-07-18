package com.commercepayment.adapter.in.web.card;

import com.commercepayment.application.dto.PaymentApprovalResult;

/**
 * PG 콜백에 대한 공통 응답 포맷
 */
public record PgCallbackResponse(
        String resultCode,
        String resultMessage
) {

    private static final String SUCCESS_RESULT_CODE = "0000";
    private static final String AUTH_FAILED_RESULT_CODE = "AUTH_FAILED";

    /**
     * 결제 승인 처리 결과를 공통 응답 포맷으로 변환한다
     */
    public static PgCallbackResponse from(PaymentApprovalResult result) {
        if (result.success()) {
            return new PgCallbackResponse(SUCCESS_RESULT_CODE, result.message());
        }
        String resultCode = result.paymentStatus() != null ? result.paymentStatus().name() : AUTH_FAILED_RESULT_CODE;
        return new PgCallbackResponse(resultCode, result.message());
    }

    /**
     * 임의의 결과 코드/메세지로 응답을 생성한다(예외 처리기에서 사용)
     */
    public static PgCallbackResponse of(String resultCode, String resultMessage) {
        return new PgCallbackResponse(resultCode, resultMessage);
    }
}
