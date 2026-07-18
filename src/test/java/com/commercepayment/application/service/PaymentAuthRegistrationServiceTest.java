package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentAuthRegistrationResult;
import com.commercepayment.application.port.out.PaymentAuthPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.common.exception.DuplicatePaymentAuthException;
import com.commercepayment.common.exception.DuplicatePaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAuthRegistrationServiceTest {

    @Mock
    private PaymentAuthPersistencePort paymentAuthPersistencePort;

    @Mock
    private PaymentPersistencePort paymentPersistencePort;

    private PaymentAuthRegistrationService paymentAuthRegistrationService;

    private PaymentApprovalCommand successCommand;

    @BeforeEach
    void setUp() {
        paymentAuthRegistrationService = new PaymentAuthRegistrationService(paymentAuthPersistencePort, paymentPersistencePort);
        successCommand = new PaymentApprovalCommand(
                PgProvider.INICIS, PaymentMethod.CARD, "order-1", 100L, "mid-1", "auth-tid-1",
                null, 10000L, AuthStatus.SUCCESS, "00", "인증성공"
        );
    }

    @Test
    void 인증과_결제가_정상적으로_등록된다() {
        when(paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(any(), anyString())).thenReturn(false);
        when(paymentPersistencePort.existsByOrderIdAndMemberIdAndPaymentStatusIn(anyString(), anyLong(), any())).thenReturn(false);
        when(paymentAuthPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAuthRegistrationResult result = paymentAuthRegistrationService.registerAuthAndPayment(successCommand);

        assertThat(result.paymentId()).isNotBlank();
        assertThat(result.authId()).isNotBlank();
        assertThat(result.requestAmount()).isEqualTo(10000L);
        verify(paymentAuthPersistencePort).save(any());
        verify(paymentPersistencePort).save(any());
    }

    @Test
    void 동일_인증키가_이미_존재하면_예외가_발생하고_Payment_는_생성되지_않는다() {
        when(paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(any(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> paymentAuthRegistrationService.registerAuthAndPayment(successCommand))
                .isInstanceOf(DuplicatePaymentAuthException.class);

        verify(paymentAuthPersistencePort, never()).save(any());
        verify(paymentPersistencePort, never()).save(any());
    }

    @Test
    void 동일_주문_회원의_진행중인_결제가_있으면_예외가_발생한다() {
        when(paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(any(), anyString())).thenReturn(false);
        when(paymentPersistencePort.existsByOrderIdAndMemberIdAndPaymentStatusIn(anyString(), anyLong(), any())).thenReturn(true);
        when(paymentAuthPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> paymentAuthRegistrationService.registerAuthAndPayment(successCommand))
                .isInstanceOf(DuplicatePaymentException.class);

        verify(paymentPersistencePort, never()).save(any());
    }

    @Test
    void 인증_실패_콜백은_PaymentAuth_만_기록하고_Payment_는_생성하지_않는다() {
        PaymentApprovalCommand failedCommand = new PaymentApprovalCommand(
                PgProvider.INICIS, PaymentMethod.CARD, "order-1", 100L, "mid-1", "auth-tid-1",
                null, 10000L, AuthStatus.FAILED, "01", "인증실패"
        );
        when(paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(any(), anyString())).thenReturn(false);
        when(paymentAuthPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String authId = paymentAuthRegistrationService.registerFailedAuth(failedCommand);

        assertThat(authId).isNotBlank();
        verify(paymentPersistencePort, never()).save(any());
        verify(paymentPersistencePort, never()).existsByOrderIdAndMemberIdAndPaymentStatusIn(anyString(), anyLong(), any());
    }

    @Test
    void 인증_실패_콜백도_동일_인증키_중복검증을_거친다() {
        PaymentApprovalCommand failedCommand = new PaymentApprovalCommand(
                PgProvider.INICIS, PaymentMethod.CARD, "order-1", 100L, "mid-1", "auth-tid-1",
                null, 10000L, AuthStatus.FAILED, "01", "인증실패"
        );
        when(paymentAuthPersistencePort.existsByPgProviderAndPgAuthKey(any(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> paymentAuthRegistrationService.registerFailedAuth(failedCommand))
                .isInstanceOf(DuplicatePaymentAuthException.class);

        verify(paymentAuthPersistencePort, never()).save(any());
    }
}
