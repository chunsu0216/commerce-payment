package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PaymentOutbox;

/**
 * PaymentOutbox 영속성 처리를 위한 아웃바운드 포트
 */
public interface PaymentOutboxPersistencePort {

    /**
     * 아웃박스 이벤트를 저장한다
     */
    PaymentOutbox save(PaymentOutbox paymentOutbox);
}
