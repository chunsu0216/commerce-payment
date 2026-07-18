package com.commercepayment.adapter.out.persistence;

import com.commercepayment.adapter.out.persistence.entity.PaymentOutbox;
import com.commercepayment.adapter.out.persistence.repository.PaymentOutboxJpaRepository;
import com.commercepayment.application.port.out.PaymentOutboxPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PaymentOutboxPersistencePort 를 JPA 로 구현하는 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class PaymentOutboxPersistenceAdapter implements PaymentOutboxPersistencePort {

    private final PaymentOutboxJpaRepository paymentOutboxJpaRepository;

    /**
     * 아웃박스 이벤트를 저장한다
     */
    @Override
    public PaymentOutbox save(PaymentOutbox paymentOutbox) {
        return paymentOutboxJpaRepository.save(paymentOutbox);
    }
}
