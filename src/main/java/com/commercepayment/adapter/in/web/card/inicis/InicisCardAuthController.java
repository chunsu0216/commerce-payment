package com.commercepayment.adapter.in.web.card.inicis;

import com.commercepayment.adapter.in.web.card.PgCallbackResponse;
import com.commercepayment.application.dto.PaymentApprovalCommand;
import com.commercepayment.application.dto.PaymentApprovalResult;
import com.commercepayment.application.port.in.ProcessCardPaymentApprovalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 이니시스 카드 인증 콜백을 수신하는 인바운드 어댑터
 */
@RestController
@RequiredArgsConstructor
public class InicisCardAuthController {

    private final ProcessCardPaymentApprovalUseCase processCardPaymentApprovalUseCase;
    private final InicisCardAuthRequestMapper inicisCardAuthRequestMapper;

    /**
     * 이니시스 카드 인증 콜백을 받아 카드 결제 승인 플로우를 실행한다
     */
    @PostMapping("/card/auth")
    public PgCallbackResponse processCardAuthCallback(@RequestParam Map<String, String> rawParams) {
        PaymentApprovalCommand command = inicisCardAuthRequestMapper.toCommand(rawParams);
        PaymentApprovalResult result = processCardPaymentApprovalUseCase.processApproval(command);
        return PgCallbackResponse.from(result);
    }
}
