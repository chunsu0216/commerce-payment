package com.commercepayment.adapter.out.persistence;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.repository.PaymentJpaRepository;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.DuplicatePaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * PaymentPersistencePort 를 JPA 로 구현하는 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentJpaRepository paymentJpaRepository;

    /**
     * 주어진 상태들 중 하나에 해당하는 결제가 동일 주문/회원에 이미 존재하는지 확인한다
     */
    @Override
    public boolean existsByOrderIdAndMemberIdAndPaymentStatusIn(String orderId, Long memberId, Collection<PaymentStatus> statuses) {
        return paymentJpaRepository.existsByOrderIdAndMemberIdAndPaymentStatusIn(orderId, memberId, statuses);
    }

    /**
     * paymentId 로 결제를 조회한다
     */
    @Override
    public Optional<Payment> findByPaymentId(String paymentId) {
        return paymentJpaRepository.findByPaymentId(paymentId);
    }

    /**
     * 결제 정보를 저장하고, 유니크 제약 위반 시 중복 결제 예외로 변환한다(3차 방어선)
     */
    @Override
    public Payment save(Payment payment) {
        try {
            return paymentJpaRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePaymentException(payment.getOrderId(), payment.getMemberId());
        }
    }
}
