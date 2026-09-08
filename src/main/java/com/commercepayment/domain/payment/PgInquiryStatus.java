package com.commercepayment.domain.payment;

/**
 * PG 거래결과 조회 API 호출 결과 상태
 */
public enum PgInquiryStatus {

    // PG 기준 승인이 정상 유지되고 있음(아직 취소되지 않음)
    APPROVED,

    // PG 기준 이미 정상적으로 취소 처리되어 있음
    CANCELLED,

    // PG 기준 승인 자체가 존재하지 않거나 거절된 상태
    NOT_APPROVED,

    // 조회 API 통신 실패 등으로 상태를 판단할 수 없음
    UNKNOWN

}
