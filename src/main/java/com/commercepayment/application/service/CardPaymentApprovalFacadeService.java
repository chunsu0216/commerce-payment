package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentApprovalResult;
import com.commercepayment.application.dto.PaymentAuthRegistrationResult;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.port.in.ProcessCardPaymentApprovalUseCase;
import com.commercepayment.application.port.out.DistributedLockPort;
import com.commercepayment.application.port.out.PgApprovalPort;
import com.commercepayment.common.exception.PaymentCompensatedException;
import com.commercepayment.common.lock.PaymentLockKeyGenerator;
import com.commercepayment.config.PaymentLockProperties;
import com.commercepayment.domain.payment.PgApprovalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 카드 결제 승인 플로우 전체(분산락 → TX1 → PG 승인 → TX2 → (실패 시) 보상)를 조율하는 오케스트레이션 서비스.
 * 트랜잭션 경계를 직접 갖지 않으며, 각 트랜잭션은 별도 빈으로 위임한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentApprovalFacadeService implements ProcessCardPaymentApprovalUseCase {

    private final DistributedLockPort distributedLockPort;
    private final PaymentLockProperties paymentLockProperties;
    private final PaymentAuthRegistrationService paymentAuthRegistrationService;
    private final PaymentResultService paymentResultService;
    private final PaymentCompensationService paymentCompensationService;
    private final PaymentCancelExecutionService paymentCancelExecutionService;
    private final PgApprovalPort pgApprovalPort;

    /**
     * 분산락을 획득한 뒤 결제 승인 플로우 전체를 실행하고, 종료 시 락을 해제한다
     */
    @Override
    public PaymentApprovalResult processApproval(PaymentApprovalCommand command) {
        String lockKey = PaymentLockKeyGenerator.generate(command.pgProvider(), command.orderId(), command.memberId());
        Duration waitTime = Duration.ofSeconds(paymentLockProperties.getWaitSeconds());
        return distributedLockPort.executeWithLock(lockKey, waitTime, () -> executeApprovalFlow(command));
    }

    /**
     * 인증 성공/실패 여부에 따라 TX1 → PG 승인 → TX2(→ 보상) 플로우를 분기 실행한다
     */
    private PaymentApprovalResult executeApprovalFlow(PaymentApprovalCommand command) {
        if (command.authStatus() == AuthStatus.FAILED) {
            String authId = paymentAuthRegistrationService.registerFailedAuth(command);
            return new PaymentApprovalResult(false, null, authId, null, "카드 인증에 실패했습니다.");
        }

        PaymentAuthRegistrationResult registration = paymentAuthRegistrationService.registerAuthAndPayment(command);
        PgApprovalResult approvalResult = requestPgApproval(command);

        try {
            paymentResultService.applyApprovalResult(registration.paymentId(), approvalResult);
        } catch (Exception e) {
            log.error("TX2(결제 결과 반영) 실패, 보상 처리를 시작합니다. paymentId={}", registration.paymentId(), e);
            compensate(registration.paymentId(), approvalResult);
            throw new PaymentCompensatedException(registration.paymentId(), "결제 결과 반영에 실패하여 보상 처리(망취소)를 완료했습니다.");
        }

        return toApprovalResult(registration, approvalResult);
    }

    /**
     * PG 승인 API를 호출하고, 통신 예외 발생 시 UNKNOWN 결과로 변환한다
     */
    private PgApprovalResult requestPgApproval(PaymentApprovalCommand command) {
        try {
            PgApprovalRequest request = new PgApprovalRequest(command.mId(), command.pgAuthKey(), command.requestAmount());
            return pgApprovalPort.approve(command.pgProvider(), request);
        } catch (Exception e) {
            log.warn("PG 승인 API 통신에 실패해 UNKNOWN으로 처리합니다.", e);
            return new PgApprovalResult(PgApprovalStatus.UNKNOWN, null, 0L, "COMMUNICATION_ERROR", e.getMessage(), null);
        }
    }

    /**
     * TX2 실패 시 PaymentCancel(PENDING) 을 먼저 커밋한 뒤, PG 조회/망취소 실행을 위임한다.
     * PG 호출 도중 앱이 죽어도 이미 커밋된 PaymentCancel 을 recovery scheduler 가 이어받아 재처리할 수 있다.
     */
    private void compensate(String paymentId, PgApprovalResult approvalResult) {
        PaymentCancel pendingCancel = paymentCompensationService.recordPendingCancel(paymentId, approvalResult);
        paymentCancelExecutionService.executeCancel(pendingCancel.getCancelId());
    }

    /**
     * TX2 정상 커밋 결과를 최종 응답 DTO로 변환한다
     */
    private PaymentApprovalResult toApprovalResult(PaymentAuthRegistrationResult registration, PgApprovalResult approvalResult) {
        boolean success = approvalResult.status() == PgApprovalStatus.SUCCESS;
        PaymentStatus finalStatus = toPaymentStatus(approvalResult.status());
        return new PaymentApprovalResult(success, registration.paymentId(), registration.authId(), finalStatus, approvalResult.pgResultMessage());
    }

    /**
     * PG 승인 상태를 Payment 상태로 변환한다
     */
    private PaymentStatus toPaymentStatus(PgApprovalStatus status) {
        return switch (status) {
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case UNKNOWN -> PaymentStatus.UNKNOWN;
        };
    }
}
