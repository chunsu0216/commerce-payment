package com.commercepayment.adapter.out.persistence;

import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.adapter.out.persistence.repository.PaymentAuthJpaRepository;
import com.commercepayment.application.port.out.PaymentAuthPersistencePort;
import com.commercepayment.common.exception.DuplicatePaymentAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PaymentAuthPersistencePort 를 JPA 로 구현하는 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class PaymentAuthPersistenceAdapter implements PaymentAuthPersistencePort {

    private final PaymentAuthJpaRepository paymentAuthJpaRepository;

    /**
     * 동일 PG사의 동일 인증키로 이미 처리된 인증이 있는지 확인한다
     */
    @Override
    public boolean existsByPgProviderAndPgAuthKey(PgProvider pgProvider, String pgAuthKey) {
        return paymentAuthJpaRepository.existsByPgProviderAndPgAuthKey(pgProvider, pgAuthKey);
    }

    /**
     * authId 로 인증 정보를 조회한다
     */
    @Override
    public Optional<PaymentAuth> findByAuthId(String authId) {
        return paymentAuthJpaRepository.findByAuthId(authId);
    }

    /**
     * 인증 정보를 저장하고, 유니크 제약 위반 시 중복 인증 예외로 변환한다(3차 방어선)
     */
    @Override
    public PaymentAuth save(PaymentAuth paymentAuth) {
        try {
            return paymentAuthJpaRepository.save(paymentAuth);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePaymentAuthException(paymentAuth.getPgAuthKey());
        }
    }
}
