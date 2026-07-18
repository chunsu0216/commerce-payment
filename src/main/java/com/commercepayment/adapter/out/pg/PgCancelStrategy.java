package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;

/**
 * PG사별 취소(망취소) API 연동을 추상화하는 전략 인터페이스
 */
public interface PgCancelStrategy {

    /**
     * 이 전략이 지원하는 PG사를 반환한다
     */
    PgProvider supports();

    /**
     * PG 취소(망취소) API를 호출한다
     */
    PgCancelResult cancel(PgCancelRequest request);
}
