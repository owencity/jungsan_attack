# 2026-10-07 백엔드 구현 진행 기록

리뷰는 CTO가 주말에 진행한다. 리뷰·병합을 기다리며 개발을 중단하지 않는다. main 병합과 운영 배포는 CTO 담당이다.

`feat/backend-v4`는 PR #17 계약 위에서 Core v2 Java/Kotlin 구현 소스를 포함한 독립 작업이다. 의존 PR을 대신 병합하지 않았다.
순수 Core와 Workflow·표시 근거·계좌 암호화는 언어별 독립 구현, HTTP와 JDBC·트랜잭션 인프라는 공유한다.

| 범위 | 상태 |
|---|---|
| 공유 방·링크·신원 + 총무별 단위·명단 | 구현, 재시도 ID·권한·별도 revision |
| 단위별 차수·SELF/HOST 응답 | 구현, 전체 seq 채번·다른 단위 차수 거절 |
| Core v2 preview·정산·AUTO·송금 근거 snapshot·되돌리기 | 구현 |
| 계좌 AES-GCM·공개 범위·송금/미수취/확인 | 구현 |
| REST 타임라인·내부 알림·열람·읽음 | 기반 구현, FC-005 전체 이벤트/문구 추가 정합성 작업 남음 |
| 단위 완료·전체 완료·7일/30일 삭제 | 구현, FK와 동시 추가 재검사 |
| 모임 기능 | 핸들러·서비스·JPA 제거, 과거 물리 테이블 보존 |
| Apple 로그인·앱 인증·원래 링크 복귀·로그아웃·탈퇴 | 후속 P0, cookie-only 규칙과 FC-014 앱 Bearer의 결정 충돌도 명시적으로 해결 필요 |
| 복수 총무 스푼 | SETTLEMENT_UNITS §7의 대상/기본 지급 미결, 조회 컬럼을 지급 완료로 집계하지 않음 |
| WebSocket 실시간 전달·알림 전체 정합성·보안/성능 마무리 | 후속 |
| 운영 DB 업그레이드·키 주입·프론트/실기기 연결·정식 스토어 출시 | 별도 검증과 사용자 리뷰/병합 필요 |

각 PR 후 `docs/study-guides/`에 source base/head, changes.patch, 원격 main 목차/정책 SHA, Java/Kotlin 코드 줄·클래스·함수·관점, 리뷰 체크와 학습 진도를 남긴다.
학습을 시작할 때 코드를 새 main으로 바꾸지 않고 해당 가이드의 고정 sourceHead와 diff를 사용한다.
자동 AI 리뷰와 실제 사람의 검토를 구분하고 미실행 검증을 통과로 쓰지 않는다.


## 실행한 검증

- `:core:test`: 54건, 실패/오류/건너뜀 0. 기존 Java parity 2,000개 입력 포함.
- `:server:test`: 79건, 실패/오류/건너뜀 0. 신규 엔드포인트 성공/비로그인 실패와 역할·상태 parity 포함.
- `:server:compileKotlin`·`compileJava`·`bootJar`: 성공.
- Spring Boot + 독립 MySQL 8.4: 실제 HTTP 25개 확인 통과. 멱등 생성, 오래된 hash, 교차 단위 응답 전체 rollback,
  AUTO 취소, A/B 동시 정산, 계좌 공개/암호화, 송금 이력, 완료 후 추가 단위, 수동 완료, 알림/읽음, 7일/30일 실제 배치 삭제.
- Liquibase 5.0.4 CLI의 별도 upgrade DB: CONFIRMED 사전 중단, COLLECTING→초기 단위+실명/SELF/seq 보존,
  다른 방 host FK 거절, checksum 유지 재실행 4개 확인. 앱 번들의 fresh migration과 CLI upgrade 결과는 구분한다.
- 아직 별도 검증할 경합: 삭제 배치와 같은 순간의 가입/입력 수정, 완료와 단위 생성의 반복 부하,
  보냈어요와 되돌리기 동시 요청, 운영 데이터/운영 키/실기기 연결. 25개 확인이 모든 경합 증명을 대신하지 않는다.

검증 재현은 scripts/verify-backend-v4.py, scripts/verify-v4-upgrade.py에 남겼다.
검증 DB/포트/컨테이너를 고정하여 운영 DB를 대상으로 받지 않는다. 알려진 로컬 테스트 비밀번호만 사용한다.
