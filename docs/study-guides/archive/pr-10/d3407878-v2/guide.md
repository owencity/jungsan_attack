# PR #10 스터디 가이드 v2

- 코드 기준: `d5feb4677f71c9fe973e31118fd3b82d27eb2788` → `d34078782c8380fc45d0eed913c867e0877c8a00`.
- 읽을 목차는 생성 시 최신 origin/main `f094e884580159ffb82844bd3d759cf028c96458`의 카탈로그에서 그대로 가져왔다.
- 추가·수정된 줄만 학습 근거로 검증했다. 먼저 changes.patch에서 해당 줄을 체크한 뒤 목차를 읽는다.
- 이 PR의 신규 구현은 Kotlin이다. Java 기본기·Effective Java·함수형 Java 항목은 없으며, Java 대응 구현은 PR #16에서 별도로 본다.

## Java 기본기

이번 diff에서 해당 카테고리의 추천 항목 없음.

## Effective Java

이번 diff에서 해당 카테고리의 추천 항목 없음.

## 함수형 Java

이번 diff에서 해당 카테고리의 추천 항목 없음.

## Kotlin 기본

- **읽을 위치:** KB-08 코틀린에서 함수를 다루는 방법 (Kotlin 기본 / 섹션 3. 코틀린에서 코드를 제어하는 방법 / 8강) · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserDto.kt:13](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserDto.kt#L13) → `User.toMeResponse` · **볼 관점:** 확장 함수의 수신 객체 User에서 응답 DTO를 만드는 필드 순서와 needsName 계산을 확인한다.
- **읽을 위치:** KB-14 코틀린에서 다양한 클래스를 다루는 방법 (Kotlin 기본 / 섹션 4. 코틀린에서의 OOP / 14강) · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserDto.kt:5](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserDto.kt#L5) → `MeResponse` · **볼 관점:** 로그인 응답에 추가된 실명·needsName 필드가 닉네임과 별개로 표현되는지 확인한다.
- **읽을 위치:** KB-07 코틀린에서 예외를 다루는 방법 (Kotlin 기본 / 섹션 3. 코틀린에서 코드를 제어하는 방법 / 7강) · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserService.kt:26](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserService.kt#L26) → `UserService.registerDisplayName` · **볼 관점:** 조건부 UPDATE 뒤 조회된 이름이 다른 경우 어떤 예외가 나가고 성공 응답이 차단되는지 확인한다.

## Kotlin 고급

- **읽을 위치:** KA-20 코틀린의 어노테이션 (Kotlin 고급 / 섹션 6. 어노테이션과 리플렉션 / 20강) · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserController.kt:14](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserController.kt#L14) → `UserController.registerDisplayName` · **볼 관점:** LoginUser·Valid·RequestBody의 적용 위치를 읽고 인증과 요청 검증이 각각 어디에서 작동하는지 확인한다.
- **읽을 위치:** KA-21 코틀린의 리플렉션 (Kotlin 고급 / 섹션 6. 어노테이션과 리플렉션 / 21강) · **코드:** [server/src/test/kotlin/app/jeongsan/server/common/AuthenticationGuardSpec.kt:26](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/test/kotlin/app/jeongsan/server/common/AuthenticationGuardSpec.kt#L26) → `AuthenticationGuardSpec` · **볼 관점:** 리플렉션으로 핸들러 파라미터의 LoginUser를 찾아 공개 화이트리스트 외 누락을 검사하는지 확인한다.

## 기술별 학습 포인트

- **읽을 위치:** JPQL 조건부 UPDATE — IS NULL·동시 최초 등록 · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserRepository.kt:14](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserRepository.kt#L14) → `UserRepository.registerDisplayName` · **볼 관점:** 동시 최초 등록: displayName IS NULL 조건이 먼저 성공한 이름을 보존하고 중복 요청을 어떻게 처리하는지 확인한다.
- **읽을 위치:** Spring Data JPA @Modifying — flushAutomatically·clearAutomatically · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserRepository.kt:13](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserRepository.kt#L13) → `UserRepository.registerDisplayName` · **볼 관점:** JPA 벌크 갱신: flush·clear 설정과 뒤따르는 재조회를 연결해 영속성 컨텍스트의 오래된 값이 응답에 남는지 확인한다.
- **읽을 위치:** JDK String.codePointCount — UTF-16 코드 단위와 유니코드 코드포인트 · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserService.kt:18](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserService.kt#L18) → `UserService.registerDisplayName` · **볼 관점:** 문자 수 경계: UTF-16 length 대신 코드포인트 수를 사용하고 공백 제거 뒤 길이를 검사하는지 확인한다.
- **읽을 위치:** Spring @Transactional — 조건부 갱신·재조회·예외 롤백 경계 · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/UserService.kt:14](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/UserService.kt#L14) → `UserService.registerDisplayName` · **볼 관점:** 트랜잭션: 조건부 갱신·재조회·충돌 예외가 하나의 쓰기 경계 안에 있는지 확인한다.
- **읽을 위치:** JPA @Column(updatable=false) — 일반 save와 전용 UPDATE 경로 · **코드:** [server/src/main/kotlin/app/jeongsan/server/user/User.kt:30](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/user/User.kt#L30) → `User.displayName` · **볼 관점:** 등록 후 보존: 일반 엔티티 save가 실명을 다시 덮지 않도록 제한하고 전용 갱신 쿼리가 쓰이는지 확인한다.
- **읽을 위치:** @LoginUser와 소유자 조회 — 인증 사용자 id의 목록 필터 · **코드:** [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringController.kt:24](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringController.kt#L24) → `GatheringController.list` · **볼 관점:** 권한과 목록 범위: 인증 사용자 id가 host 기준 조회까지 전달되어 다른 사용자의 술자리가 노출되지 않는지 확인한다.
- **읽을 위치:** Liquibase addColumn·include — 전역 changeSet id와 실명 컬럼 · **코드:** [server/src/main/resources/db/changelog/015-user-display-name.yaml:13](https://github.com/owencity/jungsan_attack/blob/d34078782c8380fc45d0eed913c867e0877c8a00/server/src/main/resources/db/changelog/015-user-display-name.yaml#L13) → `Liquibase 023-add-user-display-name` · **볼 관점:** 스키마 계약: 새 changeSet과 master include가 실명 컬럼을 만들고 기존 적용 파일을 수정하지 않는지 확인한다.
