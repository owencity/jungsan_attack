# ADR-022: Spring Boot 4 정식 계열로 전환

## 맥락

2026-10-17 기준 공식 문서는 4.1.1을 최신 stable로 표시한다. CTO가 최신 정식 계열로 전환을 지시했다.
기존 3.5.16 선택은 호환성을 보수적으로 가정한 것으로, 이 저장소에서 Boot 4의 비호환을 검증한 결과가 아니었다.
미병합 ADR-021 예약과 충돌하지 않도록 022를 사용한다.

## 결정

- Boot 4.1.1과 해당 BOM 기준 Kotlin 2.3.21을 사용한다. Java 21·Gradle 8.14.3은 유지한다.
- Web MVC·RestClient·Liquibase와 MVC 테스트는 Boot 4의 기능별 starter를 사용한다.
- 애플리케이션과 테스트의 JSON 처리를 Jackson 3로 옮긴다. `com.fasterxml.jackson.annotation`은 공식 공용 애너테이션 패키지이므로 유지한다.
- Kotlin 생성자 프로퍼티의 설정·JSON 애너테이션은 기존의 파라미터 적용을 명시한다. 프로퍼티가 아닌 생성자 인자는 원래처럼 둔다.
- JJWT 0.13.0의 `jjwt-jackson`은 Jackson 2를 사용하는 지원된 구현으로 유지한다. 두 메이저 버전의 패키지를 혼동하거나 전역 제외하지 않는다.
- 실제 사용이 없는 QueryDSL·kapt와 Spring Cloud BOM을 제거한다. JPA 엔티티·저장소는 여전히 사용하므로 유지한다.
- API 필드·날짜·오류·금액 계약 및 Liquibase 이력은 변경하지 않는다.

## 근거

- [Boot 요구사항](https://docs.spring.io/spring-boot/system-requirements.html): Java 17 이상, Gradle 8.14 이상/9.x.
- [4.0 마이그레이션 지침](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide): 모듈별 starter·Kotlin 2.2 이상·Jackson 3 전환 및 Jackson 2 공존.
- [4.1 릴리스 노트](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes): Kotlin 2.3.21·Hibernate 7.4 기준.
- [Spring Cloud 호환표](https://spring.io/projects/spring-cloud/): 2025.0.x는 Boot 3.5용이고 2025.1.2 이상은 4.1 지원이다. Cloud 자체가 Boot 4와 비호환인 것은 아니다.
- [OpenFeign QueryDSL 7.6 POM](https://github.com/OpenFeign/querydsl/blob/7.6/pom.xml): Boot 4.1·Spring 7·Hibernate 7.4 기준이 선언돼 있다. 제거 이유는 미사용이지 비호환 판정이 아니다.

## 대가

JSON과 자동 설정이 바뀌므로 컴파일 성공만으로 출시하지 않는다. Boot 자동 설정 매퍼와 RestClient 변환기를 검증하고 기존 전체 테스트·실제 MySQL/HTTP CI를 확인한다.
미병합 #33·#35 위에 쌓는 변경이므로 해당 PR 병합 뒤 대상 브랜치와 diff를 다시 확인해야 한다.
운영 배포와 실계정 로그인 검증은 이 PR의 로컬 테스트 결과와 구분한다.

## 재검토 조건

실제 DB 기동·인증·정산 계약 회귀가 해결되지 않으면 병합하지 않고 별도 브랜치에서 원인을 좁힌다.
QueryDSL 또는 Cloud가 필요해지면 그 시점의 공식 호환표와 ADR로 도입을 다시 결정한다.
