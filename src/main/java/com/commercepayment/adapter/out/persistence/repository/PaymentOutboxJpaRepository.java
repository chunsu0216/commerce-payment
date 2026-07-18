package com.commercepayment.adapter.out.persistence.repository;

import com.commercepayment.adapter.out.persistence.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PaymentOutbox 엔티티에 대한 Spring Data JPA 저장소
 */
public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutbox, Long> {
}
