package com.commercepayment.adapter.out.lock;

import com.commercepayment.common.exception.PaymentLockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedissonDistributedLockAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private RedissonDistributedLockAdapter lockAdapter;

    @BeforeEach
    void setUp() {
        lockAdapter = new RedissonDistributedLockAdapter(redissonClient);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    @Test
    void 락_획득에_성공하면_콜백을_실행하고_정상_해제한다() throws InterruptedException {
        when(rLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        String result = lockAdapter.executeWithLock("lock-key", Duration.ofSeconds(3), () -> "OK");

        assertThat(result).isEqualTo("OK");
        verify(rLock).unlock();
    }

    @Test
    void 락_획득에_실패하면_예외를_던지고_콜백을_실행하지_않는다() throws InterruptedException {
        when(rLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> lockAdapter.executeWithLock("lock-key", Duration.ofSeconds(3), () -> "OK"))
                .isInstanceOf(PaymentLockAcquisitionException.class);

        verify(rLock, never()).unlock();
    }

    @Test
    void 콜백_실행_중_이미_락_소유권을_잃었다면_해제를_생략한다() throws InterruptedException {
        when(rLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        lockAdapter.executeWithLock("lock-key", Duration.ofSeconds(3), () -> "OK");

        verify(rLock, never()).unlock();
    }
}
