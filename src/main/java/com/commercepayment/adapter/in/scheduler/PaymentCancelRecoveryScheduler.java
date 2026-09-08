package com.commercepayment.adapter.in.scheduler;

import com.commercepayment.application.port.in.RecoverPendingPaymentCancelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING/UNKNOWN 상태로 남아있는 PaymentCancel 을 주기적으로 복구하는 스케줄러
 */
@Component
@RequiredArgsConstructor
public class PaymentCancelRecoveryScheduler {

    private final RecoverPendingPaymentCancelUseCase recoverPendingPaymentCancelUseCase;

    /**
     * 설정된 주기마다 PaymentCancel 복구 유스케이스를 실행한다
     */
    @Scheduled(fixedDelayString = "${payment-cancel-recovery.fixed-delay-ms}")
    public void recoverPendingPaymentCancels() {
        recoverPendingPaymentCancelUseCase.recoverPendingCancels();
    }
}
