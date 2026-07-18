package com.commercepayment.adapter.out.persistence;

import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.repository.PaymentCancelJpaRepository;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
