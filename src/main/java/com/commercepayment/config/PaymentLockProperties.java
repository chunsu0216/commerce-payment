package com.commercepayment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 결제 분산락 관련 설정 값을 담는 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment-lock")
public class PaymentLockProperties {

    private long waitSeconds = 3;

}
