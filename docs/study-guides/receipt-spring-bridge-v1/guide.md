# Spring 영수증 선행 연결 학습 방향표

[PR #38](https://github.com/owencity/jungsan_attack/pull/38): `28ab9dd47f0ad6f8564b770176c7e6963f8bf67b` → `d3a91a27f0008bd0a9726738f8c701bd3dc66015`. 정책·목차는 origin/main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420` 고정본이다. 책 본문을 읽었다는 근거가 아니며 사용자 학습 진도와 별개다.

## Java 기본기

- JAVA-CONCURRENCY · 동시성 / 13장 쓰레드 2.1 synchronized를 이용한 동기화 p.841 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt:14](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt#L14): JVM 모니터와 공유 사용자 창·동시 슬롯의 접근 순서를 읽는다.

## Effective Java

- EJ-04 · Item 4, 인스턴스화를 막으려거든 private 생성자를 사용하라 — [server/src/main/java/app/jeongsan/server/receipt/javaimpl/ReceiptPolicy.java:8](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/java/app/jeongsan/server/receipt/javaimpl/ReceiptPolicy.java#L8): 상태 없는 Java 정책의 인스턴스 생성을 막은 선언을 확인한다.
- EJ-05 · Item 5, 자원을 직접 명시하지 말고 의존 객체 주입을 사용하라 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt:13](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt#L13): DB 권한과 외부 HTTP를 주입해 모델 없이 서비스 실패 경로를 시험하는 경계를 본다.
- EJ-49 · Item 49, 매개변수가 유효한지 검사하라 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt:21](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt#L21): 기동 시 내부 주소·키 오류를 거절하는 설정 경계를 확인한다.
- EJ-78 · Item 78, 공유 중인 가변 데이터는 동기화해 사용하라 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt:15](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt#L15): 여러 요청이 갱신하는 창과 permit 획득을 한 임계 구역에서 처리하는 이유를 본다.
- EJ-79 · Item 79, 과도한 동기화는 피하라 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt:20](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt#L20): 외부 호출은 입장 모니터 밖에서 실행하며 permit 보유와 모니터 보유를 구분한다.

## 함수형 Java

- 직접 관련된 별도 주제 없음. Java 독립 구현에는 람다·Stream·Optional 변경이 없다.


## Kotlin 기본

- KB-02 · 2강 코틀린에서 null을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptDto.kt:25](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptDto.kt#L25): 미인식 null과 확정된 0을 구분해야 하는 DTO를 읽는다.
- KB-07 · 7강 코틀린에서 예외를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt:20](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt#L20): 본문 읽기·추론·재검사 중 어느 실패에도 close가 호출되는 범위를 확인한다.
- KB-12 · 12강 코틀린에서 object 키워드를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt:36](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt#L36): 익명 객체에서 리다이렉트 설정을 바꾸는 JVM HTTP 어댑터를 읽는다.
- KB-17 · 17강 코틀린에서 람다를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt:15](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt#L15): 함수 인자로 스트림 생성을 늦춰 인증 전에 사진을 열지 않는 호출 순서를 확인한다.

## Kotlin 고급

- KA-15 · 15강 inline 함수 자세히 살펴보기 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt:28](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptService.kt#L28): inline use 람다 안의 return이 추출 함수에서 반환하면서도 자원을 닫는 흐름을 본다.
- KA-16 · 16강 SAM과 reference — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt:28](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptAdmission.kt#L28): SAM close 람다의 캡처와 AtomicBoolean으로 중복 해제를 막는 수명을 읽는다.
- KA-24 · 24강 유용한 코틀린 표준 라이브러리 함수들 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptResponseReader.kt:45](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptResponseReader.kt#L45): require 실패를 외부 계약 오류로 바꾸며 실수/문자 금액의 자동 변환을 막는 경계를 본다.

## 기술별 학습 포인트

- Spring MVC 인증과 바이너리 요청 · Spring MVC 인증과 바이너리 요청 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptController.kt:15](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptController.kt#L15): 사용자 식별을 먼저 해결하고 raw 스트림은 서비스 권한 검사 뒤 읽는 MVC 인자 처리 순서를 본다.
- Servlet multipart 지연 해석 · Servlet multipart 지연 해석 — [server/src/main/resources/application.yml:19](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/resources/application.yml#L19): multipart가 핸들러 전에 임시 파일로 파싱되는 기본 흐름과 지연 설정의 경계를 본다.
- JDBC 권한 조회·트랜잭션 수명 · JDBC 권한 조회·트랜잭션 수명 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptUnitAccess.kt:15](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptUnitAccess.kt#L15): 부모 단위 범위를 SQL에 포함하며 외부 추론 동안 DB 트랜잭션을 유지하지 않는 서비스 경계를 본다.
- RestClient 외부 응답 경계 · RestClient 외부 응답 경계 — [server/src/main/kotlin/app/jeongsan/server/receipt/HttpReceiptOcrClient.kt:19](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/HttpReceiptOcrClient.kt#L19): 상태별 오류 변환·응답 바이트 제한·응답 스트림 종료를 자동 재시도 없이 처리하는 위치를 본다.
- HTTP 시간 제한과 취소 한계 · HTTP 시간 제한과 취소 한계 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt:42](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptOcrConfiguration.kt#L42): 소켓 읽기 시간 제한과 전체 추론 취소·CPU 작업 중단을 구분한다.
- Jackson 3 숫자 타입·스키마 검증 · Jackson 3 숫자 타입·스키마 검증 — [server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptResponseReader.kt:14](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/server/src/main/kotlin/app/jeongsan/server/receipt/ReceiptResponseReader.kt#L14): 추가 JSON과 잘못된 타입을 막고 명시 DTO 필드만 외부에 노출하는 경계를 읽는다.
- MySQL·HTTP 계약 검증의 범위 · MySQL·HTTP 계약 검증의 범위 — [scripts/verify-receipt-bridge.py:90](https://github.com/owencity/jungsan_attack/blob/d3a91a27f0008bd0a9726738f8c701bd3dc66015/scripts/verify-receipt-bridge.py#L90): 실제 DB 불변과 모의 OCR 경계를 확인하되 실제 모델 정확도로 확대하지 않는 테스트 근거를 본다.

