package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.CancelType;
import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.application.port.out.PaymentOutboxPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.domain.payment.PgApprovalStatus;
import com.commercepayment.domain.payment.PgCancelStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationServiceTest {

    @Mock
    private PaymentPersistencePort paymentPersistencePort;

    @Mock
    private PaymentCancelPersistencePort paymentCancelPersistencePort;

    @Mock
    private PaymentOutboxPersistencePort paymentOutboxPersistencePort;

    private PaymentCompensationService paymentCompensationService;

    @BeforeEach
    void setUp() {
        paymentCompensationService = new PaymentCompensationService(
                paymentPersistencePort,
                paymentCancelPersistencePort,
                paymentOutboxPersistencePort,
                new PaymentEventPayloadFactory(new ObjectMapper().registerModule(new JavaTimeModule()))
        );
    }

    @Test
    void 원래_승인이_성공이었으면_PENDING_상태의_PaymentCancel_을_먼저_커밋하고_Payment_는_아직_취소되지_않는다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentCancelPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PgApprovalResult originalResult = new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now());

        PaymentCancel pendingCancel = paymentCompensationService.recordPendingCancel("payment-1", originalResult);

        // approvedAmount 는 복구되지만, 아직 PG 취소 결과를 모르므로 Payment 는 취소 상태로 전이되지 않는다
        assertThat(payment.getApprovedAmount()).isEqualTo(10000L);
        assertThat(payment.getCancelledAmount()).isZero();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(pendingCancel.getCancelType()).isEqualTo(CancelType.COMPENSATION);
        assertThat(pendingCancel.getCancelAmount()).isEqualTo(10000L);
        assertThat(pendingCancel.getCancelStatus()).isEqualTo(CancelStatus.PROCESSING);

        verify(paymentOutboxPersistencePort, never()).save(any());
    }

    @Test
    void 원래_승인이_실패였으면_취소금액_없이_PENDING_상태로만_커밋된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentCancelPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PgApprovalResult originalResult = new PgApprovalResult(PgApprovalStatus.FAILED, null, 0L, "9999", "승인 거절", null);

        PaymentCancel pendingCancel = paymentCompensationService.recordPendingCancel("payment-1", originalResult);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pendingCancel.getCancelAmount()).isEqualTo(0L);
    }

    @Test
    void 망취소_결과가_SUCCESS_이면_PaymentCancel_과_Payment_가_취소로_확정되고_이벤트가_발행된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        payment.success("pg-tx-1", 10000L, "0000", "정상 승인");
        PaymentCancel paymentCancel = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(paymentCancel));

        PgCancelResult cancelResult = new PgCancelResult(PgCancelStatus.SUCCESS, "pg-cancel-1", "0000", "정상 취소");
        paymentCompensationService.applyFinalCancelResult("cancel-1", cancelResult);

        assertThat(paymentCancel.getCancelStatus()).isEqualTo(CancelStatus.SUCCESS);
        assertThat(payment.getCancelledAmount()).isEqualTo(10000L);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentOutboxPersistencePort).save(any());
    }

    @Test
    void 망취소_결과가_FAILED_이면_PaymentCancel_만_실패로_확정되고_Payment_는_취소되지_않은_채_이벤트가_발행된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        payment.success("pg-tx-1", 10000L, "0000", "정상 승인");
        PaymentCancel paymentCancel = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(paymentCancel));

        PgCancelResult cancelResult = new PgCancelResult(PgCancelStatus.FAILED, null, "9999", "취소 거절");
        paymentCompensationService.applyFinalCancelResult("cancel-1", cancelResult);

        assertThat(paymentCancel.getCancelStatus()).isEqualTo(CancelStatus.FAILED);
        assertThat(payment.getCancelledAmount()).isZero();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentOutboxPersistencePort).save(any());
    }

    @Test
    void 망취소_결과가_UNKNOWN_이면_재시도_횟수만_증가하고_이벤트는_발행되지_않는다() {
        PaymentCancel paymentCancel = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(paymentCancel));

        PgCancelResult cancelResult = new PgCancelResult(PgCancelStatus.UNKNOWN, null, "TIMEOUT", "응답 없음");
        paymentCompensationService.applyFinalCancelResult("cancel-1", cancelResult);

        assertThat(paymentCancel.getCancelStatus()).isEqualTo(CancelStatus.UNKNOWN);
        assertThat(paymentCancel.getRetryCount()).isEqualTo(1);
        verify(paymentPersistencePort, never()).findByPaymentId(any());
        verify(paymentOutboxPersistencePort, never()).save(any());
    }
}
