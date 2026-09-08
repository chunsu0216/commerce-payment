package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;

/**
 * PG 거래결과 조회 API 호출을 위한 아웃바운드 포트
 */
public interface PgInquiryPort {

    /**
     * 지정된 PG사에 거래결과 조회를 요청한다
     */
    PgInquiryResult inquire(PgProvider pgProvider, PgInquiryRequest request);
}
