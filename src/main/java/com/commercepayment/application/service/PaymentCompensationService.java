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
import com.commercepayment.common.exception.PaymentCancelNotFoundException;
import com.commercepayment.common.exception.PaymentNotFoundException;
import com.commercepayment.domain.payment.PgApprovalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 승인 플로우의 TX2 실패에 대한 보상 취소를 담당하는 서비스.
 * "PG 망취소 호출 전 PENDING 선커밋"과 "조회/취소 결과 최종 확정"을 별도 트랜잭션으로 분리해,
 * PG 호출 도중 앱이 죽어도 recovery scheduler 가 PaymentCancel 레코드를 이어받아 재처리할 수 있게 한다.
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
     * TX2 에서 롤백된 원래 승인 결과를 Payment 에 재반영하고, PG 망취소 호출 전에 PaymentCancel(PROCESSING) 을 먼저 커밋한다.
     * 이 시점에는 아직 PG 호출 결과를 모르므로 Payment 를 취소 상태로 확정하지 않고, 아웃박스 이벤트도 발행하지 않는다.
     */
    @Transactional
    public PaymentCancel recordPendingCancel(String paymentId, PgApprovalResult originalResult) {
        Payment payment = findPayment(paymentId);
        reapplyOriginalResult(payment, originalResult);

        boolean wasApproved = originalResult.status() == PgApprovalStatus.SUCCESS;
        Long cancelAmount = wasApproved ? originalResult.approvedAmount() : 0L;
        PaymentCancel paymentCancel = new PaymentCancel(
                UUID.randomUUID().toString(),
                payment.getPaymentId(),
                UUID.randomUUID().toString(),
                CancelType.COMPENSATION,
                cancelAmount,
                COMPENSATION_REASON
        );
        return paymentCancelPersistencePort.save(paymentCancel);
    }

    /**
     * PG 조회/망취소 결과를 PaymentCancel 에 최종 반영한다.
     * PG 기준 정상 취소가 확정된 경우(SUCCESS)에만 Payment 를 취소 상태로 전이시키고 결과 이벤트를 발행한다.
     * FAILED(취소 실패 확정)에도 결과 이벤트를 발행해 원래 승인 결과를 다운스트림에 알리며,
     * UNKNOWN(판단 불가)은 재시도 대상으로 남기고 이벤트를 발행하지 않는다.
     */
    @Transactional
    public void applyFinalCancelResult(String cancelId, PgCancelResult cancelResult) {
        PaymentCancel paymentCancel = findPaymentCancel(cancelId);

        switch (cancelResult.status()) {
            case SUCCESS -> {
                paymentCancel.success(cancelResult.pgCancelId(), cancelResult.pgResultCode(), cancelResult.pgResultMessage());
                Payment payment = findPayment(paymentCancel.getPaymentId());
                payment.cancel(paymentCancel.getCancelAmount());
                publishPaymentResultEvent(payment);
            }
            case FAILED -> {
                paymentCancel.fail(cancelResult.pgResultCode(), cancelResult.pgResultMessage());
                publishPaymentResultEvent(findPayment(paymentCancel.getPaymentId()));
            }
            case UNKNOWN -> {
                paymentCancel.unknown(cancelResult.pgResultCode(), cancelResult.pgResultMessage());
                paymentCancel.increaseRetryCount();
            }
        }
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

    /**
     * cancelId 로 결제 취소 정보를 조회하고, 없으면 예외를 던진다
     */
    private PaymentCancel findPaymentCancel(String cancelId) {
        return paymentCancelPersistencePort.findByCancelId(cancelId)
                .orElseThrow(() -> new PaymentCancelNotFoundException(cancelId));
    }
}
