package com.commercepayment.adapter.out.pg;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;

/**
 * PG사별 거래결과 조회 API 연동을 추상화하는 전략 인터페이스
 */
public interface PgInquiryStrategy {

    /**
     * 이 전략이 지원하는 PG사를 반환한다
     */
    PgProvider supports();

    /**
     * PG 거래결과 조회 API를 호출한다
     */
    PgInquiryResult inquire(PgInquiryRequest request);
}
