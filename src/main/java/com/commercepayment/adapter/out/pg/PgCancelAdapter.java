package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.port.out.PgCancelPort;
import com.commercepayment.common.exception.PgCommunicationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PgProvider 에 맞는 PgCancelStrategy 로 라우팅하는 PgCancelPort 구현체
 */
@Component
public class PgCancelAdapter implements PgCancelPort {

    private final Map<PgProvider, PgCancelStrategy> strategies;

    public PgCancelAdapter(List<PgCancelStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toUnmodifiableMap(PgCancelStrategy::supports, Function.identity()));
    }

    /**
     * 지정된 PG사의 전략을 찾아 취소(망취소) API를 호출한다
     */
    @Override
    public PgCancelResult cancel(PgProvider pgProvider, PgCancelRequest request) {
        return findStrategy(pgProvider).cancel(request);
    }

    /**
     * PG사에 대응하는 취소 전략을 조회한다
     */
    private PgCancelStrategy findStrategy(PgProvider pgProvider) {
        PgCancelStrategy strategy = strategies.get(pgProvider);
        if (strategy == null) {
            throw new PgCommunicationException("지원하지 않는 PG사입니다: " + pgProvider, null);
        }
        return strategy;
    }
}
