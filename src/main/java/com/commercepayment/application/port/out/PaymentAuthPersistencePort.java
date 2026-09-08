package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;

import java.util.Optional;

/**
 * PaymentAuth 영속성 처리를 위한 아웃바운드 포트
 */
public interface PaymentAuthPersistencePort {

    /**
     * 동일 PG사의 동일 인증키로 이미 처리된 인증이 있는지 확인한다
     */
    boolean existsByPgProviderAndPgAuthKey(PgProvider pgProvider, String pgAuthKey);

    /**
     * authId 로 인증 정보를 조회한다
     */
    Optional<PaymentAuth> findByAuthId(String authId);

    /**
     * 인증 정보를 저장한다
     */
    PaymentAuth save(PaymentAuth paymentAuth);
}
