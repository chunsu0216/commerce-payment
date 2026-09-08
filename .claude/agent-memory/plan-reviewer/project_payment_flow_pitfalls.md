---
name: project-payment-flow-pitfalls
description: commerce-payment 카드 결제 승인 플로우(TX1/TX2/보상TX3) 계획에서 반복적으로 나타나는 설계 공백 패턴
metadata:
  type: project
---

카드/PG 결제 승인 플로우(PG 콜백 → 분산락 → TX1 인증/결제 INSERT → PG 승인 API(트랜잭션 밖) → TX2 결과 반영+Outbox → 실패시 PG망취소+보상TX3) 계획을 검토할 때 반복적으로 놓치는 지점들.

## 자주 놓치는 지점
1. **보상 트랜잭션(TX3)의 Outbox 누락**: TX2(PAYMENT UPDATE + OUTBOX INSERT)가 롤백되면 원래 세팅된 approvedAmount 등 도메인 상태와 Outbox 이벤트가 함께 사라진다. TX3는 PaymentCancel 엔티티만 만들고 Outbox 재발행을 빠뜨리기 쉬운데, 이러면 다운스트림(order 서비스 등)이 결제 최종 상태를 영영 모르게 됨. TX3 설계에는 반드시 "원 결과 재적용(success/fail/unknown 재호출) → cancel() → PaymentCancel 생성 → Outbox INSERT"가 포함되어야 함.
2. **인증 실패 분기 누락**: PG 콜백의 인증 결과(예: 이니시스 P_STATUS)가 실패인데도 TX1이 무조건 Payment(PROCESSING) INSERT + PG 승인 API 호출로 이어지는 설계가 자주 나옴. 인증 자체가 실패했으면 승인 시도 자체를 생략해야 함.
3. **UNKNOWN 상태 중복 방지 누락**: 중복 결제 방지 검증(existsBy...PaymentStatusIn)에 보통 PROCESSING/SUCCESS만 넣고 UNKNOWN을 빠뜨림. UNKNOWN은 "PG에서 실제로는 승인됐을 수도 있는" 상태라 이걸 막지 않으면 이중 승인 위험이 생김.
4. **PG 콜백 실패 응답 포맷 미정의**: 정상 응답(resultCode=0000)만 정의하고 예외 상황(중복/락획득실패/PG통신오류)의 HTTP 응답 설계(GlobalExceptionHandler 등)가 패키지 구조에서 빠지는 경우가 많음. PG는 응답 포맷이 이상하면 무한 재시도할 수 있어 반드시 짚어야 함.
5. **DistributedLockPort 반환 타입**: application 포트가 Redisson RLock을 그대로 반환하도록 설계되면 헥사고날 포트 추상화 위반. opaque handle을 반환하도록 확인 필요.
6. **기존 스텁 파일과 신규 패키지 구조 충돌**: "기존 코드베이스 변경 없음"으로 명시된 컨트롤러(e.g. CardAuthController)와 새로 설계된 PG별 서브패키지 컨트롤러(e.g. inicis/InicisCardAuthController)가 동일 경로(`@PostMapping`)에 매핑되어 라우팅 충돌 가능성이 있는지 항상 확인. Plan에는 기존 파일을 이동/삭제하는지 여부가 종종 빠져 있음.

7. **락 네임스페이스 분리가 "크래시 복구"만 막고 "느리지만 살아있는" 동시 실행은 못 막는 패턴**: TX-A 선커밋 → TX 밖 PG 호출 → TX-B 확정 구조에서, 원 동기 흐름이 이미 승인 락(`PaymentLockKeyGenerator`, watchdog로 TTL 자동갱신)을 쥔 채 느리게 진행 중일 때, recovery scheduler가 "별도 네임스페이스" 락(예: cancelId 기반)을 걸어도 두 락 키가 다르면 Redis 레벨에서 전혀 배타적이지 않다. staleSeconds 임계값만으로 "크래시"와 "그냥 느림"을 구분하지 못하므로, scheduler와 원 흐름이 같은 PaymentCancel/Payment row를 동시에 처리할 수 있다. `Payment.cancel()`이 `cancelledAmount += cancelAmount`처럼 가산식이면 이중 실행 시 금액 정합성이 깨지고, 최종 확정 트랜잭션에서 아웃박스를 발행하면 이벤트도 중복 발행된다. 이런 계획을 볼 때마다: (a) 원 흐름과 scheduler가 "같은 락 키"를 공유하는지, 또는 (b) 확정 단계에 DB 레벨 원자적 클레임(조건부 UPDATE WHERE status='PENDING' 또는 @Version)이 있는지 반드시 확인할 것.
8. **불필요한 enum 값 리네이밍**: `ddl-auto: none` 환경에서 `@Enumerated(EnumType.STRING)` enum의 값 이름을 단순 "더 명확한 이름"을 위해 바꾸는 계획(예: PROCESSING→PENDING)은 기존 로우에 대한 수동 DML을 요구하면서 기능적 이득이 없는 경우가 많다. 기존 값이 이미 요구사항이 원하는 의미(PG 호출 전 대기 등)를 담고 있다면 이름 변경 자체를 범위에서 빼도록 권고.

## 이 프로젝트의 구조적 사실 (검토 시 참고, 코드가 바뀌면 재확인할 것)
- 엔티티(Payment/PaymentAuth/PaymentCancel/PaymentOutbox)는 이미 완성되어 있고 도메인 모델 겸용으로 그대로 사용(루트 CLAUDE.md 방침과 일치). domain/ 패키지가 거의 비어있는 것 자체는 문제가 아님.
- BaseEntity는 @EnableJpaAuditing + @CreatedDate/@LastModifiedDate 사용 중이며 CommercePaymentApplication에 이미 @EnableJpaAuditing 붙어있음 — 이 부분은 항상 정상.
- application.yml의 kafka consumer 관련 설정 일부(`spring.json.value.default.type: com.commerce.order...`, `logging.level.com.commerce.order...`)가 commerce-order 서비스에서 복사된 잔재로 보임. 실제 클래스가 존재하지 않는 참조라 컨슈머 경로를 새로 설계할 때는 반드시 지적할 것.
- (2026-09-08 정정) build.gradle에 Redisson 의존성이 이미 존재함(`org.redisson:redisson-spring-boot-starter:3.40.2`) 및 `RedissonDistributedLockAdapter`(leaseTime 미지정, watchdog가 TTL 자동갱신)가 이미 구현되어 있음. "Redisson 의존성 없음"은 더 이상 사실이 아니므로 분산락 계획 검토 시 이 어댑터의 watchdog 동작(락이 스레드 생존 중엔 만료되지 않음)을 근거로 동시성 분석할 것.
- `Payment.cancel(Long cancelAmount)`는 `cancelledAmount += cancelAmount` 가산식이며 자체적으로 중복 호출 방어(idempotency guard)가 없다. 취소/보상 관련 계획을 볼 때마다 이 메서드가 두 번 호출될 경로가 없는지 항상 확인할 것.
- `PaymentCancel`(idx_payment_cancel_status_updated_at), `Payment`(idx_payment_status_updated_at) 모두 recovery scheduler가 필요로 하는 (status, updated_at) 복합 인덱스를 이미 가지고 있어, recovery 대상 조회 자체는 추가 인덱스 없이 구현 가능.
