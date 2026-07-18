package com.commercepayment.application.port.out;

import java.time.Duration;

/**
 * 분산락 획득/해제를 캡슐화한 아웃바운드 포트
 */
public interface DistributedLockPort {

    /**
     * 지정된 키로 분산락을 획득한 뒤 콜백을 실행하고, 콜백 종료 시 락을 해제한다
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, LockCallback<T> callback);

    /**
     * 락을 획득한 상태에서 실행할 작업을 정의하는 콜백
     */
    @FunctionalInterface
    interface LockCallback<T> {
        T call();
    }
}
