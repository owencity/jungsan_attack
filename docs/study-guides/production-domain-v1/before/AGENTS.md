# AGENTS.md — 정산어택 백엔드 (jungsan_attack)

> **2026-10-09 인증 결정:** CTO가 앱은 Bearer, 웹은 기존 httpOnly 쿠키를 사용하도록 확정했다.
> 아래 §4-7 쿠키 전용 규칙은 웹에 적용하며 앱은 [AUTH_RELEASE](docs/AUTH_RELEASE.md)를 따른다.
> APP/WEB 토큰은 교차 사용하지 않고, 앱의 일회용 티켓은 verifier와 함께 교환한다.

> **2026-10-06 현재 제품 방향:** REQUIREMENTS v4와 [SETTLEMENT_UNITS](docs/SETTLEMENT_UNITS.md)를 먼저 읽는다.
> 같은 Gathering의 링크·참여자 신원은 공유하고, 총무·명단·담당 차수·입력 버전·정산 상태는 단위별이다.
> 아래 v3 스냅샷의 전역 관리·정산 표현을 신규 개발에 사용하지 않는다. Domain V2의 단일 총무 설계도 기록이다.
> API v5는 설계 계약이며 실제 API·DB 구현 완료와 구분한다. 면제 API는 보류다.
> 신규 기능은 Java와 Kotlin을 각각 독립 구현하고 결과를 비교한다. Java 학습 구현은 별도 패키지로 두어
> Spring Bean·URL을 중복 등록하지 않는다. 학습 가이드는 origin/main의 정책·카탈로그와 확정 PR diff에서 뽑는다.

