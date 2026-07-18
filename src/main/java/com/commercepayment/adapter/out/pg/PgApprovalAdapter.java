package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.port.out.PgApprovalPort;
import com.commercepayment.common.exception.PgCommunicationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PgProvider 에 맞는 PgApprovalStrategy 로 라우팅하는 PgApprovalPort 구현체
 */
@Component
public class PgApprovalAdapter implements PgApprovalPort {

    private final Map<PgProvider, PgApprovalStrategy> strategies;

    public PgApprovalAdapter(List<PgApprovalStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toUnmodifiableMap(PgApprovalStrategy::supports, Function.identity()));
    }

    /**
     * 지정된 PG사의 전략을 찾아 승인 API를 호출한다
     */
    @Override
    public PgApprovalResult approve(PgProvider pgProvider, PgApprovalRequest request) {
        return findStrategy(pgProvider).approve(request);
    }

    /**
     * PG사에 대응하는 승인 전략을 조회한다
     */
    private PgApprovalStrategy findStrategy(PgProvider pgProvider) {
        PgApprovalStrategy strategy = strategies.get(pgProvider);
        if (strategy == null) {
            throw new PgCommunicationException("지원하지 않는 PG사입니다: " + pgProvider, null);
        }
        return strategy;
    }
}
