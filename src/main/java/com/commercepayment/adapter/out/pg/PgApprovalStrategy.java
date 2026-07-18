package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;

/**
 * PG사별 승인 API 연동을 추상화하는 전략 인터페이스
 */
public interface PgApprovalStrategy {

    /**
     * 이 전략이 지원하는 PG사를 반환한다
     */
    PgProvider supports();

    /**
     * PG 승인 API를 호출한다
     */
    PgApprovalResult approve(PgApprovalRequest request);
}
