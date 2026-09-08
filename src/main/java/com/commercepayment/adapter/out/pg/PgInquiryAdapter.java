package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;
import com.commercepayment.application.port.out.PgInquiryPort;
import com.commercepayment.common.exception.PgCommunicationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PgProvider 에 맞는 PgInquiryStrategy 로 라우팅하는 PgInquiryPort 구현체
 */
@Component
public class PgInquiryAdapter implements PgInquiryPort {

    private final Map<PgProvider, PgInquiryStrategy> strategies;

    public PgInquiryAdapter(List<PgInquiryStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toUnmodifiableMap(PgInquiryStrategy::supports, Function.identity()));
    }

    /**
     * 지정된 PG사의 전략을 찾아 거래결과 조회 API를 호출한다
     */
    @Override
    public PgInquiryResult inquire(PgProvider pgProvider, PgInquiryRequest request) {
        return findStrategy(pgProvider).inquire(request);
    }

    /**
     * PG사에 대응하는 조회 전략을 조회한다
     */
    private PgInquiryStrategy findStrategy(PgProvider pgProvider) {
        PgInquiryStrategy strategy = strategies.get(pgProvider);
        if (strategy == null) {
            throw new PgCommunicationException("지원하지 않는 PG사입니다: " + pgProvider, null);
        }
        return strategy;
    }
}
