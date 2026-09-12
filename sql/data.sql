-- commerce-payment 로컬 데모용 샘플 데이터 (선택 사항)
-- schema.sql 실행 이후 적용합니다. 애플리케이션 동작에 필수는 아니며,
-- 테이블 구조와 조회 쿼리를 바로 확인해보고 싶을 때 사용합니다.
-- POST /card/auth 콜백을 직접 호출해 실제 플로우로 데이터를 쌓아보는 것을 권장합니다. (README 참고)

USE local_db;

INSERT INTO payment_auth
    (auth_id, order_id, member_id, pg_provider, payment_method, m_id, pg_auth_key, pg_transaction_id,
     auth_status, request_amount, pg_result_code, pg_result_message, authenticated_at, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'order-demo-0001', 1, 'INICIS', 'CARD',
     'inicis_demo_mid', 'auth-tid-demo-0001', 'pg-tx-demo-0001',
     'SUCCESS', 10000, '00', '정상 인증되었습니다.', NOW(), NOW(), NOW());

INSERT INTO payment
    (payment_id, auth_id, order_id, member_id, payment_method, payment_status, pg_provider, pg_transaction_id,
     payment_amount, approved_amount, cancelled_amount, pg_result_code, pg_result_message, approved_at,
     created_at, updated_at)
VALUES
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'order-demo-0001', 1,
     'CARD', 'SUCCESS', 'INICIS', 'pg-tx-demo-0001', 10000, 10000, 0, '0000', '정상 승인되었습니다.', NOW(),
     NOW(), NOW());

INSERT INTO payment_outbox
    (event_id, aggregate_id, event_type, payload, status, retry_count, published_at, created_at, updated_at)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'order-demo-0001', 'PAYMENT_RESULT',
     JSON_OBJECT('orderId', 'order-demo-0001', 'memberId', 1, 'paymentStatus', 'SUCCESS', 'amount', 10000),
     'PUBLISHED', 0, NOW(), NOW(), NOW());
