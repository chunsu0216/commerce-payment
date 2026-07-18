package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentApprovalResult;
import com.commercepayment.application.dto.PaymentAuthRegistrationResult;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.port.out.DistributedLockPort;
import com.commercepayment.application.port.out.PgApprovalPort;
import com.commercepayment.application.port.out.PgCancelPort;
import com.commercepayment.common.exception.PaymentCompensatedException;
import com.commercepayment.config.PaymentLockProperties;
import com.commercepayment.domain.payment.PgApprovalStatus;
import com.commercepayment.domain.payment.PgCancelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPaymentApprovalFacadeServiceTest {

    @Mock
    private DistributedLockPort distributedLockPort;

    @Mock
    private PaymentAuthRegistrationService paymentAuthRegistrationService;

    @Mock
    private PaymentResultService paymentResultService;

    @Mock
    private PaymentCompensationService paymentCompensationService;

    @Mock
    private PgApprovalPort pgApprovalPort;

    @Mock
    private PgCancelPort pgCancelPort;

    private CardPaymentApprovalFacadeService facade;

    private PaymentApprovalCommand successAuthCommand;

    @BeforeEach
    void setUp() {
        PaymentLockProperties lockProperties = new PaymentLockProperties();
        lockProperties.setWaitSeconds(3);

        facade = new CardPaymentApprovalFacadeService(
                distributedLockPort,
                lockProperties,
                paymentAuthRegistrationService,
                paymentResultService,
                paymentCompensationService,
                pgApprovalPort,
                pgCancelPort
        );

        successAuthCommand = new PaymentApprovalCommand(
                PgProvider.INICIS, PaymentMethod.CARD, "order-1", 100L, "mid-1", "auth-tid-1",
                null, 10000L, AuthStatus.SUCCESS, "00", "인증성공"
        );

        when(distributedLockPort.executeWithLock(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    DistributedLockPort.LockCallback<?> callback = invocation.getArgument(2);
                    return callback.call();
                });
    }

    @Test
    void 인증_실패_콜백은_Payment_생성과_PG_승인_호출_없이_즉시_실패_응답을_반환한다() {
        PaymentApprovalCommand failedCommand = new PaymentApprovalCommand(
                PgProvider.INICIS, PaymentMethod.CARD, "order-1", 100L, "mid-1", "auth-tid-1",
                null, 10000L, AuthStatus.FAILED, "01", "인증실패"
        );
        when(paymentAuthRegistrationService.registerFailedAuth(failedCommand)).thenReturn("auth-1");

        PaymentApprovalResult result = facade.processApproval(failedCommand);

        assertThat(result.success()).isFalse();
        assertThat(result.paymentId()).isNull();
        assertThat(result.authId()).isEqualTo("auth-1");
        verify(paymentAuthRegistrationService, never()).registerAuthAndPayment(any());
        verify(pgApprovalPort, never()).approve(any(), any());
    }

    @Test
    void 승인_성공이면_TX1_TX2_를_거쳐_성공_결과를_반환한다() {
        when(paymentAuthRegistrationService.registerAuthAndPayment(successAuthCommand))
                .thenReturn(new PaymentAuthRegistrationResult("payment-1", "auth-1", 10000L));
        when(pgApprovalPort.approve(any(), any()))
                .thenReturn(new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now()));

        PaymentApprovalResult result = facade.processApproval(successAuthCommand);

        assertThat(result.success()).isTrue();
        assertThat(result.paymentId()).isEqualTo("payment-1");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentResultService).applyApprovalResult(eq("payment-1"), any());
        verify(paymentCompensationService, never()).recordCompensation(any(), any(), any());
    }

    @Test
    void TX2_가_실패하면_PG_망취소와_보상처리를_수행하고_보상완료_예외를_던진다() {
        when(paymentAuthRegistrationService.registerAuthAndPayment(successAuthCommand))
                .thenReturn(new PaymentAuthRegistrationResult("payment-1", "auth-1", 10000L));
        when(pgApprovalPort.approve(any(), any()))
                .thenReturn(new PgApprovalResult(PgApprovalStatus.SUCCESS, "pg-tx-1", 10000L, "0000", "정상 승인", LocalDateTime.now()));
        doThrow(new RuntimeException("DB 커밋 실패"))
                .when(paymentResultService).applyApprovalResult(anyString(), any());
        when(pgCancelPort.cancel(any(), any()))
                .thenReturn(new PgCancelResult(PgCancelStatus.SUCCESS, "pg-cancel-1", "0000", "정상 취소"));

        assertThatThrownBy(() -> facade.processApproval(successAuthCommand))
                .isInstanceOf(PaymentCompensatedException.class);

        verify(pgCancelPort).cancel(any(), any());
        verify(paymentCompensationService).recordCompensation(anyString(), any(), any());
    }

    @Test
    void PG_승인_통신_실패시_UNKNOWN_으로_처리되어_TX2_까지는_정상_진행된다() {
        when(paymentAuthRegistrationService.registerAuthAndPayment(successAuthCommand))
                .thenReturn(new PaymentAuthRegistrationResult("payment-1", "auth-1", 10000L));
        when(pgApprovalPort.approve(any(), any())).thenThrow(new RuntimeException("타임아웃"));

        PaymentApprovalResult result = facade.processApproval(successAuthCommand);

        assertThat(result.success()).isFalse();
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        verify(paymentResultService).applyApprovalResult(eq("payment-1"), any());
    }
}
