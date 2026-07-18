package com.commercepayment.adapter.in.web.card.inicis;

import com.commercepayment.adapter.out.persistence.entity.AuthStatus;
import com.commercepayment.adapter.out.persistence.entity.PaymentMethod;
import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 이니시스 콜백 파라미터를 공통 결제 승인 커맨드로 변환하는 매퍼
 */
@Component
public class InicisCardAuthRequestMapper {

    // 이니시스 인증 성공을 나타내는 결과 코드(mock 단계 가정값, 실연동 시 이니시스 규격에 맞춰 확정 필요)
    private static final String SUCCESS_AUTH_CODE = "00";

    /**
     * 이니시스 콜백 원본 파라미터를 PaymentApprovalCommand 로 변환한다
     */
    public PaymentApprovalCommand toCommand(Map<String, String> rawParams) {
        String authCode = rawParams.get("P_STATUS");
        String authMessage = rawParams.get("P_RMESG");
        String mId = rawParams.get("P_MID");
        String pgAuthKey = rawParams.get("P_AUTH_TID");
        String orderId = rawParams.get("P_OID");
        Long requestAmount = Long.valueOf(rawParams.get("P_AMT"));
        Long memberId = Long.valueOf(rawParams.get("P_NOTI"));

        return new PaymentApprovalCommand(
                PgProvider.INICIS,
                PaymentMethod.CARD,
                orderId,
                memberId,
                mId,
                pgAuthKey,
                null,
                requestAmount,
                resolveAuthStatus(authCode),
                authCode,
                authMessage
        );
    }

    /**
     * 이니시스 인증 결과 코드를 공통 인증 상태로 변환한다
     */
    private AuthStatus resolveAuthStatus(String authCode) {
        return SUCCESS_AUTH_CODE.equals(authCode) ? AuthStatus.SUCCESS : AuthStatus.FAILED;
    }
}
