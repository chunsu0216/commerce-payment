package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.application.port.in.RecoverPendingPaymentCancelUseCase;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.config.PaymentCancelRecoveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PENDING(PROCESSING)/UNKNOWN 상태로 오래 머물러 있는 PaymentCancel 을 조회해 재처리하는 서비스.
 * 개별 건은 PaymentCancelExecutionService 의 cancelId 기준 분산락을 통해 최초 보상 흐름과 상호 배제되며,
 * 한 건의 처리 실패가 나머지 건 처리를 막지 않도록 예외를 건별로 격리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelRecoveryService implements RecoverPendingPaymentCancelUseCase {

    private static final Set<CancelStatus> RECOVERY_TARGET_STATUSES = Set.of(CancelStatus.PROCESSING, CancelStatus.UNKNOWN);

    private final PaymentCancelPersistencePort paymentCancelPersistencePort;
    private final PaymentCancelExecutionService paymentCancelExecutionService;
    private final PaymentCancelRecoveryProperties paymentCancelRecoveryProperties;

    /**
     * 복구 대상 PaymentCancel 들을 조회해 한 건씩 재처리하고, 개별 실패는 로그만 남기고 다음 건을 계속 처리한다
     */
    @Override
    public void recoverPendingCancels() {
        List<PaymentCancel> targets = findRecoveryTargets();
        for (PaymentCancel target : targets) {
            recoverOne(target);
        }
    }

    /**
     * 상태 정지 기준 시각과 재시도 상한을 적용해 복구 대상을 조회한다
     */
    private List<PaymentCancel> findRecoveryTargets() {
        LocalDateTime updatedBefore = LocalDateTime.now().minusSeconds(paymentCancelRecoveryProperties.getStaleSeconds());
        return paymentCancelPersistencePort.findRecoveryTargets(
                RECOVERY_TARGET_STATUSES,
                updatedBefore,
                paymentCancelRecoveryProperties.getMaxRetryCount(),
                paymentCancelRecoveryProperties.getBatchSize()
        );
    }

    /**
     * 한 건의 PaymentCancel 을 재처리하고, 실패해도 예외를 전파하지 않고 로그만 남긴다
     */
    private void recoverOne(PaymentCancel target) {
        try {
            paymentCancelExecutionService.executeCancel(target.getCancelId());
        } catch (Exception e) {
            log.error("PaymentCancel 복구 처리에 실패했습니다. cancelId={}", target.getCancelId(), e);
        }
    }
}
