# Apple 발급 대기와 카카오 운영 — 고정 스터디 가이드

기준 diff `c11c9b9af1dfd76c7daf90b9a4e6f92f23c00ce4` → `d3c7e47ccb879d2b65aefb220a3900f976d3e186`. 정책·목차는 작업 당시 origin/main `c11c9b9af1dfd76c7daf90b9a4e6f92f23c00ce4`의 실제 파일이다.
[고정 diff](changes.patch)의 변경 줄을 먼저 체크하고 before/after의 같은 파일을 비교한다. 기존 가이드와 학습 진도는 덮어쓰지 않는다.

## Java 기본기

- 자바의 정석 14장 2.5 스트림의 최종연산 (p.976) — [server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java:20](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java#L20): 설정 목록의 allMatch와 문자열 codePoints의 anyMatch가 각각 무엇을 검사하는지, 빈 목록은 size 조건에서 먼저 거절하는지 변경 줄을 읽는다.

## Effective Java

- Item 49, 매개변수가 유효한지 검사하라 — [server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java:16](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java#L16): 제공자 이름·인자 개수·빈 값 검사를 생략하면 잘못된 설정을 사용 가능으로 판단하는지 메서드와 신규 parity 테스트를 대조한다.

## 함수형 Java

- Chapter 6 / Section 6.3, 스트림 파이프라인 구축하기 — [server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java:21](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java#L21): 바깥 목록과 안쪽 문자 스트림에서 술어가 받는 값과 true/false를 구분해 변경한 검증 파이프라인을 읽는다.

## Kotlin 기본

- 5강, 코틀린에서 제어문을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt:12](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt#L12): when 식의 각 분기가 Boolean을 반환하고 알 수 없는 제공자는 false가 되는 흐름을 읽는다.

- 17강, 코틀린에서 람다를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt:13](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt#L13): all의 람다가 설정 문자열 하나를 받아 isNotBlank 결과를 돌려주는지 Java 술어와 신규 parity 테스트에서 비교한다.

## Kotlin 고급

- 해당 없음

## 기술별 학습 포인트

- 서비스 가용성 — [server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt:44](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt#L44): 카카오 필수 설정과 Apple 발급 대기를 서로 다른 기동 조건으로 검사하며, 준비 안 된 제공자는 HTTP/DB 호출 전에 닫히는지 init·available·신규 테스트를 함께 읽는다.

- Docker Compose 환경변수 — [docker-compose.prod.yml:54](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/docker-compose.prod.yml#L54): ${VAR?}가 미선언을 거절하되 빈 값은 전달하는 동작을 기존 ${VAR:?}와 diff에서 비교하고 prod 프로필 기동 시험 결과를 확인한다.

- 운영 설정 검증 — [scripts/verify-release.sh:48](https://github.com/owencity/jungsan_attack/blob/d3c7e47ccb879d2b65aefb220a3900f976d3e186/scripts/verify-release.sh#L48): 테스트가 로컬 프로필 성공에 머물지 않고 Apple 값이 빈 실제 prod 서버와 MySQL에서 health·카카오 인가·503·인증 가드를 확인하는지 diff를 읽는다.

검증: 서버 전체 테스트 통과, 신규 제공자 테스트 8건 포함. CI 실제 prod/MySQL 검증과 운영 health·실계정 로그인은 별도 확인한다.
