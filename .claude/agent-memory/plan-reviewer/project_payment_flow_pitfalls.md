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

## 이 프로젝트의 구조적 사실 (검토 시 참고, 코드가 바뀌면 재확인할 것)
- 엔티티(Payment/PaymentAuth/PaymentCancel/PaymentOutbox)는 이미 완성되어 있고 도메인 모델 겸용으로 그대로 사용(루트 CLAUDE.md 방침과 일치). domain/ 패키지가 거의 비어있는 것 자체는 문제가 아님.
- BaseEntity는 @EnableJpaAuditing + @CreatedDate/@LastModifiedDate 사용 중이며 CommercePaymentApplication에 이미 @EnableJpaAuditing 붙어있음 — 이 부분은 항상 정상.
- application.yml의 kafka consumer 관련 설정 일부(`spring.json.value.default.type: com.commerce.order...`, `logging.level.com.commerce.order...`)가 commerce-order 서비스에서 복사된 잔재로 보임. 실제 클래스가 존재하지 않는 참조라 컨슈머 경로를 새로 설계할 때는 반드시 지적할 것.
- build.gradle에는 Redisson 의존성이 없음(RedisTemplate만 있음) — 분산락 계획이 나오면 redisson-spring-boot-starter 추가가 필요하다는 점은 매번 확인 대상.
