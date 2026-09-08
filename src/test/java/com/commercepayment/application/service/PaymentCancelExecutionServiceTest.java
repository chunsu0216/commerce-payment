package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.CancelType;
import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.dto.PgInquiryResult;
import com.commercepayment.application.port.out.DistributedLockPort;
import com.commercepayment.application.port.out.PaymentAuthPersistencePort;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.application.port.out.PgCancelPort;
import com.commercepayment.application.port.out.PgInquiryPort;
import com.commercepayment.config.PaymentLockProperties;
import com.commercepayment.domain.payment.PgCancelStatus;
import com.commercepayment.domain.payment.PgInquiryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentCancelExecutionServiceTest {

    @Mock
    private DistributedLockPort distributedLockPort;

    @Mock
    private PaymentCancelPersistencePort paymentCancelPersistencePort;

    @Mock
    private PaymentPersistencePort paymentPersistencePort;

    @Mock
    private PaymentAuthPersistencePort paymentAuthPersistencePort;

    @Mock
    private PgInquiryPort pgInquiryPort;

    @Mock
    private PgCancelPort pgCancelPort;

    @Mock
    private PaymentCompensationService paymentCompensationService;

    private PaymentCancelExecutionService service;

    private PaymentCancel pendingCancel;
    private Payment payment;
    private PaymentAuth paymentAuth;

    @BeforeEach
    void setUp() {
        PaymentLockProperties lockProperties = new PaymentLockProperties();
        lockProperties.setWaitSeconds(3);

        service = new PaymentCancelExecutionService(
                distributedLockPort, lockProperties, paymentCancelPersistencePort, paymentPersistencePort,
                paymentAuthPersistencePort, pgInquiryPort, pgCancelPort, paymentCompensationService
        );

        pendingCancel = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        payment = new Payment("payment-1", "auth-1", "order-1", 100L, PaymentMethod.CARD, PgProvider.INICIS, 10000L);
        paymentAuth = new PaymentAuth(
                "auth-1", "order-1", 100L, PgProvider.INICIS, PaymentMethod.CARD,
                "mid-1", "auth-key-1", "auth-tid-1", AuthStatus.SUCCESS, 10000L, "00", "인증성공", LocalDateTime.now()
        );

        when(distributedLockPort.executeWithLock(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    DistributedLockPort.LockCallback<?> callback = invocation.getArgument(2);
                    return callback.call();
                });
    }

    @Test
    void PG_기준_이미_취소되어있으면_망취소_API_를_호출하지_않고_SUCCESS_로_확정한다() {
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(pendingCancel));
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentAuthPersistencePort.findByAuthId("auth-1")).thenReturn(Optional.of(paymentAuth));
        when(pgInquiryPort.inquire(any(), any()))
                .thenReturn(new PgInquiryResult(PgInquiryStatus.CANCELLED, "pg-tx-1", "0000", "이미 취소됨"));

        service.executeCancel("cancel-1");

        verify(pgCancelPort, never()).cancel(any(), any());
        ArgumentCaptor<PgCancelResult> captor = ArgumentCaptor.forClass(PgCancelResult.class);
        verify(paymentCompensationService).applyFinalCancelResult(anyString(), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PgCancelStatus.SUCCESS);
    }

    @Test
    void PG_기준_아직_승인_유지중이면_망취소_API_를_호출하고_그_결과로_확정한다() {
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(pendingCancel));
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentAuthPersistencePort.findByAuthId("auth-1")).thenReturn(Optional.of(paymentAuth));
        when(pgInquiryPort.inquire(any(), any()))
                .thenReturn(new PgInquiryResult(PgInquiryStatus.APPROVED, "pg-tx-1", "0000", "승인 유지"));
        when(pgCancelPort.cancel(any(), any()))
                .thenReturn(new PgCancelResult(PgCancelStatus.SUCCESS, "pg-cancel-1", "0000", "정상 취소"));

        service.executeCancel("cancel-1");

        verify(pgCancelPort).cancel(any(), any());
        verify(paymentCompensationService).applyFinalCancelResult(eq("cancel-1"), any());
    }

    @Test
    void 조회_API_통신에_실패하면_망취소_API_를_호출하지_않고_UNKNOWN_으로_확정한다() {
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(pendingCancel));
        when(paymentPersistencePort.findByPaymentId("payment-1")).thenReturn(Optional.of(payment));
        when(paymentAuthPersistencePort.findByAuthId("auth-1")).thenReturn(Optional.of(paymentAuth));
        when(pgInquiryPort.inquire(any(), any())).thenThrow(new RuntimeException("타임아웃"));

        service.executeCancel("cancel-1");

        verify(pgCancelPort, never()).cancel(any(), any());
        ArgumentCaptor<PgCancelResult> captor = ArgumentCaptor.forClass(PgCancelResult.class);
        verify(paymentCompensationService).applyFinalCancelResult(anyString(), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PgCancelStatus.UNKNOWN);
    }

    @Test
    void 이미_SUCCESS_로_확정된_건은_재조회_없이_스킵한다() {
        pendingCancel.success("pg-cancel-1", "0000", "정상 취소");
        when(paymentCancelPersistencePort.findByCancelId("cancel-1")).thenReturn(Optional.of(pendingCancel));

        service.executeCancel("cancel-1");

        verify(pgInquiryPort, never()).inquire(any(), any());
        verify(pgCancelPort, never()).cancel(any(), any());
        verify(paymentCompensationService, never()).applyFinalCancelResult(any(), any());
    }
}
