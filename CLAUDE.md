# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

`commerce-payment`는 이커머스 플랫폼의 **결제 서비스** 마이크로서비스입니다.
- Java 21, Spring Boot 3.5.0 (Spring Cloud 2025.0.0)
- 포트: **8085**
- Eureka 에 서비스 등록 (`commerce-payment`), 로컬 Eureka 서버는 `localhost:8761` 필요
- 로컬 인프라는 Docker Compose 로 구동 (MySQL 8.4, Redis 7.2, Kafka 3.7 KRaft 모드)

이 서비스는 다른 마이크로서비스들(예: `commerce-order`, `commerce-member` 등)과 함께 동작하는 전체 이커머스 플랫폼의 일부입니다. Eureka Discovery Client 가 활성화되어 있으므로, 다른 서비스와 연동되는 기능을 다룰 때는 이 점을 고려합니다.

## 아키텍처 개요

- DDD 를 기반으로 한 hexagonal architecture(포트&어댑터) 구조를 지향합니다. 새 코드를 추가할 때는 도메인 로직과 인프라(웹, JPA, Kafka, Redis 등 어댑터)를 분리하는 패키지 구조를 유지합니다.
- 단, 도메인 로직 중 순수 도메인 모델일 경우, 영속성 엔티티와 도메인 엔티티(모델)을 변환, 분리하지않고 엔티티 모델을 사용합니다.