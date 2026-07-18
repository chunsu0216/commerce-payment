package com.commercepayment.application.port.in;

import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentApprovalResult;

/**
 * 카드 결제 승인 콜백을 처리하는 인바운드 유스케이스
 */
public interface ProcessCardPaymentApprovalUseCase {

    /**
     * PG 콜백으로 전달된 결제 승인 요청을 처리한다
     */
    PaymentApprovalResult processApproval(PaymentApprovalCommand command);
}
