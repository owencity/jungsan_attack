# Boot 4 전환 학습 방향표

[PR #36](https://github.com/owencity/jungsan_attack/pull/36) · `e258f1c338e8ba8b4d2c99fe7b3aa17464055287` → `ad0b154effea3aa72e004d367f9f15150d66e122`. 정책·카탈로그는 origin/main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`로 고정했다.

### Java 기본기

- 해당 없음

### Effective Java

- 해당 없음

### 함수형 Java

- 해당 없음

### Kotlin 기본

- 해당 없음

### Kotlin 고급

- 20강, 코틀린의 어노테이션 — [server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt:27](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt#L27): 생성자 프로퍼티의 파라미터 적용 대상을 고정한 변경이 Kotlin 버전별 Spring 설정 주입 위치와 연결된다.

### 기술별 학습 포인트

#### Gradle 의존성 관리

- 컴파일러·BOM·도구 모음의 버전 경계 — [gradle/libs.versions.toml:2](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/gradle/libs.versions.toml#L2): Kotlin 플러그인·Boot BOM 버전을 함께 맞추면서 Java 도구 모음과 프레임워크 기준을 구분한다.

#### Spring Boot 4

- 기능별 starter와 자동 설정 발견 — [server/build.gradle.kts:23](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/build.gradle.kts#L23): Liquibase 라이브러리만 있는 것과 Boot가 기동 시 실행하는 자동 설정 모듈의 차이가 DB 준비를 결정한다.

#### Jackson·JJWT

- JSON 메이저 버전 공존과 런타임 의존성 — [server/build.gradle.kts:32](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/build.gradle.kts#L32): 애플리케이션 Jackson 3와 JJWT 내부 Jackson 2가 다른 패키지로 공존하므로 전역 제외의 영향을 확인한다.

#### Jackson 3

- 매퍼 빌더와 JSON 노드 문자열 접근 — [server/src/main/java/app/jeongsan/server/user/javaimpl/AppleTokens.java:23](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/src/main/java/app/jeongsan/server/user/javaimpl/AppleTokens.java#L23): 구성 완료 뒤 생성한 매퍼와 문자열 전용 헤더 접근이 Apple JWT의 기존 거절 경로를 유지하는지 읽는다.

#### Spring Boot·JSON

- 자동 설정 JSON 계약 회귀 검증 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:25](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L25): 운영 자동 설정 매퍼로 ISO 날짜·null·큰 정수를 검증해 수동 테스트 매퍼와의 차이를 확인한다.

#### 데이터 호환성

- 저장 JSON의 후방 호환성과 독립 기대값 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:48](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L48): 이전 저장 문자열을 고정해야 새 직렬화 결과끼리 비교하며 계좌번호 선행 0 손실을 놓치지 않는다.

#### Spring RestClient

- HTTP 클라이언트 변환기와 외부 응답 바인딩 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:61](https://github.com/owencity/jungsan_attack/blob/ad0b154effea3aa72e004d367f9f15150d66e122/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L61): RestClient 기본 변환기의 별칭·추가 필드·큰 ID 처리를 실제 제공자 로그인과 구분해 검증한다.

