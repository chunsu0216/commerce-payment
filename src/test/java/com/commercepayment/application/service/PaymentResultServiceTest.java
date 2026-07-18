package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.port.out.PaymentOutboxPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.PaymentNotFoundException;
import com.commercepayment.domain.payment.PgApprovalStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentResultServiceTest {

    @Mock
    private PaymentPersistencePort paymentPersistencePort;

    @Mock
    private PaymentOutboxPersistencePort paymentOutboxPersistencePort;

    private PaymentResultService paymentResultService;

    @BeforeEach
    void setUp() {
        paymentResultService = new PaymentResultService(
                paymentPersistencePort,
                paymentOutboxPersistencePort,
                new PaymentEventPayloadFactory(new ObjectMapper().registerModule(new JavaTimeModule()))
        );
    }

    @Test
    void 승인_성공이면_Payment_가_SUCCESS_로_전이되고_아웃박스가_적재된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));

        PgApprovalResult result = new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now());

        paymentResultService.applyApprovalResult("payment-1", result);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getApprovedAmount()).isEqualTo(10000L);
        verify(paymentOutboxPersistencePort).save(any());
    }

    @Test
    void 승인_실패면_Payment_가_FAILED_로_전이된다() {
        Payment payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));

        PgApprovalResult result = new PgApprovalResult(PgApprovalStatus.FAILED, null, 0L, "9999", "승인 거절", null);

        paymentResultService.applyApprovalResult("payment-1", result);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentOutboxPersistencePort).save(any());
    }

    @Test
    void 결제_정보가_없으면_예외가_발생한다() {
        when(paymentPersistencePort.findByPaymentId(anyString())).thenReturn(Optional.empty());

        PgApprovalResult result = new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now());

        assertThatThrownBy(() -> paymentResultService.applyApprovalResult("payment-1", result))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
