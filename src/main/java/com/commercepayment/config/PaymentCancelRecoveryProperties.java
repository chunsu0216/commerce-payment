package com.commercepayment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PaymentCancel recovery scheduler 관련 설정 값을 담는 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment-cancel-recovery")
public class PaymentCancelRecoveryProperties {

    // 스케줄러 실행 주기(ms)
    private long fixedDelayMs = 5000;

    // 이 시간(초) 이상 상태 변경이 없어야 recovery 대상으로 간주한다
    private long staleSeconds = 60;

    // 1회 실행당 최대 처리 건수
    private int batchSize = 50;

    // 이 횟수 이상 재시도한 건은 더 이상 자동 재처리하지 않는다
    private int maxRetryCount = 10;

}
