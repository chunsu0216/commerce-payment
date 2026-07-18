package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentOutbox;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.port.out.PaymentOutboxPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 승인 플로우의 TX2(PG 승인 결과 반영 및 아웃박스 적재)를 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PaymentResultService {

    private static final String PAYMENT_RESULT_EVENT_TYPE = "PAYMENT_RESULT";

    private final PaymentPersistencePort paymentPersistencePort;
    private final PaymentOutboxPersistencePort paymentOutboxPersistencePort;
    private final PaymentEventPayloadFactory paymentEventPayloadFactory;

    /**
     * PG 승인 결과를 Payment 에 반영하고 결과 이벤트를 아웃박스에 적재한다
     */
    @Transactional
    public void applyApprovalResult(String paymentId, PgApprovalResult result) {
        Payment payment = findPayment(paymentId);
        applyResultToPayment(payment, result);
        publishPaymentResultEvent(payment);
    }

    /**
     * 승인 결과 상태에 따라 Payment 의 상태 전이 메서드를 호출한다
     */
    private void applyResultToPayment(Payment payment, PgApprovalResult result) {
        switch (result.status()) {
            case SUCCESS -> payment.success(result.pgTransactionId(), result.approvedAmount(), result.pgResultCode(), result.pgResultMessage());
            case FAILED -> payment.fail(result.pgResultCode(), result.pgResultMessage());
            case UNKNOWN -> payment.unknown(result.pgResultCode(), result.pgResultMessage());
        }
    }

    /**
     * Payment 의 현재 상태를 PAYMENT_RESULT 이벤트로 아웃박스에 적재한다
     */
    private void publishPaymentResultEvent(Payment payment) {
        String payload = paymentEventPayloadFactory.createPaymentResultPayload(payment);
        paymentOutboxPersistencePort.save(
                new PaymentOutbox(UUID.randomUUID().toString(), payment.getPaymentId(), PAYMENT_RESULT_EVENT_TYPE, payload)
        );
    }

    /**
     * paymentId 로 결제를 조회하고, 없으면 예외를 던진다
     */
    private Payment findPayment(String paymentId) {
        return paymentPersistencePort.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
