# 없는 HTTP 경로 404 수정 학습 방향표

https://github.com/owencity/jungsan_attack/pull/33

기준 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420` → `9e240275abbb18709e10d3156c502801b82bd910`. 구현 커밋을 고정한 방향표이며 실제 계정·운영 확인이나 학습 완료 기록이 아니다.

### Java 기본기

- 자바의 정석 8장 1.2 예외 클래스의 계층구조 (p.445) — [server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt:38](https://github.com/owencity/jungsan_attack/blob/9e240275abbb18709e10d3156c502801b82bd910/server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt#L38): 경로 부재의 구체 예외 두 타입과 기존 Exception 처리의 선택 관계를 확인한다.

### Effective Java

- 해당 없음

### 함수형 Java

- 해당 없음

### Kotlin 기본

- 7강, 코틀린에서 예외를 다루는 방법 — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:38](https://github.com/owencity/jungsan_attack/blob/9e240275abbb18709e10d3156c502801b82bd910/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L38): 테스트 실패 시에도 use가 MVC 컨텍스트를 닫는 자원 수명주기를 확인한다.

### Kotlin 고급

- 해당 없음

### 기술별 학습 포인트

#### Spring MVC

- fun handleMissingRoute — [server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt:39](https://github.com/owencity/jungsan_attack/blob/9e240275abbb18709e10d3156c502801b82bd910/server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt#L39): 구체 예외 핸들러의 선택과 기존 catch-all 500 처리의 관계가 HTTP 상태를 결정한다.

- registry.addResourceHandler("/**") — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:104](https://github.com/owencity/jungsan_attack/blob/9e240275abbb18709e10d3156c502801b82bd910/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L104): 등록된 리소스 핸들러 안의 경로 부재와 매핑 자체 부재가 다른 예외를 생성한다.

#### MockMvc

- DUMMY_SECRET — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:40](https://github.com/owencity/jungsan_attack/blob/9e240275abbb18709e10d3156c502801b82bd910/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L40): 실제 DispatcherServlet 요청과 엄격 JSON 비교가 404 상태뿐 아니라 오류 본문의 정보 노출 회귀를 검증한다.
