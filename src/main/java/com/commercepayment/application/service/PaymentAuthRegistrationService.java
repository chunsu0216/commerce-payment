package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentAuthRegistrationResult;
import com.commercepayment.application.port.out.PaymentAuthPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.DuplicatePaymentAuthException;
import com.commercepayment.common.exception.DuplicatePaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제 승인 플로우의 TX1(PaymentAuth/Payment 등록)을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PaymentAuthRegistrationService {

    private static final List<PaymentStatus> DUPLICATE_CHECK_STATUSES =
            List.of(PaymentStatus.PROCESSING, PaymentStatus.SUCCESS, PaymentStatus.UNKNOWN);

    private final PaymentAuthPersistencePort paymentAuthPersistencePort;
    private final PaymentPersistencePort paymentPersistencePort;

    /**
     * 인증 성공 콜백을 검증한 뒤 PaymentAuth 와 Payment(PROCESSING) 를 함께 등록한다
     */
    @Transactional
    public PaymentAuthRegistrationResult registerAuthAndPayment(PaymentApprovalCommand command) {
        validateNotDuplicateAuth(command.pgProvider(), command.pgAuthKey());
        PaymentAuth paymentAuth = saveAuth(command, AuthStatus.SUCCESS);

        validateNotDuplicatePayment(command.orderId(), command.memberId());
        Payment payment = new Payment(
                generateId(),
                paymentAuth.getAuthId(),
                command.orderId(),
                command.memberId(),
                command.paymentMethod(),
                command.pgProvider(),
                command.requestAmount()
        );
        paymentPersistencePort.save(payment);

        return new PaymentAuthRegistrationResult(payment.getPaymentId(), paymentAuth.getAuthId(), command.requestAmount());
    }

    /**
     * 인증 실패 콜백을 PaymentAuth 이력으로만 기록하고, Payment 생성과 PG 승인 호출은 생략한다
     */
    @Transactional
    public String registerFailedAuth(PaymentApprovalCommand command) {
        validateNotDuplicateAuth(command.pgProvider(), command.pgAuthKey());
        PaymentAuth paymentAuth = saveAuth(command, AuthStatus.FAILED);
        return paymentAuth.getAuthId();
    }

    /**
     * PaymentAuth 엔티티를 생성해 저장한다
     */
    private PaymentAuth saveAuth(PaymentApprovalCommand command, AuthStatus authStatus) {
        PaymentAuth paymentAuth = new PaymentAuth(
                generateId(),
                command.orderId(),
                command.memberId(),
                command.pgProvider(),
                command.paymentMethod(),
                command.pgAuthKey(),
                command.pgTransactionId(),
                authStatus,
                command.requestAmount(),
                command.authResultCode(),
                command.authResultMessage(),
                LocalDateTime.now()
        );
        return paymentAuthPersistencePort.save(paymentAuth);
    }

    /**
     * 동일 PG사/인증키로 이미 처리된 콜백인지 검증한다
     */
    private void validateNotDuplicateAuth(PgProvider pgProvider, String pgAuthKey) {
        if (paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(pgProvider, pgAuthKey)) {
            throw new DuplicatePaymentAuthException(pgAuthKey);
        }
    }

    /**
     * 동일 주문/회원에 이미 진행 중이거나 처리된 결제가 있는지 검증한다
     */
    private void validateNotDuplicatePayment(String orderId, Long memberId) {
        if (paymentPersistencePort.existsByOrderIdAndMemberIdAndPaymentStatusIn(orderId, memberId, DUPLICATE_CHECK_STATUSES)) {
            throw new DuplicatePaymentException(orderId, memberId);
        }
    }

    /**
     * UUID 기반 식별자를 생성한다
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
