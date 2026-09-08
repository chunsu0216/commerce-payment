package com.commercepayment.adapter.out.pg.inicis;

import com.commercepayment.adapter.out.persistence.entity.PgProvider;
import com.commercepayment.adapter.out.pg.PgApprovalStrategy;
import com.commercepayment.adapter.out.pg.PgCancelStrategy;
import com.commercepayment.adapter.out.pg.PgInquiryStrategy;
import com.commercepayment.application.dto.PgApprovalRequest;
import com.commercepayment.application.dto.PgApprovalResult;
import com.commercepayment.application.dto.PgCancelRequest;
import com.commercepayment.application.dto.PgCancelResult;
import com.commercepayment.application.dto.PgInquiryRequest;
import com.commercepayment.application.dto.PgInquiryResult;
import com.commercepayment.domain.payment.PgApprovalStatus;
import com.commercepayment.domain.payment.PgCancelStatus;
import com.commercepayment.domain.payment.PgInquiryStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이니시스 승인/취소/거래결과 조회 API 연동을 대신하는 mock 구현체.
 * 실제 연동 전까지는 항상 정상 처리된 것으로 가정한 공통 값만 리턴하며,
 * 실제 PG 연동 시점에는 이 클래스 내부 구현만 실제 API 호출로 교체하면 된다.
 */
@Component
public class InicisPgApprovalStrategyImpl implements PgApprovalStrategy, PgCancelStrategy, PgInquiryStrategy {

    private static final String SUCCESS_RESULT_CODE = "0000";

    /**
     * 이 전략이 지원하는 PG사(이니시스)를 반환한다
     */
    @Override
    public PgProvider supports() {
        return PgProvider.INICIS;
    }

    /**
     * 이니시스 승인 API 호출을 대신해, 항상 승인 성공을 가정한 결과를 리턴한다
     */
    @Override
    public PgApprovalResult approve(PgApprovalRequest request) {
        return new PgApprovalResult(
                PgApprovalStatus.SUCCESS,
                UUID.randomUUID().toString(),
                request.approvedAmount(),
                SUCCESS_RESULT_CODE,
                "정상 승인되었습니다.",
                LocalDateTime.now()
        );
    }

    /**
     * 이니시스 취소(망취소) API 호출을 대신해, 항상 취소 성공을 가정한 결과를 리턴한다
     */
    @Override
    public PgCancelResult cancel(PgCancelRequest request) {
        return new PgCancelResult(
                PgCancelStatus.SUCCESS,
                UUID.randomUUID().toString(),
                SUCCESS_RESULT_CODE,
                "정상 취소되었습니다."
        );
    }

    /**
     * 이니시스 거래결과 조회 API 호출을 대신해, 항상 "아직 정상 취소되지 않고 승인이 유지 중"인 결과를 리턴한다.
     * 실제 연동 전까지는 이 결과로 인해 조회 이후 단계에서 항상 실제 취소 API 호출 경로를 타게 된다.
     */
    @Override
    public PgInquiryResult inquire(PgInquiryRequest request) {
        return new PgInquiryResult(
                PgInquiryStatus.APPROVED,
                UUID.randomUUID().toString(),
                SUCCESS_RESULT_CODE,
                "승인 상태가 유지되고 있습니다."
        );
    }
}
