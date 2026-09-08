package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.CancelStatus;
import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentAuth;
import com.commercepayment.adapter.out.persistence.entity.PaymentCancel;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;
import com.commercepayment.application.port.out.DistributedLockPort;
import com.commercepayment.application.port.out.PaymentAuthPersistencePort;
import com.commercepayment.application.port.out.PaymentCancelPersistencePort;
import com.commercepayment.application.port.out.PaymentPersistencePort;
import com.commercepayment.application.port.out.PgCancelPort;
import com.commercepayment.application.port.out.PgInquiryPort;
import com.commercepayment.common.exception.PaymentCancelNotFoundException;
import com.commercepayment.common.exception.PaymentNotFoundException;
import com.commercepayment.common.lock.PaymentLockKeyGenerator;
import com.commercepayment.config.PaymentLockProperties;
import com.commercepayment.domain.payment.PgCancelStatus;
import com.commercepayment.domain.payment.PgInquiryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * PENDING(PROCESSING)/UNKNOWN 상태의 PaymentCancel 에 대해 "PG 거래결과 조회 → (필요시) 망취소 호출 → 결과 확정"을 실행하는 서비스.
 * 최초 보상 흐름(CardPaymentApprovalFacadeService)과 recovery scheduler 양쪽이 이 서비스의 단일 진입점을 거치며,
 * cancelId 기준 분산락으로 감싸 두 흐름이 같은 취소 건을 동시에 처리하지 못하도록 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelExecutionService {

    private final DistributedLockPort distributedLockPort;
    private final PaymentLockProperties paymentLockProperties;
    private final PaymentCancelPersistencePort paymentCancelPersistencePort;
    private final PaymentPersistencePort paymentPersistencePort;
    private final PaymentAuthPersistencePort paymentAuthPersistencePort;
    private final PgInquiryPort pgInquiryPort;
    private final PgCancelPort pgCancelPort;
    private final PaymentCompensationService paymentCompensationService;

    /**
     * cancelId 기준 분산락을 획득한 뒤 취소 실행 로직을 수행한다
     */
    public void executeCancel(String cancelId) {
        String lockKey = PaymentLockKeyGenerator.generateCancelExecutionKey(cancelId);
        Duration waitTime = Duration.ofSeconds(paymentLockProperties.getWaitSeconds());
        distributedLockPort.executeWithLock(lockKey, waitTime, () -> {
            doExecuteCancel(cancelId);
            return null;
        });
    }

    /**
     * 락 획득 이후 최신 상태를 다시 조회해, 이미 확정된 건이면 스킵하고 그렇지 않으면 조회→취소→확정을 수행한다
     */
    private void doExecuteCancel(String cancelId) {
        PaymentCancel paymentCancel = findPaymentCancel(cancelId);
        if (isAlreadyFinalized(paymentCancel)) {
            log.info("이미 확정된 취소 건이라 재처리를 스킵합니다. cancelId={}", cancelId);
            return;
        }

        Payment payment = findPayment(paymentCancel.getPaymentId());
        PaymentAuth paymentAuth = findPaymentAuth(payment.getAuthId());

        PgInquiryResult inquiryResult = inquire(payment, paymentAuth, cancelId);
        if (inquiryResult.status() == PgInquiryStatus.UNKNOWN) {
            paymentCompensationService.applyFinalCancelResult(cancelId, toUnknownCancelResult(inquiryResult));
            return;
        }
        if (inquiryResult.status() == PgInquiryStatus.CANCELLED) {
            paymentCompensationService.applyFinalCancelResult(cancelId, toAlreadyCancelledResult(inquiryResult));
            return;
        }

        PgCancelResult cancelResult = cancel(payment, paymentAuth, paymentCancel);
        paymentCompensationService.applyFinalCancelResult(cancelId, cancelResult);
    }

    /**
     * 이미 SUCCESS/FAILED 로 확정되어 더 이상 재처리가 필요 없는 건인지 판단한다
     */
    private boolean isAlreadyFinalized(PaymentCancel paymentCancel) {
        return paymentCancel.getCancelStatus() == CancelStatus.SUCCESS || paymentCancel.getCancelStatus() == CancelStatus.FAILED;
    }

    /**
     * PG 거래결과 조회 API를 호출하고, 통신 예외 발생 시 UNKNOWN 결과로 변환한다
     */
    private PgInquiryResult inquire(Payment payment, PaymentAuth paymentAuth, String cancelId) {
        try {
            PgInquiryRequest request = new PgInquiryRequest(paymentAuth.getMId(), paymentAuth.getPgAuthKey(), cancelId);
            return pgInquiryPort.inquire(payment.getPgProvider(), request);
        } catch (Exception e) {
            log.warn("PG 거래결과 조회 API 통신에 실패해 UNKNOWN으로 처리합니다. cancelId={}", cancelId, e);
            return new PgInquiryResult(PgInquiryStatus.UNKNOWN, null, "INQUIRY_COMMUNICATION_ERROR", e.getMessage());
        }
    }

    /**
     * PG 망취소 API를 호출하고, 통신 예외 발생 시 UNKNOWN 결과로 변환한다
     */
    private PgCancelResult cancel(Payment payment, PaymentAuth paymentAuth, PaymentCancel paymentCancel) {
        try {
            PgCancelRequest request = new PgCancelRequest(paymentAuth.getMId(), paymentAuth.getPgAuthKey(), paymentCancel.getCancelAmount(), paymentCancel.getCancelReason());
            return pgCancelPort.cancel(payment.getPgProvider(), request);
        } catch (Exception e) {
            log.error("PG 망취소 호출에 실패해 UNKNOWN으로 기록합니다. cancelId={}", paymentCancel.getCancelId(), e);
            return new PgCancelResult(PgCancelStatus.UNKNOWN, null, "CANCEL_COMMUNICATION_ERROR", e.getMessage());
        }
    }

    /**
     * 조회 실패(UNKNOWN) 결과를 재시도 대상 PgCancelResult 로 변환한다
     */
    private PgCancelResult toUnknownCancelResult(PgInquiryResult inquiryResult) {
        return new PgCancelResult(PgCancelStatus.UNKNOWN, null, inquiryResult.pgResultCode(), inquiryResult.pgResultMessage());
    }

    /**
     * PG 기준 이미 정상 취소된 조회 결과를 취소 확정용 PgCancelResult(SUCCESS) 로 변환한다
     */
    private PgCancelResult toAlreadyCancelledResult(PgInquiryResult inquiryResult) {
        return new PgCancelResult(PgCancelStatus.SUCCESS, inquiryResult.pgTransactionId(), inquiryResult.pgResultCode(), inquiryResult.pgResultMessage());
    }

    /**
     * cancelId 로 결제 취소 정보를 조회하고, 없으면 예외를 던진다
     */
    private PaymentCancel findPaymentCancel(String cancelId) {
        return paymentCancelPersistencePort.findByCancelId(cancelId)
                .orElseThrow(() -> new PaymentCancelNotFoundException(cancelId));
    }

    /**
     * paymentId 로 결제를 조회하고, 없으면 예외를 던진다
     */
    private Payment findPayment(String paymentId) {
        return paymentPersistencePort.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    /**
     * authId 로 인증 정보를 조회하고, 없으면 예외를 던진다
     */
    private PaymentAuth findPaymentAuth(String authId) {
        return paymentAuthPersistencePort.findByAuthId(authId)
                .orElseThrow(() -> new PaymentNotFoundException(authId));
    }
}
