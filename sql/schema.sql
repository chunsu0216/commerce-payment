-- commerce-payment 로컬 실행용 스키마 초기화 스크립트
-- JPA hibernate.ddl-auto 가 none 으로 설정되어 있어, 아래 DDL을 먼저 실행해야 애플리케이션이 정상 동작합니다.
-- src/main/java/com/commercepayment/adapter/out/persistence/entity 의 엔티티 정의를 기준으로 작성했습니다.

USE local_db;

-- 카드 인증(PG 인증 콜백) 이력
CREATE TABLE IF NOT EXISTS payment_auth (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    auth_id            VARCHAR(36)  NOT NULL,
    order_id           VARCHAR(36)  NOT NULL,
    member_id          BIGINT       NOT NULL,
    pg_provider        VARCHAR(30)  NOT NULL,
    payment_method     VARCHAR(30)  NOT NULL,
    m_id               VARCHAR(100) NOT NULL,
    pg_auth_key        VARCHAR(100) NOT NULL,
    pg_transaction_id  VARCHAR(100),
    auth_status        VARCHAR(30)  NOT NULL,
    request_amount     BIGINT       NOT NULL,
    pg_result_code     VARCHAR(50),
    pg_result_message  VARCHAR(500),
    authenticated_at   DATETIME,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_auth_id UNIQUE (auth_id),
    CONSTRAINT uk_payment_auth_pg UNIQUE (pg_provider, pg_auth_key),
    KEY idx_payment_auth_order_id (order_id),
    KEY idx_payment_auth_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 결제
CREATE TABLE IF NOT EXISTS payment (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    payment_id         VARCHAR(36)  NOT NULL,
    auth_id            VARCHAR(36),
    order_id           VARCHAR(36)  NOT NULL,
    member_id          BIGINT       NOT NULL,
    payment_method     VARCHAR(30)  NOT NULL,
    payment_status     VARCHAR(30)  NOT NULL,
    pg_provider        VARCHAR(30)  NOT NULL,
    pg_transaction_id  VARCHAR(100),
    payment_amount     BIGINT       NOT NULL,
    approved_amount    BIGINT       NOT NULL,
    cancelled_amount   BIGINT       NOT NULL,
    pg_result_code     VARCHAR(50),
    pg_result_message  VARCHAR(500),
    approved_at        DATETIME,
    failed_at          DATETIME,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_id UNIQUE (payment_id),
    CONSTRAINT uk_payment_auth_id UNIQUE (auth_id),
    CONSTRAINT uk_payment_pg_transaction UNIQUE (pg_provider, pg_transaction_id),
    KEY idx_payment_order_id (order_id),
    KEY idx_payment_member_id (member_id),
    KEY idx_payment_status_updated_at (payment_status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 결제 취소(망취소 포함)
CREATE TABLE IF NOT EXISTS payment_cancel (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    cancel_id          VARCHAR(36)  NOT NULL,
    payment_id         VARCHAR(36)  NOT NULL,
    cancel_request_id  VARCHAR(100) NOT NULL,
    cancel_type        VARCHAR(30)  NOT NULL,
    cancel_status      VARCHAR(30)  NOT NULL,
    cancel_amount      BIGINT       NOT NULL,
    cancel_reason      VARCHAR(500),
    pg_cancel_id       VARCHAR(100),
    pg_result_code     VARCHAR(50),
    pg_result_message  VARCHAR(500),
    requested_at       DATETIME,
    cancelled_at       DATETIME,
    retry_count        INT          NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_cancel_id UNIQUE (cancel_id),
    CONSTRAINT uk_payment_cancel_request_id UNIQUE (cancel_request_id),
    KEY idx_payment_cancel_payment_id (payment_id),
    KEY idx_payment_cancel_status_updated_at (cancel_status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 결제 이벤트 아웃박스 (Kafka 발행용)
CREATE TABLE IF NOT EXISTS payment_outbox (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    event_id       VARCHAR(36)  NOT NULL,
    aggregate_id   VARCHAR(36)  NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    retry_count    INT          NOT NULL,
    published_at   DATETIME,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_outbox_event_id UNIQUE (event_id),
    KEY idx_payment_outbox_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
