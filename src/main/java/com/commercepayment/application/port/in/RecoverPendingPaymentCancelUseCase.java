package com.commercepayment.application.port.in;

/**
 * PENDING/UNKNOWN 상태로 남아있는 PaymentCancel 을 복구(재처리)하는 인바운드 유스케이스
 */
public interface RecoverPendingPaymentCancelUseCase {

    /**
     * 복구 대상 PaymentCancel 들을 조회해 재처리한다
     */
    void recoverPendingCancels();
}
