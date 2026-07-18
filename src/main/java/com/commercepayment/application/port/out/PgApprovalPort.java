package com.commercepayment.application.port.out;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;

/**
 * PG 승인 API 호출을 위한 아웃바운드 포트
 */
public interface PgApprovalPort {

    /**
     * 지정된 PG사에 승인을 요청한다
     */
    PgApprovalResult approve(PgProvider pgProvider, PgApprovalRequest request);
}
