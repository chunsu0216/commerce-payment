package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;

/**
 * PaymentCancel 영속성 처리를 위한 아웃바운드 포트
 */
public interface PaymentCancelPersistencePort {

    /**
     * 결제 취소 정보를 저장한다
     */
    PaymentCancel save(PaymentCancel paymentCancel);
}
