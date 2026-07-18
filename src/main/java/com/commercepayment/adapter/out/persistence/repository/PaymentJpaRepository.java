package com.commercepayment.adapter.out.persistence.repository;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

/**
 * Payment 엔티티에 대한 Spring Data JPA 저장소
 */
public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    boolean existsByOrderIdAndMemberIdAndPaymentStatusIn(String orderId, Long memberId, Collection<PaymentStatus> statuses);
}
