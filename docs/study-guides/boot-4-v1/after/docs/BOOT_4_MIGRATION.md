# Spring Boot 4 전환

버전업은 Gradle의 버전을 바꾼 뒤, 의존성·호출 API·설정의 변경을 맞추고 실행 결과를 검증하는 작업이다.

| 순서 | 이 저장소에서의 변경 | 이유 |
|---|---|---|
| 1 | `gradle/libs.versions.toml`: Boot 3.5.16 → 4.1.1, Kotlin 2.1.20 → 2.3.21 | 최신 정식 Boot와 BOM 기준 Kotlin 사용 |
| 2 | `server/build.gradle.kts`: webmvc·restclient·liquibase·webmvc-test starter | Boot 4의 기능별 자동 설정 포함 |
| 3 | 미사용 Cloud BOM·QueryDSL·kapt 제거 | 불필요한 버전 제약과 코드 생성 단계 제거 |
| 4 | Jackson 3 의존성·import·변환기·매퍼 호출 수정 | Boot가 기본 등록하는 JSON 매퍼와 일치 |
| 4a | Kotlin 생성자 프로퍼티의 `@param:Value`·`@param:JsonProperty` 명시 | 컴파일러 변경 경고를 없애고 기존 애너테이션 적용 위치 유지 |
| 5 | 계산·서버 테스트와 bootJar, 실제 MySQL·HTTP 검증 | 컴파일 이후 기동·마이그레이션·API 회귀 확인 |

Java 21과 Gradle 8.14.3은 지원 범위이므로 유지한다. Spring·Hibernate·Tomcat 등의 하위 버전은 Boot BOM에 맡기며 각각 임의로 고정하지 않는다.
JJWT 내부 Jackson 2와 공용 Jackson 애너테이션은 유지한다. 계산 규칙·DB 스키마·API 계약을 변경하기 위한 작업은 아니다.

`JsonCompatibilitySpec`은 Boot 자동 설정 매퍼로 날짜 문자열·null·정수 금액·기존 계좌 JSON을 검증하고, RestClient 기본 변환기로 카카오 DTO를 읽는다.
기존 인증·정산·오류·권한·Java/Kotlin 동등성 테스트와 실제 MySQL CI가 나머지 경로를 검증한다.

결정과 공식 근거는 [ADR-022](ADR/022-spring-boot-4.md), 실행 결과는 이 PR의 자체 점검 기록을 따른다.
단위/CI 성공은 운영 실계정 로그인이나 웹·앱의 연결 검증 완료를 뜻하지 않는다.
