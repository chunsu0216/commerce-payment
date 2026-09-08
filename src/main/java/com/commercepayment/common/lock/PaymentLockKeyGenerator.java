package com.commercepayment.common.lock;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;

/**
 * 결제 승인 플로우의 분산락 키를 생성하는 유틸리티
 */
public class PaymentLockKeyGenerator {

    private static final String KEY_PREFIX = "payment:approval:lock:";
    private static final String CANCEL_KEY_PREFIX = "payment:cancel:lock:";

    private PaymentLockKeyGenerator() {
    }

    /**
     * pgProvider, orderId, memberId 조합으로 분산락 키를 생성한다
     */
    public static String generate(PgProvider pgProvider, String orderId, Long memberId) {
        return KEY_PREFIX + pgProvider + ":" + orderId + ":" + memberId;
    }

    /**
     * cancelId 기준으로 망취소 실행(조회→취소→확정) 분산락 키를 생성한다.
     * 최초 보상 흐름과 recovery scheduler가 이 키를 공유해야 동시 실행을 막을 수 있다.
     */
    public static String generateCancelExecutionKey(String cancelId) {
        return CANCEL_KEY_PREFIX + cancelId;
    }
}
