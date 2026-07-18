package com.commercepayment.adapter.out.persistence.repository;

import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PaymentCancel 엔티티에 대한 Spring Data JPA 저장소
 */
public interface PaymentCancelJpaRepository extends JpaRepository<PaymentCancel, Long> {
}
