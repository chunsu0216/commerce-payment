package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;

/**
 * PG 취소(망취소) API 호출을 위한 아웃바운드 포트
 */
public interface PgCancelPort {

    /**
     * 지정된 PG사에 취소(망취소)를 요청한다
     */
    PgCancelResult cancel(PgProvider pgProvider, PgCancelRequest request);
}
