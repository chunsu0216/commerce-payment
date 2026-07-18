package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.CancelType;
import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.entity.PaymentOutbox;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.application.port.out.PaymentOutboxPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.PaymentNotFoundException;
import com.commercepayment.domain.payment.PgApprovalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 승인 플로우의 TX3(TX2 실패에 대한 보상 처리)를 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private static final String PAYMENT_RESULT_EVENT_TYPE = "PAYMENT_RESULT";
    private static final String COMPENSATION_REASON = "TX2 결제 결과 반영 실패에 대한 보상 취소";

    private final PaymentPersistencePort paymentPersistencePort;
    private final PaymentCancelPersistencePort paymentCancelPersistencePort;
    private final PaymentOutboxPersistencePort paymentOutboxPersistencePort;
    private final PaymentEventPayloadFactory paymentEventPayloadFactory;

    /**
     * TX2 에서 롤백된 원래 승인 결과를 재적용하고, PG 망취소 결과를 PaymentCancel(COMPENSATION)로 기록한 뒤
     * 최종 결과를 아웃박스에 적재한다
     */
    @Transactional
    public void recordCompensation(String paymentId, PgApprovalResult originalResult, PgCancelResult cancelResult) {
        Payment payment = findPayment(paymentId);

        reapplyOriginalResult(payment, originalResult);
        boolean wasApproved = originalResult.status() == PgApprovalStatus.SUCCESS;
        if (wasApproved) {
            payment.cancel(originalResult.approvedAmount());
        }

        PaymentCancel paymentCancel = createPaymentCancel(payment, originalResult, wasApproved);
        applyCancelResult(paymentCancel, cancelResult);
        paymentCancelPersistencePort.save(paymentCancel);

        publishPaymentResultEvent(payment);
    }

    /**
     * TX2 에서 시도했으나 롤백된 원래 승인 결과를 Payment 에 다시 반영한다
     */
    private void reapplyOriginalResult(Payment payment, PgApprovalResult result) {
        switch (result.status()) {
            case SUCCESS -> payment.success(result.pgTransactionId(), result.approvedAmount(), result.pgResultCode(), result.pgResultMessage());
            case FAILED -> payment.fail(result.pgResultCode(), result.pgResultMessage());
            case UNKNOWN -> payment.unknown(result.pgResultCode(), result.pgResultMessage());
        }
    }

    /**
     * 보상 취소 이력을 표현하는 PaymentCancel 엔티티를 생성한다
     */
    private PaymentCancel createPaymentCancel(Payment payment, PgApprovalResult originalResult, boolean wasApproved) {
        Long cancelAmount = wasApproved ? originalResult.approvedAmount() : 0L;
        return new PaymentCancel(
                UUID.randomUUID().toString(),
                payment.getPaymentId(),
                UUID.randomUUID().toString(),
                CancelType.COMPENSATION,
                cancelAmount,
                COMPENSATION_REASON
        );
    }

    /**
     * PG 망취소 호출 결과를 PaymentCancel 상태에 반영한다
     */
    private void applyCancelResult(PaymentCancel paymentCancel, PgCancelResult cancelResult) {
        switch (cancelResult.status()) {
            case SUCCESS -> paymentCancel.success(cancelResult.pgCancelId(), cancelResult.pgResultCode(), cancelResult.pgResultMessage());
            case FAILED -> paymentCancel.fail(cancelResult.pgResultCode(), cancelResult.pgResultMessage());
            case UNKNOWN -> paymentCancel.unknown(cancelResult.pgResultCode(), cancelResult.pgResultMessage());
        }
    }

    /**
     * Payment 의 최종 상태를 PAYMENT_RESULT 이벤트로 아웃박스에 적재한다
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
