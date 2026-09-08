package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * PaymentCancel 영속성 처리를 위한 아웃바운드 포트
 */
public interface PaymentCancelPersistencePort {

    /**
     * 결제 취소 정보를 저장한다
     */
    PaymentCancel save(PaymentCancel paymentCancel);

    /**
     * cancelId 로 결제 취소 정보를 조회한다
     */
    Optional<PaymentCancel> findByCancelId(String cancelId);

    /**
     * 주어진 상태에 머물러 있으면서, updatedAt 이 기준 시각 이전이고 재시도 횟수가 상한 미만인 recovery 대상을 조회한다
     */
    List<PaymentCancel> findRecoveryTargets(Collection<CancelStatus> statuses, LocalDateTime updatedBefore, int maxRetryCount, int limit);
}
