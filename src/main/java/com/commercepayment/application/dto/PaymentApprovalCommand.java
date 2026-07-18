package com.commercepayment.application.dto;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;

/**
 * PG 인증 콜백을 공통화한 결제 승인 요청 커맨드
 */
public record PaymentApprovalCommand(
        PgProvider pgProvider,
        PaymentMethod paymentMethod,
        String orderId,
        Long memberId,
        String mId,
        String pgAuthKey,
        String pgTransactionId,
        Long requestAmount,
        AuthStatus authStatus,
        String authResultCode,
        String authResultMessage
) {
}
