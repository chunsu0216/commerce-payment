# commerce-payment

이커머스 플랫폼(`commerce-*`)의 결제 서비스입니다. DDD 기반 헥사고날 아키텍처로 구성되어 있으며,
카드 결제 승인(PG 콜백 수신), 결제 취소/망취소 보상, 아웃박스를 통한 Kafka 이벤트 발행을 담당합니다.

- Java 21 / Spring Boot 3.5.0 (Spring Cloud 2025.0.0)
- 포트: **8085**
- Eureka 서비스명: `commerce-payment`
- PG(이니시스) 연동은 `InicisPgApprovalStrategyImpl` 이 **항상 성공을 리턴하는 mock 구현**이라, 외부 PG 없이도
  전체 결제 승인 플로우를 로컬에서 그대로 재현할 수 있습니다.

## 1. 사전 준비

- JDK 21
- Docker Desktop (또는 Docker Engine + Compose v2)
- MySQL 클라이언트(`mysql` CLI) — 없다면 아래 3번에서 `docker exec` 방식을 사용하면 됩니다.

## 2. 로컬 인프라 기동 (MySQL / Redis / Kafka)

```bash
docker compose up -d
```

`local-mysql`(3306), `local-redis`(6379), `local-kafka`(9092) 컨테이너가 뜹니다.

> **전체 플랫폼(commerce-member / commerce-order / commerce-product 포함)을 함께 실행하는 경우**
> 각 프로젝트의 `docker-compose.yml` 은 동일한 컨테이너 이름·포트·DB(`local_db`)를 공유하도록 맞춰져 있습니다.
> 즉 **`docker compose up -d`는 4개 프로젝트 중 아무 곳에서나 한 번만** 실행하면 되고, 모든 서비스가 같은
> MySQL/Redis/Kafka 인스턴스를 함께 사용합니다. (두 번째 프로젝트에서 다시 실행하면 컨테이너 이름 충돌이 납니다.)

## 3. DB 스키마 초기화

`spring.jpa.hibernate.ddl-auto=none` 이므로 애플리케이션 기동 전에 스키마를 직접 생성해야 합니다.

```bash
# mysql CLI가 있는 경우
mysql -h127.0.0.1 -P3306 -uapp -papp1234 local_db < sql/schema.sql
mysql -h127.0.0.1 -P3306 -uapp -papp1234 local_db < sql/data.sql   # 선택 사항 (샘플 조회용 데이터)

# 또는 컨테이너에 직접 흘려넣기
docker exec -i local-mysql mysql -uapp -papp1234 local_db < sql/schema.sql
docker exec -i local-mysql mysql -uapp -papp1234 local_db < sql/data.sql
```

- `sql/schema.sql`: `payment`, `payment_auth`, `payment_cancel`, `payment_outbox` 테이블 DDL
- `sql/data.sql`: 조회용 샘플 데이터(선택). 실제 결제 플로우는 4번의 콜백 호출로 직접 만들어보는 것을 권장합니다.

## 4. 애플리케이션 실행

Eureka Discovery Client가 활성화되어 있어 기본적으로 `commerce-discovery`(http://localhost:8761)가 떠있어야 합니다.
단, 이 서비스 혼자 결제 승인 플로우를 테스트하는 것은 Eureka/Gateway 없이도 가능합니다
(Eureka 등록 실패 시에도 애플리케이션 자체는 재시도하며 계속 기동됩니다).

```bash
# (선택) 다른 commerce-* 프로젝트에서 discovery 서버를 먼저 띄워두면 등록 로그 경고가 사라집니다.
./gradlew bootRun
```

정상 기동 확인: `http://localhost:8085/actuator/health` → `{"status":"UP"}`

## 5. 동작 확인 — 카드 결제 승인 플로우 데모

`POST /card/auth` 는 PG(이니시스)가 카드 인증 성공/실패를 알려주는 콜백을 흉내 낸 엔드포인트입니다.
mock PG 전략 덕분에 아래처럼 로컬에서 바로 호출해 전체 승인 플로우(PaymentAuth 등록 → Payment 등록 →
PG 승인 호출 → 성공 처리)를 확인할 수 있습니다.

```bash
curl -X POST http://localhost:8085/card/auth \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "P_STATUS=00" \
  -d "P_RMESG=정상 처리되었습니다" \
  -d "P_MID=inicis_demo_mid" \
  -d "P_AUTH_TID=auth-tid-$(date +%s)" \
  -d "P_OID=order-demo-$(date +%s)" \
  -d "P_AMT=10000" \
  -d "P_NOTI=1"
```

- `P_STATUS=00` 이면 인증 성공 → `PaymentAuth` + `Payment(PROCESSING)` 등록 후 PG 승인까지 자동 진행되어 `SUCCESS` 로 종료됩니다.
- `P_STATUS` 를 `00` 이외의 값으로 보내면 인증 실패 이력만 남고 승인은 진행되지 않습니다.
- `P_AUTH_TID`(PG 인증키), `P_OID`(주문 id)는 유니크 제약이 있으므로 재호출 시 값을 바꿔야 합니다.

호출 후 DB 확인:

```bash
docker exec -it local-mysql mysql -uapp -papp1234 local_db \
  -e "SELECT payment_id, order_id, payment_status, payment_amount, approved_amount FROM payment ORDER BY id DESC LIMIT 5;"
```

## 6. 정리

```bash
docker compose down          # 컨테이너만 정리 (볼륨 유지)
docker compose down -v       # 볼륨까지 삭제 (스키마/데이터 초기화)
```

## 참고: 전체 플랫폼과 함께 실행하기

이 서비스는 `commerce-order` 의 결제 결과 수신(Kafka `payment-result` 토픽), 망취소 보상(`payment-cancel` 토픽)과
연동됩니다. 전체 플랫폼을 함께 띄워 주문 → 재고 예약 → 결제 승인 → 주문 상태 변경까지 이어지는 흐름을 보려면
다음 순서를 권장합니다.

1. `commerce-discovery` (Eureka, 8761)
2. `commerce-gateway` (API Gateway, 8080) — 선택
3. 인프라 기동 + 스키마 초기화 (위 2~3번, 아래 서비스들 중 아무 한 곳에서)
4. `commerce-member` (8084), `commerce-product` (8082), `commerce-order` (8083), `commerce-payment` (8085)

각 프로젝트의 README를 참고하세요.
