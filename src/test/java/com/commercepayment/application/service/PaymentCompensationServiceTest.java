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
    void 원래_승인이_성공이었으면_approvedAmount_를_복구한_뒤_전액_취소로_확정된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));

        PgApprovalResult originalResult = new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now());
        PgCancelResult cancelResult = new PgCancelResult(PgCancelStatus.SUCCESS, "pg-cancel-1", "0000", "정상 취소");

        paymentCompensationService.recordCompensation("payment-1", originalResult, cancelResult);

        // approvedAmount 가 먼저 복구된 뒤 cancel() 이 호출되어야 cancelledAmount == approvedAmount 가 되어 CANCELLED 로 전이된다
        assertThat(payment.getApprovedAmount()).isEqualTo(10000L);
        assertThat(payment.getCancelledAmount()).isEqualTo(10000L);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);

        ArgumentCaptor<PaymentCancel> captor = ArgumentCaptor.forClass(PaymentCancel.class);
        verify(paymentCancelPersistencePort).save(captor.capture());
        PaymentCancel savedCancel = captor.getValue();
        assertThat(savedCancel.getCancelType()).isEqualTo(CancelType.COMPENSATION);
        assertThat(savedCancel.getCancelAmount()).isEqualTo(10000L);
        assertThat(savedCancel.getCancelStatus()).isEqualTo(CancelStatus.SUCCESS);

        verify(paymentOutboxPersistencePort).save(any());
    }

    @Test
    void 원래_승인이_실패였으면_취소금액_없이_실패_상태로만_재확정된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));

        PgApprovalResult originalResult = new PgApprovalResult(PgApprovalStatus.FAILED, null, 0L, "9999", "승인 거절", null);
        PgCancelResult cancelResult = new PgCancelResult(PgCancelStatus.SUCCESS, "pg-cancel-1", "0000", "정상 취소");

        paymentCompensationService.recordCompensation("payment-1", originalResult, cancelResult);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCancelledAmount()).isZero();

        ArgumentCaptor<PaymentCancel> captor = ArgumentCaptor.forClass(PaymentCancel.class);
        verify(paymentCancelPersistencePort).save(captor.capture());
        assertThat(captor.getValue().getCancelAmount()).isEqualTo(0L);
    }
}
