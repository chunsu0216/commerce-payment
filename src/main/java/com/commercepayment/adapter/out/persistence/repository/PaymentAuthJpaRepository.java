package com.commercepayment.adapter.out.persistence.repository;

import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PaymentAuth 엔티티에 대한 Spring Data JPA 저장소
 */
public interface PaymentAuthJpaRepository extends JpaRepository<PaymentAuth, Long> {

    boolean existsByPgProviderAndPgAuthKey(PgProvider pgProvider, String pgAuthKey);
}
