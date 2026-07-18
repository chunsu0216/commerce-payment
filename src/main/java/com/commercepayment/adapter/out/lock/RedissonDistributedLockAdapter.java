package com.commercepayment.adapter.out.lock;

import com.commercepayment.application.port.out.DistributedLockPort;
import com.commercepayment.common.exception.PaymentLockAcquisitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 기반 DistributedLockPort 구현체.
 * leaseTime 을 지정하지 않아 Redisson watchdog 이 락 보유 중 TTL 을 자동 연장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonDistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;

    /**
     * 지정된 키로 분산락을 획득한 뒤 콜백을 실행하고, 종료 시 락을 해제한다
     */
    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, LockCallback<T> callback) {
        RLock lock = redissonClient.getLock(lockKey);
        if (!tryAcquire(lock, waitTime)) {
            throw new PaymentLockAcquisitionException(lockKey);
        }
        try {
            return callback.call();
        } finally {
            releaseIfHeld(lock);
        }
    }

    /**
     * 지정된 대기시간 동안 락 획득을 시도한다
     */
    private boolean tryAcquire(RLock lock, Duration waitTime) {
        try {
            return lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 현재 스레드가 보유 중인 락만 안전하게 해제한다(TTL 만료로 이미 소유권을 잃은 경우 스킵)
     */
    private void releaseIfHeld(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        } else {
            log.warn("락이 이미 만료되어 해제를 생략합니다. lockName={}", lock.getName());
        }
    }
}
