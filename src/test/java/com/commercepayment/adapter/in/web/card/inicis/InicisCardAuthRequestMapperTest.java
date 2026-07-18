package com.commercepayment.adapter.in.web.card.inicis;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InicisCardAuthRequestMapperTest {

    private final InicisCardAuthRequestMapper mapper = new InicisCardAuthRequestMapper();

    @Test
    void 인증_성공_코드면_authStatus_SUCCESS_로_매핑된다() {
        Map<String, String> rawParams = Map.of(
                "P_STATUS", "00",
                "P_RMESG", "인증성공",
                "P_MID", "mid-1",
                "P_AUTH_TID", "auth-tid-1",
                "P_OID", "order-1",
                "P_AMT", "10000",
                "P_NOTI", "100"
        );

        PaymentApprovalCommand command = mapper.toCommand(rawParams);

        assertThat(command.pgProvider()).isEqualTo(PgProvider.INICIS);
        assertThat(command.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(command.orderId()).isEqualTo("order-1");
        assertThat(command.memberId()).isEqualTo(100L);
        assertThat(command.mId()).isEqualTo("mid-1");
        assertThat(command.pgAuthKey()).isEqualTo("auth-tid-1");
        assertThat(command.requestAmount()).isEqualTo(10000L);
        assertThat(command.authStatus()).isEqualTo(AuthStatus.SUCCESS);
    }

    @Test
    void 인증_성공_코드가_아니면_authStatus_FAILED_로_매핑된다() {
        Map<String, String> rawParams = Map.of(
                "P_STATUS", "01",
                "P_RMESG", "인증실패",
                "P_MID", "mid-1",
                "P_AUTH_TID", "auth-tid-1",
                "P_OID", "order-1",
                "P_AMT", "10000",
                "P_NOTI", "100"
        );

        PaymentApprovalCommand command = mapper.toCommand(rawParams);

        assertThat(command.authStatus()).isEqualTo(AuthStatus.FAILED);
    }
}
