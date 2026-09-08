package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.CancelType;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.config.PaymentCancelRecoveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCancelRecoveryServiceTest {

    @Mock
    private PaymentCancelPersistencePort paymentCancelPersistencePort;

    @Mock
    private PaymentCancelExecutionService paymentCancelExecutionService;

    private PaymentCancelRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new PaymentCancelRecoveryService(
                paymentCancelPersistencePort, paymentCancelExecutionService, new PaymentCancelRecoveryProperties()
        );
    }

    @Test
    void 복구_대상을_조회해_각각_취소_실행을_위임한다() {
        PaymentCancel target1 = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        PaymentCancel target2 = new PaymentCancel("cancel-2", "payment-2", "req-2", CancelType.COMPENSATION, 5000L, "보상 취소");
        when(paymentCancelPersistencePort.findRecoveryTargets(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(target1, target2));

        recoveryService.recoverPendingCancels();

        verify(paymentCancelExecutionService).executeCancel("cancel-1");
        verify(paymentCancelExecutionService).executeCancel("cancel-2");
    }

    @Test
    void 한_건_처리에_실패해도_나머지_건은_계속_처리된다() {
        PaymentCancel target1 = new PaymentCancel("cancel-1", "payment-1", "req-1", CancelType.COMPENSATION, 10000L, "보상 취소");
        PaymentCancel target2 = new PaymentCancel("cancel-2", "payment-2", "req-2", CancelType.COMPENSATION, 5000L, "보상 취소");
        when(paymentCancelPersistencePort.findRecoveryTargets(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(target1, target2));
        doThrow(new RuntimeException("락 획득 실패")).when(paymentCancelExecutionService).executeCancel("cancel-1");

        recoveryService.recoverPendingCancels();

        verify(paymentCancelExecutionService).executeCancel("cancel-1");
        verify(paymentCancelExecutionService).executeCancel("cancel-2");
    }

    @Test
    void 조회_조건에_PROCESSING_과_UNKNOWN_상태가_포함된다() {
        when(paymentCancelPersistencePort.findRecoveryTargets(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        recoveryService.recoverPendingCancels();

        verify(paymentCancelPersistencePort).findRecoveryTargets(
                argThatContainsProcessingAndUnknown(), any(), anyInt(), anyInt()
        );
    }

    private Collection<CancelStatus> argThatContainsProcessingAndUnknown() {
        return org.mockito.ArgumentMatchers.argThat(statuses ->
                statuses.contains(CancelStatus.PROCESSING) && statuses.contains(CancelStatus.UNKNOWN)
        );
    }
}
