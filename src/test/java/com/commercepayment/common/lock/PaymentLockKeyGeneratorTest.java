package com.commercepayment.common.lock;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentLockKeyGeneratorTest {

    @Test
    void 락_키는_pgProvider_orderId_memberId_조합으로_생성된다() {
        String lockKey = PaymentLockKeyGenerator.generate(PgProvider.INICIS, "order-1", 100L);

        assertThat(lockKey).isEqualTo("payment:approval:lock:INICIS:order-1:100");
    }
}
