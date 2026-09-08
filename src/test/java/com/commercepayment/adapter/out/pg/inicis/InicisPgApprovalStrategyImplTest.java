package com.commercepayment.adapter.out.pg.inicis;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;
import com.commercepayment.domain.payment.PgApprovalStatus;
import com.commercepayment.domain.payment.PgCancelStatus;
import com.commercepayment.domain.payment.PgInquiryStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InicisPgApprovalStrategyImplTest {

    private final InicisPgApprovalStrategyImpl strategy = new InicisPgApprovalStrategyImpl();

    @Test
    void 지원하는_PG사는_이니시스다() {
        assertThat(strategy.supports()).isEqualTo(PgProvider.INICIS);
    }

    @Test
    void 승인_요청은_항상_성공을_가정한_결과를_리턴한다() {
        PgApprovalResult result = strategy.approve(new PgApprovalRequest("mid", "authKey", 10000L));

        assertThat(result.status()).isEqualTo(PgApprovalStatus.SUCCESS);
        assertThat(result.approvedAmount()).isEqualTo(10000L);
        assertThat(result.pgTransactionId()).isNotBlank();
    }

    @Test
    void 취소_요청은_항상_성공을_가정한_결과를_리턴한다() {
        PgCancelResult result = strategy.cancel(new PgCancelRequest("mid", "authKey", 10000L, "reason"));

        assertThat(result.status()).isEqualTo(PgCancelStatus.SUCCESS);
        assertThat(result.pgCancelId()).isNotBlank();
    }

    @Test
    void 거래결과_조회_요청은_항상_승인_유지_상태를_리턴한다() {
        PgInquiryResult result = strategy.inquire(new PgInquiryRequest("mid", "authKey", "cancel-1"));

        assertThat(result.status()).isEqualTo(PgInquiryStatus.APPROVED);
        assertThat(result.pgTransactionId()).isNotBlank();
    }
}
