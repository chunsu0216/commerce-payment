package com.commercepayment.common.exception;

/**
 * 결제 결과 반영 트랜잭션(TX2) 실패로 PG 망취소 및 보상 처리(TX3)까지 완료되었을 때 발생하는 예외.
 * 원 요청 자체는 실패로 응답하되, 보상 처리가 정상적으로 마무리되었음을 나타낸다.
 */
public class PaymentCompensatedException extends RuntimeException {

    public PaymentCompensatedException(String paymentId, String message) {
        super(message + " paymentId=" + paymentId);
    }
}
