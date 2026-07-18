package com.commercepayment.application.service;

import com.commercepayment.adapter.out.persistence.entity.Payment;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PaymentStatus;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * PAYMENT_RESULT 아웃박스 이벤트 payload 를 생성하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPayloadFactory {

    private final ObjectMapper objectMapper;

    /**
     * Payment 의 현재 상태를 기반으로 PAYMENT_RESULT 이벤트 payload(JSON)를 생성한다
     */
    public String createPaymentResultPayload(Payment payment) {
        PaymentResultPayload payload = new PaymentResultPayload(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getMemberId(),
                payment.getPaymentMethod(),
                payment.getPgProvider(),
                payment.getPaymentStatus(),
                payment.getApprovedAmount(),
                payment.getCancelledAmount(),
                payment.getPgTransactionId(),
                payment.getPgResultCode(),
                payment.getPgResultMessage(),
                payment.getApprovedAt(),
                payment.getFailedAt()
        );
        return writeAsJson(payload);
    }

    /**
     * 객체를 JSON 문자열로 직렬화한다
     */
    private String writeAsJson(PaymentResultPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결제 결과 이벤트 payload 직렬화에 실패했습니다.", e);
        }
    }

    private record PaymentResultPayload(
            String paymentId,
            String orderId,
            Long memberId,
            PaymentMethod paymentMethod,
            PgProvider pgProvider,
            PaymentStatus paymentStatus,
            Long approvedAmount,
            Long cancelledAmount,
            String pgTransactionId,
            String pgResultCode,
            String pgResultMessage,
            LocalDateTime approvedAt,
            LocalDateTime failedAt
    ) {
    }
}