> **2026-10-09 작업 기준:** Codex는 백엔드 시니어, Claude는 프론트 시니어다.
> [§14 백엔드 시니어의 개발·검증 기준](#14-백엔드-시니어의-개발검증-기준)을 적용한다.
> Java·Kotlin 독립 구현과 PR diff 기반 스터디 보존은 유지한다. 작업 시작 시 최신 원격
> `flow-changes`의 모든 `열림` 항목을 읽고, 이번 PR의 반영·후속 범위를 기록한다.

> **2026-10-10 병합 결정:** 최신 PR 커밋의 CI 전체 통과·리뷰 기록과 스터디 가이드 보존을 확인하고,
> CTO 결정이 필요한 미결 사항이 없으면 Codex가 PR 병합과 배포를 진행한다. 결정 사항은 PR 본문에
> 남기고 CTO에게 묻는다. 학습 진도와 운영 로그인 검증은 병합 여부와 별도로 기록한다.

이 문서는 이 저장소에서 코드를 쓰는 에이전트(Codex)를 위한 것이다.
**대상은 `server`·`core` 모듈, 즉 백엔드뿐이다.** 프론트엔드는 별도 저장소 두 곳에
있고 Claude가 작업한다 — 웹은 `profile`(React), 앱은 `jungsan_app`(Kotlin Multiplatform,
iOS·Android). 이 저장소 안에 프론트 코드는 없다.

## 역할 관계

- **CTO(사용자)** — 방향을 정하고 우선순위를 매긴다.
- **프론트 시니어(Claude)** — 웹·앱을 구현하고 서버에 필요한 변동을 FC로 먼저 남긴다.
  보고에는 FC 번호를 붙인다. 백엔드에 없는 요구를 채팅 보고에만 남기지 않는다.
- **백엔드 시니어(Codex, 너)** — 백엔드의 설계·초기 구현·자체 점검·테스트·PR·스터디 기록을
  책임진다. 구현 직후 중복 처리·동시성·가용성·권한·데이터 불변 조건을 다시 점검한다.
  합의된 범위의 개발을 자율적으로 진행하며, CTO의 PR 리뷰를 자체 검증의 대체물로 삼지 않는다.
  **`main`에 직접 커밋·푸시하지 않는다.** 브랜치 → PR로 전달하고 위 2026-10-10 조건을 충족한 PR은
  Codex가 병합한다. CTO의 미결 판단을 임의로 확정하지 않는다.

**너는 대화형으로 즉답을 못 받는다.** 애매한 지점을 만나면 추측으로 밀어붙이지 말고,
가정을 명시하고 되돌리기 쉽게 짠 뒤 PR 설명에 질문을 남겨라
(→ [§8 애매하면 가정을 적어라](#8-애매하면-가정을-적어라)).

---

## 1. 무엇을 만드는가

**정산어택** — **일회용** 술자리 정산 서비스. 총무가 술자리를 만들어 링크를 뿌리면,
참여자가 자기 차수별 참석·음주를 버튼으로 체크하고, 서버가 차수별로 계산해 **금액과
근거를 함께** 보여준다. N빵이 아니다. 정산이 끝나면 7일 뒤 사라진다.

MVP 플로우 (제품 v3, 2026-09-29):

```
총무:   로그인(카카오) → [새 술자리] → 차수별 금액·술 항목·결제자 입력
        → [링크 공유] → 응답 현황 → [정산하기] → 입금 확인 → 완료(7일 뒤 삭제)
참여자: 링크 → 로그인 → 차수별 [불참]/[논알코올]/[알코올] → 금액 확인
        → 송금 → [보냈어요]
```

**용어 — `Gathering` = 술자리가 최상위다. 모임(`Group`)은 없다**(`ADR-019`가 `ADR-009`를
대체). 차수마다 돈을 낸 사람은 **결제자(`Payer`)**다 — v2의 "차수 총무"라는 말은 쓰지 않는다.

> ⚠️ **`main`에는 아직 모임 코드가 남아 있다** — `server/group` 패키지, `GroupController`,
> `user_groups`·`group_members` 테이블, `gatherings.group_id`. 전부 **제거 대상**이다
> (`DOMAIN_DB_DESIGN_V2.md` §4.1·§8). 모임 코드를 참고해 새 기능을 짜지 마라.

제품 정의는 [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) **v3**가 기준이다.
[`docs/SPEC.md`](docs/SPEC.md)는 레거시 명세라 충돌하면 REQUIREMENTS가 이긴다.
요구사항만 보고 코드를 먼저 바꾸지 말고 계산 규칙·스키마·API 계약을 순서대로 개정한 뒤
구현한다.

---

## 2. 읽는 순서

1. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — 제품 요구사항 **v3**, 용어, 역할·권한,
   상태. **앞으로 만들 제품의 기준**
2. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 시스템이 지금 어떤 모양인지 한눈에
3. [`docs/DOMAIN_DB_DESIGN_V2.md`](docs/DOMAIN_DB_DESIGN_V2.md) — v3 도메인·상태·논리 스키마·
   트랜잭션·동시성. **스키마·도메인 작업은 여기서 시작한다**
4. [`docs/CALC_RULES_V2.md`](docs/CALC_RULES_V2.md) — 결제자별 수취·1원 올림 계산 규칙.
   **`core` 작업은 여기서 시작한다.** (`CALC_RULES.md`는 v1 회귀 기준으로만 남아 있다)
5. [`docs/ERD.md`](docs/ERD.md) — 스키마 요약. **진실은 `server/src/main/resources/db/changelog/`의
   Liquibase YAML이다** — 스키마를 바꿀 땐 changelog를 먼저 고치고 ERD.md를 맞춰 갱신한다.
6. [`docs/API.md`](docs/API.md) — 엔드포인트·요청·응답·오류 코드 계약. v3 개정 전이라 모임
   관련 절은 폐기 대상이다. **`server` 작업 시 API.md를 같은 PR에서 v3로 개정한다.**
7. [`docs/ADR/000-index.md`](docs/ADR/000-index.md) — 왜 이렇게 정했는지.
   특히 [001](docs/ADR/001-rational-not-bigdecimal.md)(BigDecimal 금지),
   [004](docs/ADR/004-two-step-confirm.md)(`inputHash` 2단계 확정),
   [014](docs/ADR/014-monolith-first-feature-package.md)(모놀리스·feature 패키지),
   [015](docs/ADR/015-immutable-confirmed-transfer-snapshot.md)(송금 스냅샷),
   [019](docs/ADR/019-onetime-gathering-no-group.md)(일회용 술자리·모임 제거 — **가장 최근 결정**)
8. [`docs/flow-changes/`](docs/flow-changes/README.md) — **프론트가 만들며 바뀐 흐름 중 서버가 알아야
   하는 것.** **백엔드 작업 시작 전에 최신 원격 main의 목록과 모든 `열림` 항목 본문을 읽는다.**
   작업 브랜치와 아직 병합되지 않은 프론트 문서 PR에 추가된 FC도 비교한다. 이번 작업 관련 항목은
   반영하고, 후속 항목은 이유와 남은 범위를 PR에 기록한다. `결정 필요`는 CTO 결정 전이라 구현하지
   않는다. 전부 반영했으면 같은 PR에서 목록·본문 상태를 `반영됨`으로 맞추고, 목록의 **백엔드 반영·근거**에
   PR·파일·검증을 적는다. 일부 반영·면제 보류·미검증은 `열림`을 유지한다. 운영 적용 여부는 별도로 적는다.
9. [`DEVLOG.md`](DEVLOG.md) — 최근 결정 이력
10. [`docs/DEPLOY.md`](docs/DEPLOY.md) — 배포 절차 (배포를 건드릴 때만)

> **ADR-013(MSA)과 ADR-010(채팅 분리)은 "보류"이지 "폐기"가 아니다.** 설계는 살아
> 있지만 **지금 코드는 014(모놀리스) 기준으로 쓴다.** 저장소 안에 "ADR-013의 REST API
> 서비스" 같은 오래된 주석이 남아 있는데(예: `GatheringController.kt`), 013이 유효하던
> 시점에 쓴 것이고 지금은 014가 대체했다. **보류된 ADR에 설계가 있다는 이유로 구현하지 마라.**

> 일부 ADR이 `report/flow.md`를 참조하는데 **그 파일은 이 저장소에 없다**(초기 기획
> 산출물이라 옮겨오지 않았다). 링크가 깨진 거지 네가 잘못 찾은 게 아니니, 찾아 헤매지 말고
> `SPEC.md`를 기준으로 삼아라.

---

## 3. 지금 상태 (2026-09-29 스냅샷)

이 표는 며칠이면 낡는다. 작업 전에 `git log --oneline -10`과 실제 컨트롤러 파일로 교차 확인해라.

| 영역 | 상태 (`main` 기준) |
|---|---|
| `core` 계산 엔진 | ⚠️ **`main`은 아직 v1**(전역 대표결제자 + greedy 상계). v2(결제자별 수취·1원 올림)는 로컬 작업 트리에만 있고 **커밋된 적이 없다** — 최우선으로 검증·커밋할 대상 |
| DB 스키마 | 🚧 changelog `011`(changeSet `017`)까지. 모임 관리 기반 `012`~`014`는 병합 안 됐고 v3에서 쓰지 않는다 |
| 카카오 로그인 | ✅ 동작. OAuth2 → httpOnly JWT 쿠키(`jeongsan_token`) → `GET /auth/me` |
| `Group` (모임) | ❌ **제거 대상**(`ADR-019`). `GroupController`와 테이블이 남아 있다 |
| `Gathering` (술자리) | 🚧 `GET /api/v1/gatherings`만 존재. **인증 안 걸림 + 응답 필드 4개뿐**인 옛 뼈대 코드 |
| 차수·응답·정산하기·송금·삭제 배치 | ❌ 엔드포인트 없음. 프론트는 전부 목데이터로 동작 중 |
| 정산방 타임라인·스푼 | ❌ 아직 없음. v3 MVP에 포함 — 타임라인은 **모놀리스 안 WebSocket**으로 만든다(`ADR-014`) |
| 배포 | 🚧 GitHub Actions → OCI SSH 파이프라인 작성·로컬 검증 완료. **실제 배포는 아직 안 함** |

**컨트롤러는 지금 3개뿐이다** — `AuthController`, `GroupController`(제거 대상), `GatheringController`.

> **표의 경로는 축약형이다 — 실제 매핑은 전부 `/api/v1` 프리픽스가 붙는다.**
> (`GroupController`는 `@RequestMapping("/api/v1/groups")`) 새 컨트롤러도 반드시
> `/api/v1/...`로 매핑해라. CORS가 `WebConfig`에서 `/api/**`에만 걸려 있어,
> 프리픽스를 빼면 프론트에서 CORS로 막힌다.
> `common/LoginUser.kt`의 KDoc 예시가 `@GetMapping("/groups")`로 축약돼 있는데 그건 예시일 뿐이다.

---

## 4. 절대 규칙

어기면 안 되는 것들이다. 바꾸고 싶으면 코드를 먼저 쓰지 말고 PR에서 문제 제기부터 한다.

1. **계산은 `core` 모듈만 한다.** `server`는 입력을 모아 `core`에 넘기고 결과를 응답할
   뿐이다 — 재계산도, 결과 저장도 하지 않는다(`ADR-005`). 새 계산 로직을 `server`에
   직접 짜지 마라. 호출법은 [§6](#6-core-모듈-호출-계약).
2. **금액 누적에 `BigDecimal`을 쓰지 않는다.** `core`의 `Rational`만 쓴다. 실측 근거:
   랜덤 30,000건 중 210건(0.70%)에서 `BigDecimal` 누적이 틀린 금액을 냈다
   (`CALC_RULES.md` §2.1, `ADR-001`).
3. **`core`는 순수하게 유지한다.** Spring·JPA·시간(`Instant.now()`)·랜덤 등 부수효과를
   `core`에 들이지 마라. 현재 `core`의 의존성은 테스트용 Kotest뿐이다.
   추가로 **`core`는 `allWarningsAsErrors = true`다 — 경고 하나만 나도 빌드가 깨진다.**
4. **스키마는 Liquibase changelog로만 바꾼다.** `ddl-auto: none` 고정이라 엔티티를
   고쳐도 테이블은 안 바뀐다. 순서는 항상 **changelog 먼저, 엔티티가 그걸 따라간다.**
   작성 규칙은 [§7](#7-liquibase-changelog-작성-규칙) — **번호 규칙이 함정이니 꼭 읽어라.**
5. **패키지는 layer가 아니라 feature로 나눈다** — `server/group`, `server/gathering`,
   `server/user`, `server/common`, `server/config`. `controller/`·`service/`·`repository/`
   최상위 패키지를 만들지 않는다(`ADR-014`).
6. **Spring Security를 쓰지 않는다.** 의도적 결정이다 — 세션 기반 OAuth2 Client
   오토컨피그가 이 프로젝트의 "완전 무상태" 방향과 안 맞는다(`server/build.gradle.kts`
   주석 참조). 카카오 토큰/사용자정보는 `RestClient`로 직접 호출하고 우리 JWT만 발급한다.
   인증이 필요하면 기존 `@LoginUser` 방식을 확장해라.
7. **인증은 httpOnly JWT 쿠키(`jeongsan_token`)뿐이다.** `Authorization: Bearer` 헤더나
   `localStorage` 토큰 방식을 새로 만들지 않는다. `api.jungsan.devkdk.com`과
   `jungsan.devkdk.com`이 같은 등록 도메인(`devkdk.com`)을 공유해 `SameSite=Lax`가
   same-site로 동작한다는 전제가 깔려 있다 — 도메인 구조를 바꾸는 변경은 이 전제를 다시 봐야 한다.

   > ⚠️ **인증 필터가 없다. 기본값은 "공개"다.** Spring Security를 안 쓰므로 요청을
   > 가로채 검사하는 계층이 없고, 인증은 오직 `@LoginUser` 파라미터 리졸버뿐이다.
   > **`@LoginUser`를 안 붙인 핸들러는 누구나 부를 수 있다.** 실제로
   > `GatheringController`가 안 붙여서 `findAll()`로 전 사용자의 술자리가 나가고 있다.
   > 로그인이 필요한 엔드포인트에는 **반드시** `@LoginUser userId: Long`을 받아라.
   >
   > **이 함정을 사람 기억에 의존하지 않는다.** `server/src/test`에 인증 가드
   > 테스트를 하나 둔다 — 로그인 없이 호출돼도 되는 엔드포인트를 화이트리스트로
   > 명시하고, 그 외 모든 컨트롤러 핸들러가 `@LoginUser`(또는 동등한 인증
   > 파라미터)를 갖는지 리플렉션으로 순회 검증한다. 새 컨트롤러·핸들러를
   > 추가하는 PR은 이 테스트를 통과해야 하고, 화이트리스트에 새 항목을
   > 추가한다면 PR 설명에 왜 공개여야 하는지 적는다.

8. **`API.md`에 `⚠️ 아직 정하지 않았다`로 표시된 결정을 임의로 정리하지 마라.**
   문서와 구현이 어긋난 채로 **일부러 열어둔** 지점들이다(예: JWT 만료가 문서 14일 /
   구현 30일 — `API.md` §2.3). 지나가는 김에 한쪽으로 맞추면 CTO의 미결 결정이
   리뷰 없이 확정된다. 발견하면 고치지 말고 PR 설명에 "여기 미결이 있다"고만 적어라.
9. **`application-prod.yml`에 기본값을 넣지 않는다.** 환경변수가 없으면 부팅이 즉시
   실패해야 한다(fail-fast). 빈 값으로 떠서 나중에 조용히 깨지는 게 훨씬 나쁘다.
10. **N+1을 만들지 않는다.** 목록에 여러 항목이 있으면 항목마다 쿼리를 날리지 말고
   `IN :ids` + `GROUP BY` 배치로 한 번에 모은다
   (현재 예시는 `GroupRepository.kt`의 `countByGroupIds` — 모임 코드는 제거 대상이지만
   **패턴은 그대로 쓴다.** 예: 술자리 목록의 참여자 수·응답 수를 한 쿼리로 모으기).
11. **새 의존성을 임의로 추가하지 않는다.** 이 저장소는 스택 선택을 ADR로 관리한다.
    라이브러리가 필요하면 추가하지 말고 PR 설명에 "무엇이·왜 필요한지"를 적어 물어라.

---

## 5. 자주 걸리는 함정

실제로 걸렸던 것들이다.

- **이미 적용된 changelog 파일을 절대 수정하지 마라.** Liquibase는 changeSet 내용의
  checksum을 `DATABASECHANGELOG`에 저장해 두고, 다음 기동 때 파일이 바뀌었으면
  `ValidationFailedException`을 던진다 — **앱이 아예 안 뜬다.** 주석 한 글자도 마찬가지다.
  바꿀 게 있으면 **항상 새 changeSet을 추가**해라.

  > ⚠️ **바로 아래 항목이 이 함정으로 유인한다.** `002-groups.yaml`에는 아직
  > `tableName: groups`와 "USERS가 예약어라 복수형을 쓰듯…"이라는 **틀린 주석**이
  > 남아 있다. 고치고 싶어지겠지만 **고치면 안 된다.** 그래서 `011`이 002를 건드리는
  > 대신 `renameTable`이라는 새 changeSet으로 처리한 것이다.

- **MySQL에서 컬럼 제약과 주석을 순차 변경하면 서로 지워질 수 있다.** 실제로 `018`의
  `addNotNullConstraint`가 `admin_user_id` 주석을 지웠고, `021`의
  `setColumnRemarks`는 반대로 `NOT NULL`을 풀었다. 둘 다 필요하면 후속 changeSet의
  단일 `ALTER TABLE ... MODIFY COLUMN ... NOT NULL COMMENT ...`에서 전체 정의를 함께
  선언하고, `information_schema.columns`로 최종 상태를 확인해라(`014` 참조).

- **`GROUPS`는 MySQL 예약어다.** `USERS`는 통했는데 `GROUPS`는 복수형도 예약어라서
  (윈도우 함수 프레임용) 테이블을 `user_groups`로 리네임해야 했다(changelog `011`).
  엔티티는 `@Table(name = "user_groups")`이지만 클래스명은 `Group` 그대로다.
  **테이블명을 새로 지을 때 "복수형이니 안전하겠지"라고 가정하지 말고
  `SELECT * FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD='...' AND RESERVED=1`로 확인해라.**
- **CORS `allowedMethods`에 `PATCH`가 빠져 있다.** `WebConfig`는 현재
  `GET, POST, PUT, DELETE, OPTIONS`만 허용한다. `API.md` §3.3의
  `PATCH /gatherings/{id}`를 구현하면 여기도 같이 추가해야 프론트에서 부를 수 있다.
- **Kotlin의 non-null `Long`은 primitive `long`으로 컴파일된다.**
  `HandlerMethodArgumentResolver`에서 타입을 볼 때 `Long::class.java` 하나만 보면 놓친다.
  `Long::class.javaPrimitiveType`과 `javaObjectType` 둘 다 봐야 한다
  (`common/LoginUser.kt:48-49`가 이미 그렇게 돼 있다 — 지우지 마라).
- **`GlobalExceptionHandler`의 핸들러 4개를 지우지 마라.** `ApiException`,
  `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `Exception` 순으로
  있다. 특히 `HttpMessageNotReadableException`이 빠지면 깨진 JSON 요청이 400이 아니라
  500(`INTERNAL_ERROR`)으로 나간다.
- **`API.md` 계약과 구현이 갈라지는 건 컴파일 에러가 안 난다.** `GET /groups/{id}`가
  한동안 멤버 닉네임 없이 `userId`만 내려간 적이 있다. 응답 DTO를 만들었으면
  PR 올리기 전에 `API.md`의 해당 절과 필드 단위로 대조해라.
- **`server` 컴파일이 느리다(약 30초).** QueryDSL의 kapt 어노테이션 처리 때문이다.
  계산 로직만 건드렸다면 `:core:test`(수 초)로 먼저 빠르게 돌려라.

---

## 6. `core` 모듈 호출 계약

`server`가 정산을 붙일 때 필요한 전부다. 진입점은 **하나**다.

```kotlin
Settlement.settle(input: SettlementInput): SettlementOutcome
```

**엔진은 예외를 던지지 않는다.** 실패도 반환값이다 — HTTP 매핑은 API 계층의 몫이다.

```kotlin
sealed interface SettlementOutcome {
    data class Success(val result: SettlementResult) : SettlementOutcome
    data class Failure(val errors: List<ValidationError>) : SettlementOutcome
}
```

`try/catch`로 감싸지 말고 `when`으로 두 갈래를 모두 처리해라. `Failure`를 무시하거나
`!!`로 뚫으면 잘못된 금액이 아니라 500이 나간다.

주요 타입 (전부 `app.jeongsan.core` 패키지):

| 타입 | 파일 | 용도 |
|---|---|---|
| `SettlementInput` | `Model.kt` | 엔진 입력. `Participant`·`Round`·`ExtraItem`·`Attendance`를 담는다 |
| `SettlementResult` | `SettlementResult.kt` | `amounts`, `breakdown`, `transfers`, `grandTotal` 등 |
| `ParticipantBreakdown` | `SettlementResult.kt` | 근거 화면(W2)용. **계산만 하고 버리지 마라** |
| `Validator.validate(input, phase)` | `Validation.kt` | `ValidationPhase.SAVE` / `CONFIRM` |
| `ErrorCode` (enum) | `Validation.kt` | **`API.md` §1.4의 오류 코드 문자열이 이 enum이다** |
| `Rational` | `Rational.kt` | 유리수. `ceilTo(unit)` 등 |

**`ErrorCode` enum을 고치면 `API.md` §1.4도 같은 PR에서 고쳐라.** 프론트가 그 문자열로
분기한다.

주의: `settle()`은 내부에서 `Validator.validate(input, CONFIRM)`을 이미 부른다.
저장 시점 검증이 필요하면 `SAVE` phase로 따로 호출해라.

> ⚠️ **`SettlementResult`를 그대로 응답 바디로 쓰지 마라.** 안에 `Rational` 타입이
> 그대로 박혀 있고(`rawTotal`, `foodShare`, `alcoholShare`, `amount`, `share`),
> `Rational`은 `numerator`/`denominator`가 public이라 Jackson이
> `{"numerator":123400,"denominator":3}` 같은 걸 뱉는다. `API.md`는 그 자리에 **정수 원**을
> 기대한다. 응답 DTO를 따로 만들고 표시용 금액(`Long`)으로 변환해서 내보내라 —
> 어떤 값이 반올림된 표시용이고 어떤 게 원본 유리수인지는 `CALC_RULES.md`가 정한다.

> `SettlementOutcome.Failure`를 HTTP로 바꾸는 경로는 **아직 코드에 없다.** 현재
> `GlobalExceptionHandler`의 `FieldErrorDetail`은 `{code, message, field}` 모양인데
> `API.md` §1.2는 정산 오류에 `{code, message, roundId, extraId, participantId}`를
> 요구한다. 확정/정산 엔드포인트를 만드는 PR에서 이 변환을 함께 설계하고,
> 어떤 모양으로 했는지 PR 설명에 적어라.

---

## 7. Liquibase changelog 작성 규칙

**파일 번호와 changeSet id 번호는 서로 다른 체계다. 여기서 가장 많이 실수한다.**

- 파일명은 파일 단위로 센다: `001-users.yaml` … `011-rename-groups.yaml`
- **`changeSet: id:`는 파일을 가로질러 전역으로 연속한다.** 한 파일에 changeSet이
  여러 개면 그만큼 번호가 나간다.

실제 현황:

```
009-group-type-and-lifecycle.yaml  →  id: 013, 014, 015   (3개)
010-table-korean-names.yaml        →  id: 016
011-rename-groups.yaml             →  id: 017          ← main의 마지막
─ 아래는 main에 병합된 적 없음. v3에서 쓰지 않음(ADR-019) ─
012-group-management-foundation.yaml → id: 018, 019, 020
013-restore-group-admin-remarks.yaml → id: 021
014-enforce-group-admin-definition.yaml → id: 022
```

**`012`~`014` 파일명과 changeSet `018`~`022`는 "소진됨"으로 취급한다.** `main`엔 없지만
누군가의 로컬 DB에 적용됐을 수 있다 — 같은 번호로 다른 내용을 만들면 그 DB에서
checksum이 깨진다. **따라서 다음 파일은 `015-*.yaml`이고, 그 안의 첫 changeSet id는
`023-...`이다.** 파일 번호를 id에 그대로 쓰면 이미 적용된 번호와 충돌한다.

그 외:

- `author: jeongsan` 으로 통일한다.
- **모든 테이블·컬럼에 설명을 단다.** 테이블은 `setTableRemarks`(또는 `createTable`의
  `remarks`), 컬럼은 `column: remarks:`. 기존 changelog를 열어보면 전부 그렇게 돼 있다 —
  "무엇"이 아니라 **"왜 이 컬럼이 있는지"**를 쓴다.
- `changeSet`에 `comment:`로 그 변경의 배경을 남긴다.
- **새 파일은 `db.changelog-master.yaml`에 `include`를 추가해야 실제로 적용된다.**
  이걸 빼먹으면 파일만 있고 아무 일도 안 일어난다.
- 열거형 컬럼에 DB `CHECK` 제약을 걸지 않는다 — `VARCHAR` + `remarks`로 허용값만
  적는다. 검증은 애플리케이션이 한다(`ERD.md` §4).

---

## 8. 애매하면 가정을 적어라

`docs/생각하고-개발하기.md`가 원본이다. 아래 중 하나라도 해당되면 **코드부터 쓰지 마라:**

- 에러 로그만 있고 원인 추정·시도해본 것이 없을 때
- 새 기능·구조·API인데 왜 이 방식이어야 하는지 이유가 없을 때
- 라이브러리·프레임워크·DB·아키텍처 선택에 비교한 흔적이 없을 때
- 기존 설계·컨벤션과 다른 방향인데 왜 바꾸는지 설명이 없을 때
- 요청이 두 가지 이상으로 해석 가능해 임의로 골라야 할 때

**반대로 아래는 그냥 바로 해라.** 여기까지 되물으면 규칙 자체가 무시당한다.

- 오탈자·포맷팅·네이밍·import 정리 같은 기계적 수정
- 이미 ADR이나 DEVLOG에 이유가 적혀 있는 작업의 연장선
- "이유 됐고 그냥 해줘"라고 명시했을 때

**질문은 PR당 최대 2개.** 아래 네 가지 중 상황에 맞는 것만 짧게 골라 쓴다.
① 정확히 어떤 문제를 푸는가 ② 다른 방법도 있나 ③ 왜 이걸 골랐나·트레이드오프는
④ 나중에 아쉬울 지점이 보이나

**너는 즉답을 못 받으므로 멈추는 대신 이렇게 한다:** 가장 그럴듯한 해석 하나를 고르고,
**되돌리기 쉬운 최소 구현**으로 짜고, PR 설명 맨 위에 이렇게 적는다.

```
## 확인 필요
- X를 A로 해석하고 구현했다. B로도 읽히는데, B라면 <파일>의 <함수>만 바꾸면 된다.
```

여러 해석을 다 구현하거나, 추측으로 큰 구조를 세우지 마라.

의미 있는 결정을 내렸으면 `DEVLOG.md` **맨 위에** 4줄 형식으로 추가한다 —
`## [YYYY-MM-DD] 제목` 다음에 **문제 / 고려한 대안 / 선택 이유·트레이드오프 / 아쉬운 점**.
기존 항목이 분량·톤의 기준이다. "아쉬운 점"을 비워두지 마라 — 그 칸이 이 로그의 핵심이다.

---

## 9. 코드 관례

**린터가 없다**(ktlint·detekt 모두 미도입). 스타일의 기준은 **주변 코드**뿐이니
새 파일을 만들기 전에 같은 패키지의 기존 파일을 먼저 읽어라.

- **주석은 "무엇"이 아니라 "왜"를 쓴다.** 이 저장소의 주석은 대부분 결정의 근거이거나
  함정 경고다. `// userId 를 가져온다` 같은 건 쓰지 마라. 한국어로 쓴다.
- **엔티티**: `class`(data class 아님), 생성자 프로퍼티에 기본값, 변경 가능한 필드는
  `var`, PK는 `val id: Long = 0`, enum은 `@Enumerated(EnumType.STRING)`,
  시각은 `Instant`(UTC), 날짜만이면 `LocalDate`.
  KDoc 첫 줄에 **"이 테이블의 진실은 어느 changelog인지"**를 적는다(기존 엔티티가 전부 그렇다).
- **DTO**: `data class`, `{Feature}Dto.kt`에 모아 둔다. 요청은 `@Valid` + Bean Validation.
- **예외**: `common/ApiException.kt` 계층을 쓴다(`NotFoundException`,
  `MalformedRequestException` 등). 새 오류가 필요하면 그 계층에 추가하고
  `API.md` §1.4에도 코드를 적어라.
- **트랜잭션**: 서비스 계층에 `@Transactional`, 읽기 전용은 `@Transactional(readOnly = true)`.
  `open-in-view: false`라 컨트롤러에서 지연 로딩이 안 된다 — 서비스 안에서 다 조립해라.

### 테스트

- 프레임워크는 **Kotest**(JUnit 아님). 파일명은 `*Spec.kt`.
- `core/src/test`에 5개 스펙이 있다 — `RationalSpec`, `SettlementSpec`, `ValidationSpec`,
  `InvariantSpec`, `TestFixtures`. 계산 로직을 건드렸으면 여기에 케이스를 추가한다.
- **`server/src/test`는 아직 없다.** 의존성(`spring-boot-starter-test`, Kotest)은 이미
  걸려 있으니 인프라는 준비돼 있다. **새 엔드포인트를 만들면 최소한
  성공 1건 + 실패(권한/검증) 1건은 테스트를 붙여라.** 없던 관례를 만드는 것이므로,
  첫 PR에서 어떤 방식(MockMvc / `@SpringBootTest`)을 골랐는지 PR 설명에 적어라.
- **테스트를 지우거나 비활성화해서 통과시키지 마라.** 깨졌으면 원인을 고치거나,
  못 고치겠으면 그대로 두고 PR에 적어라.

  > ⚠️ Kotest는 **테스트 이름 앞에 `!` 한 글자**만 붙이면 그 테스트를 건너뛰고
  > 빌드가 초록으로 통과한다(`"!T7 · …"`). diff에서 눈에 거의 안 띄는데
  > 배포 워크플로가 돌리는 `:core:test`까지 조용히 통과시킨다. 절대 쓰지 마라.

---

## 10. 작업 방식 (브랜치 + PR)

- 브랜치 접두어: `feat/`(새 기능) · `fix/`(버그 수정) · `refactor/`(리팩터링) ·
  `chore/`(설정·빌드·기타) · `docs/`(문서) · `test/`(테스트). 짧은 형태를 쓴다
  (`feature/`가 아니라 `feat/`). 예: `feat/짧은-설명`. 한글 가능.
- 커밋: 제목은 `type(scope): 무엇을`(한글), 본문은 **왜**에 집중한다 — 무엇을 바꿨는지는
  diff가 이미 보여준다. `git log --oneline -15`로 실제 스타일을 확인해라.
- **`main`에 직접 커밋하지 않는다.** `--no-verify`, force push, `rebase -i` 같은
  이력 조작을 하지 않는다.

> ⚠️ **PR에 CI가 없다.** 이 저장소의 유일한 워크플로는 `.github/workflows/deploy.yml`이고
> 트리거가 `push: [main]`이다. 즉 **네 PR을 자동으로 검증해 주는 게 아무것도 없다** —
> §11 체크리스트는 **네가 직접 돌리고 결과를 PR 설명에 붙여야** 의미가 있다.
>
> 그리고 **main 병합은 곧 OCI 운영 배포다**(스테이징 없음, `ADR-006`). 문서만 고친
> 경우는 `paths-ignore`로 배포가 안 돌지만, 코드가 섞이면 바로 나간다. 리뷰가
> 마지막 방어선이라는 뜻이다.

---

## 11. 병합 전 체크리스트

**직접 돌리고 결과를 PR 설명에 붙여라.** "통과했다"가 아니라 실제 출력 요약을 적는다.

- [ ] `./gradlew :core:test` — 계산 로직을 건드렸으면 필수
      (`core`는 경고도 오류다 — 컴파일 경고가 나면 빌드가 깨진다)
- [ ] `./gradlew :server:compileKotlin` 통과
- [ ] 새 엔드포인트를 만들었으면 테스트 추가(성공 1 + 실패 1)
- [ ] 새 컨트롤러 핸들러를 추가했다면 인증 가드 테스트(화이트리스트 외
      전부 `@LoginUser` 보유 검증, §4-7)가 통과함
- [ ] 스키마를 바꿨다면: 새 changelog + **전역 연속 changeSet id**(§7) +
      `db.changelog-master.yaml` include 추가 + 엔티티가 그걸 따라감(거꾸로 아님) +
      모든 컬럼에 `remarks`
- [ ] API를 바꿨다면 `docs/API.md`를 같은 PR에서 갱신. 스키마를 바꿨다면 `docs/ERD.md`도.
      **`API.md`는 버전 문서다** — 해당 절만 덮어쓰지 말고, 제목의 `v{n}`을 올리고
      상단에 `> **v{n} 변경(날짜)**` 블록 + 바뀐 절 표를 추가해라. **이전 버전 블록은 지우지 않는다.**
      프론트는 별도 저장소라 이 표만 보고 따라온다.
- [ ] 새 목록/집계 조회에 N+1이 없음
- [ ] `application-prod.yml`에 기본값을 추가하지 않았음
- [ ] 새 의존성을 임의로 추가하지 않았음
- [ ] 의미 있는 결정이 있었다면 `DEVLOG.md`에 기록 추가
- [ ] 시크릿(비밀번호·키·토큰)이 커밋에 없음

---

## 12. 로컬 실행

```bash
docker compose up -d              # MySQL 하나만 뜬다
./gradlew :server:bootRun         # application-local.yml 기본값으로 환경변수 없이 떠야 정상
./gradlew :core:test              # 계산 엔진 테스트만 (Spring 안 띄움, 수 초)
./gradlew build                   # 전체 빌드 + 테스트
```

`bootRun`은 환경변수 없이 떠야 한다 — DB 비밀번호 등 로컬 값은 `application-local.yml`에
기본값으로 박혀 있고, 그 값들은 `docker-compose.yml`에 이미 공개돼 있어 숨길 이유가 없다.
**안 뜨면 로컬 설정이 깨진 것이니 환경변수로 우회하지 말고 원인을 찾아라.**

카카오 로그인을 로컬에서 테스트하려면 카카오 개발자 콘솔에
`http://localhost:8080/api/v1/auth/kakao/callback`을 Redirect URI로 등록하고
`KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET`을 환경변수로 넣어야 한다 — 기본값이 빈 문자열이라
안 넣으면 서버는 뜨지만 카카오 콜백에서 실패한다.

---

## 13. 하지 않는 것

`docs/REQUIREMENTS.md` §12 "만들지 않는 것"이 기준이다. 특히:

- **모임(`Group`) 기능 전부** — 관리자·가입·검색·비밀번호·강퇴·차단·보관 (`ADR-019`)
- **정산 기록 보관·검색** — 완료 7일 뒤 삭제가 원칙이다 (`ADR-019`)
- **이의제기 절차** — 당사자 1:1 채팅방·협상·오프라인 조율. v3는 [아직 안 들어왔어요] 버튼 하나다
  (정산방 타임라인은 이의제기 절차가 아니다 — 참여자 전원이 보는 한 줄 대화 + 시스템 소식)
- **스푼을 돈·상품으로 바꾸는 기능** — 보여주기 전용이다
- 참여자에게 앱 설치를 요구하는 것 (`ADR-003` — 링크로 웹에서 끝까지 가능해야 한다)
- **채팅 서버를 별도 프로세스로 떼는 것** (`ADR-010` 원안 — 보류. 정산방 타임라인은 만들되
  `ADR-014`대로 모놀리스 안 WebSocket으로 만든다. 분리는 재검토 조건이 온 뒤에)
- MSA·k3s 전환 (`ADR-013` 보류 — `ADR-014` 재검토 조건 전엔 손대지 않는다)
- 계산 결과를 DB에 저장하는 모든 형태 (`ADR-005`)
- Spring Security 도입 (§4-6)
- `ddl-auto`를 켜서 스키마 문제를 우회하는 것 (§4-4)

---

## 14. 백엔드 시니어의 개발·검증 기준

### 작업 시작과 완료의 근거

- 원격을 갱신해 문서 기준 commit을 확인한다. 낡은 작업 브랜치 문서로 최신 결정을 판단하지 않는다.
  모든 열린 FC를 읽고 **확인한 번호 / 이번 반영 / 관련 후속·보류 이유**를 PR에 적는다.
  채팅·연결 보고에 서버 영향이 있는데 FC가 없으면 FC 초안을 남겨 누락을 드러낸다.
  보고의 제안과 CTO가 확정한 결정을 구분하고 미결을 구현으로 확정하지 않는다.
- 합의된 제품 규칙과 기존 ADR 안에서 구현·발견한 결함 수정·검증·PR 작성까지 자율적으로 진행한다.
  새 제품 결정·새 의존성·큰 구조 변경은 §8대로 가정·대안·영향을 남긴다. 플랫폼이 요구하는 실행
  승인은 별개이며, 기존 승인 범위를 재사용하고 불필요하게 승인 요청을 나누지 않는다.
- **구현 → 자체 점검 → 위험에 맞는 테스트 → PR → 리뷰·스터디 보존**을 한 작업으로 취급한다.
  리뷰·학습·병합·운영 배포 완료는 각각 별도로 기록한다. 검증 실패·미실행을 성공으로 쓰지 않는다.

### 구현 후 반드시 다시 볼 위험

| 영역 | 점검할 질문 | 중요한 변경의 검증 근거 |
|---|---|---|
| 중복·재시도 | 같은 요청이 반복되면 송금·확정·알림·스푼이 중복 생성되는가? 요청 키가 같고 내용이 다를 때 어떻게 거절하는가? | 반복 요청·동시 중복 요청 테스트, DB 유일 제약·멱등 처리 |
| 동시성·트랜잭션 | 마지막 응답·정산·되돌리기·탈퇴가 겹쳐도 한 상태로 끝나는가? 잠금 순서가 일관되며 중간 실패는 롤백되는가? | 경합 재현·롤백 테스트, 실제 MySQL 검증 필요 여부 |
| 가용성·장애 복구 | 외부 호출의 시간 제한·재시도 횟수와 간격은 유한한가? 프로세스가 중간에 끝나도 복구 가능한가? | 타임아웃·실패·재시도 테스트, 복구 절차 |
| 권한·정보 노출 | 로그인한 사람도 다른 술자리·단위를 바꿀 수 없는가? 공개 응답·로그에 계좌·토큰·개인정보가 새는가? | 미인증·다른 사용자·다른 단위의 거절 테스트, 응답·로그 점검 |
| 데이터·계산 | 합계·음수·범위·오버플로·UTC·확정 스냅샷 조건이 깨지는가? | 기대 결과·불변 조건·경계값 테스트 |
| 조회·자원 | N+1·무제한 조회·긴 잠금·불필요한 외부 호출로 부하가 커지는가? | 쿼리 수·범위·잠금 경계 점검, 필요할 때 실행 계획·부하 검증 |
| 배포·호환성 | 기존 DB·기존 앱에 적용 가능한가? 오류 코드·DTO·마이그레이션·운영 설정이 맞는가? | API 필드 대조, 필요할 때 업그레이드·부팅·실패 복구 검증 |

정산·송금 상태·권한·인증·탈퇴·자동 지급처럼 실패 시 금액·데이터·사용자 권리에 영향을 주는 변경은
성공 경로만 테스트하지 않는다. 실제로 가능한 중복·경합·권한 실패·부분 실패를 골라 **테스트 코드로 남기고 실행**한다.
DB 잠금·제약의 정확성을 주장할 때 Mock만으로 검증했다고 쓰지 않는다. 영향 없는 점검 항목은
해당 없음과 이유를 적고, 모든 항목마다 형식적인 테스트나 새 인프라를 만들지 않는다.

### Java·Kotlin과 이름

- 같은 업무 기능의 정책·모델·계산은 `.java`·`.kt`에 **독립 구현**한다. 한쪽이 다른 쪽 구현을 호출해
  비교 테스트를 통과하는 방식은 쓰지 않는다. Spring URL·Bean을 중복 등록하지 않도록 학습 Java
  구현은 별도 패키지에 둔다. 공유 HTTP/JDBC 인프라는 PR에서 구분한다.
- Java·Kotlin 결과 비교와 별도로 올바른 기대값·업무 불변 조건을 테스트한다. 두 구현이 같은 실수를
  하는 경우도 잡아야 한다. 금액뿐 아니라 실패 코드·정렬·상태 전이도 계약에 해당하면 비교한다.
- `s`, `p`, `r`처럼 역할을 숨긴 축약명 대신 `participant`, `settlementUnit`, `retryAttemptCount`처럼
  의미가 드러나는 이름을 쓴다. 새 코드·수정 범위의 매개변수, 지역 변수, 람다에도 적용한다.
  명확한 수학 기호·관용적 타입 변수는 이유가 있으면 허용하고, 관계없는 기존 코드를 일괄 변경하지 않는다.

### SOLID를 실제 변경의 검토 기준으로 사용

| 원칙 | 이번 프로젝트에서 확인할 관점 |
|---|---|
| SRP · 단일 책임 | 계산 규칙·권한 정책·외부 인증·저장 처리가 서로 다른 이유로 바뀌는데 한 객체에 얽혀 있지 않은가? |
| OCP · 개방 폐쇄 | 실제로 반복되는 정책 변경을 격리했는가? 예상만으로 확장 구조를 미리 만들지는 않았는가? |
| LSP · 리스코프 치환 | 같은 계약의 구현을 바꿔도 입력·결과·실패 조건을 지키는가? 상속·대체 구현이 없으면 적용하지 않는다. |
| ISP · 인터페이스 분리 | 호출자가 쓰지 않는 기능·의존성까지 강제하는 인터페이스가 있는가? |
| DIP · 의존성 역전 | 핵심 정책·계산이 Spring·DB·외부 제공자 구현에 묶여 독립 검증이 어려운가? |

다섯 원칙을 모든 PR에 형식적으로 채우지 않는다. 실제 문제가 있는 원칙만 **파일·함수·변경 이유·검증**과
연결한다. 클래스마다 인터페이스를 만들거나 패턴을 늘리는 규칙이 아니다. 단순함·응집도·변경 비용과
기존 모놀리스·feature 패키지·순수 core 결정이 우선이다.
원칙의 출처는 Robert C. Martin의 [SOLID 소개](https://cleancoder.com/files/solid.md),
[단일 책임](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html),
[의존성 방향](https://blog.cleancoder.com/uncle-bob/2016/01/04/ALittleArchitecture.html)을 참고한다.

### Effective Java·언어 목차를 참고하는 순서

원격 main의 `STUDY_POLICY.md`·`STUDY_CATALOG.md`를 확인하고 **업무 문제 → 해당 원칙·목차 → 구현·검증 →
확정 diff의 학습 위치** 순으로 연결한다. 카탈로그는 목차·키워드이며 책 본문을 읽었다는 근거가 아니다.
번호·제목·페이지·강의는 카탈로그 그대로 쓰고, 없는 것은 만들어내지 않는다. Java 설계는 Java 자료,
Kotlin 문법·타입 동작은 Kotlin 자료에 먼저 대조한다. Effective Java를 Kotlin 문법 목차 대신 쓰지 않는다.

다음은 개발 점검 때 **해당 변경이 있을 때만** 찾을 항목이다. 모든 PR의 필수 학습 목록이 아니다.

| 카탈로그 ID | 확인한 제목 | 연결할 변경 |
|---|---|---|
| EJ-05 | 자원을 직접 명시하지 말고 의존 객체 주입을 사용하라 | 외부 의존 객체 구성·교체·독립 검증 |
| EJ-15 | 클래스와 멤버의 접근 권한을 최소화하라 | 변경 가능한 상태·내부 구현 노출 |
| EJ-17 | 변경 가능성을 최소화하라 | 공유 상태·입력·확정 결과의 가변성 |
| EJ-18 | 상속보다는 컴포지션을 사용하라 | 재사용을 위한 상속·위임 선택 |
| EJ-49 | 매개변수가 유효한지 검사하라 | 경계값·업무 입력 검증 |
| EJ-50 | 적시에 방어적 복사본을 만들라 | 외부의 가변 객체·컬렉션 수신·반환 |
| EJ-76 | 가능한 한 실패 원자적으로 만들라 | 중간 실패와 상태 복구·롤백 |
| EJ-78 | 공유 중인 가변 데이터는 동기화해 사용하라 | 여러 요청이 공유하는 메모리 상태 |
| EJ-79 | 과도한 동기화는 피하라 | 잠금 범위·잠금 중 외부 호출·교착 위험 |

EJ-78의 메모리 동기화와 DB 트랜잭션 격리·행 잠금은 같은 기능이 아니다. DB·Spring 검증은
기술별 관점으로 별도 기록한다. 책 원칙만으로 실제 DB 경합이 검증됐다고 판단하지 않는다.
스터디는 실제 PR base/head의 diff를 고정해 **클래스·함수·줄·정확한 목차·지금 볼 관점**을 남긴다.
정책·문서 변경만 있는 PR은 이를 Java/Kotlin 구현 학습으로 꾸미지 않는다. 자동 리뷰와 자체 점검도 구분한다.
