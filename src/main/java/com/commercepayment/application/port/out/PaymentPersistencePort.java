package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * Payment 영속성 처리를 위한 아웃바운드 포트
 */
public interface PaymentPersistencePort {

    /**
     * 주어진 상태들 중 하나에 해당하는 결제가 동일 주문/회원에 이미 존재하는지 확인한다
     */
    boolean existsByOrderIdAndMemberIdAndPaymentStatusIn(String orderId, Long memberId, Collection<PaymentStatus> statuses);

    /**
     * paymentId 로 결제를 조회한다
     */
    Optional<Payment> findByPaymentId(String paymentId);

    /**
     * 결제 정보를 저장한다
     */
    Payment save(Payment payment);
}
