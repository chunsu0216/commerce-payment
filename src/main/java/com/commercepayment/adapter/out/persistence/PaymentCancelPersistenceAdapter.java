package com.commercepayment.adapter.out.persistence;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.repository.PaymentCancelJpaRepository;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * PaymentCancelPersistencePort 를 JPA 로 구현하는 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class PaymentCancelPersistenceAdapter implements PaymentCancelPersistencePort {

    private final PaymentCancelJpaRepository paymentCancelJpaRepository;

    /**
     * 결제 취소 정보를 저장한다
     */
    @Override
    public PaymentCancel save(PaymentCancel paymentCancel) {
        return paymentCancelJpaRepository.save(paymentCancel);
    }

    /**
     * cancelId 로 결제 취소 정보를 조회한다
     */
    @Override
    public Optional<PaymentCancel> findByCancelId(String cancelId) {
        return paymentCancelJpaRepository.findByCancelId(cancelId);
    }

    /**
     * 주어진 상태에 머물러 있으면서, updatedAt 이 기준 시각 이전이고 재시도 횟수가 상한 미만인 recovery 대상을 조회한다
     */
    @Override
    public List<PaymentCancel> findRecoveryTargets(Collection<CancelStatus> statuses, LocalDateTime updatedBefore, int maxRetryCount, int limit) {
        return paymentCancelJpaRepository.findByCancelStatusInAndUpdatedAtBeforeAndRetryCountLessThan(
                statuses, updatedBefore, maxRetryCount, PageRequest.of(0, limit)
        );
    }
}
