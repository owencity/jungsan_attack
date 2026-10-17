# 출시 연결·기존 데이터 전환 학습 방향표

[PR #37](https://github.com/owencity/jungsan_attack/pull/37) · `b8b91bb348b828a6ec0ff98a3749fc8d66c5a15b` → `e9a3d50b0d11f87507ef80ceb772f04850f2442e`. 정책·카탈로그는 원격 main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`. 구현·리뷰·배포·사용자 학습은 별개다.

### Java 기본기

- 해당 없음

### Effective Java

- 해당 없음

### 함수형 Java

- 해당 없음

### Kotlin 기본

- 해당 없음

### Kotlin 고급

- 해당 없음

### 기술별 학습 포인트

#### CI 기준 소스

- 이전 실행 산출물의 재현 가능한 기준 — [.github/workflows/ci.yml:49](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/.github/workflows/ci.yml#L49): 확정된 이전 소스와 새 소스를 따로 빌드해야 같은 구현을 구버전처럼 시험하는 오류를 피한다.

#### 서버 수명주기

- 같은 DB를 유지한 프로세스 교체 — [scripts/verify-release.sh:43](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-release.sh#L43): Boot 3의 저장 뒤 서버만 교체하므로 새 DB 기동과 실제 업그레이드 검증을 구분한다.

#### Liquibase

- 적용 이력·체크섬·실행 시각 불변 — [scripts/verify-boot-upgrade.py:155](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-boot-upgrade.py#L155): 업그레이드 시 기존 changeSet을 다시 쓰거나 재실행하지 않았는지 DB 이력으로 대조한다.

#### 저장 데이터

- 암호문 보존과 읽기 호환성 — [scripts/verify-boot-upgrade.py:156](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-boot-upgrade.py#L156): 같은 계좌를 새로 저장하는 것과 구버전 암호문·JSON을 새 런타임에서 읽는 검증은 다르다.

#### JWT·HTTP

- 티켓 fixture와 실제 토큰 발급의 경계 — [scripts/verify-boot-upgrade.py:100](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-boot-upgrade.py#L100): 제공자 승인은 시험하지 않고 구버전 서버의 실제 티켓 교환·토큰 발급·새 서버의 검증을 이어 읽는다.

#### 정산 입력 계약

- 버전 전환 전 미리보기의 revision/hash — [scripts/verify-boot-upgrade.py:171](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-boot-upgrade.py#L171): 이전 입력 해시를 그대로 보내 정산하므로 런타임 변경이 준비된 요청의 계약을 깨는지 확인한다.

#### HTTP·DB 검증

- 검증 실패의 저장 부수효과 — [scripts/verify-boot-upgrade.py:194](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/scripts/verify-boot-upgrade.py#L194): 앱의 목표 인원 1 요청이 400이어도 단위를 남기지 않는지 실제 DB 개수와 함께 검증한다.

#### Spring MVC 검증

- 요청 검증과 서비스 호출 경계 — [server/src/test/kotlin/app/jeongsan/server/gathering/GatheringApiSpec.kt:85](https://github.com/owencity/jungsan_attack/blob/e9a3d50b0d11f87507ef80ceb772f04850f2442e/server/src/test/kotlin/app/jeongsan/server/gathering/GatheringApiSpec.kt#L85): 명단 크기와 자동 정산 목표 인원을 구분하고 Bean Validation 거절이 서비스 실행 전에 일어나는지 읽는다.

