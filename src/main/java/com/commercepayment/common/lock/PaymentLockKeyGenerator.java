package com.commercepayment.common.lock;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;

/**
 * 결제 승인 플로우의 분산락 키를 생성하는 유틸리티
 */
public class PaymentLockKeyGenerator {

    private static final String KEY_PREFIX = "payment:approval:lock:";

    private PaymentLockKeyGenerator() {
    }

    /**
     * pgProvider, orderId, memberId 조합으로 분산락 키를 생성한다
     */
    public static String generate(PgProvider pgProvider, String orderId, Long memberId) {
        return KEY_PREFIX + pgProvider + ":" + orderId + ":" + memberId;
    }
}
