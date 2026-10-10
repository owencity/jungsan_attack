# AGENTS.md — 정산어택 백엔드 (jungsan_attack)

이 문서는 이 저장소에서 코드를 쓰는 에이전트(Codex)를 위한 것이다.
**대상은 `server`·`core` 모듈, 즉 백엔드뿐이다.** 참여자 웹 프론트엔드는 별도
저장소(`profile`)에 있고 사람이 직접 작업한다 — 이 저장소 안에 프론트 코드는 없다.

## 역할 관계

- **CTO(사용자)** — 방향을 정하고 우선순위를 매긴다.
- **시니어(Claude)** — PR을 리뷰하고, 애매한 요청을 구체화하고, 최종 병합 여부를 판단한다.
- **Codex(너)** — 브랜치에서 구현한다. **`main`에 직접 커밋·푸시하지 않는다.**
  항상 새 브랜치 → PR. 병합은 CTO·시니어가 리뷰한 뒤에만 한다.

**너는 대화형으로 즉답을 못 받는다.** 애매한 지점을 만나면 추측으로 밀어붙이지 말고,
가정을 명시하고 되돌리기 쉽게 짠 뒤 PR 설명에 질문을 남겨라
(→ [§8 애매하면 가정을 적어라](#8-애매하면-가정을-적어라)).

---

## 1. 무엇을 만드는가

**정산어택** — 술자리 정산 서비스. 총무 혼자 정산 몫을 입력하지 않는다. 참여자
각자가 링크로 들어와 "몇 차까지 있었는지 · 술을 마셨는지"를 체크하면, 서버가
차수별로 계산해서 **금액과 근거를 함께** 보여준다. N빵이 아니다.

MVP 플로우 (채팅은 보류):

```
로그인(카카오)
  → 총무: 모임(Group) 생성 — 번개(FLASH, 1회성) or 주기(RECURRING, 지속)
  → 참여자: 공유 링크로 모임 참여
  → 모임 안에서 술자리(Gathering)가 열리고, 차수·기타항목을 총무가 입력
  → 참여자가 각자 출석/음주 체크
  → 확정(2단계: 미리보기 → 수락) → 결과·입금 상태 확인
```

**용어 — `Group` = 모임, `Gathering` = 술자리.** 모임 하나가 술자리 여러 개를 담고,
번개(FLASH)만 예외로 정확히 하나만 담는다.

단, **`gatherings.group_id`는 nullable이다.** 모임 없이 술자리 하나만 만드는 경로가
스키마상 열려 있다(`ADR-009`가 기존 `Gathering` 구조를 안 바꾸기로 한 결과). 코드를
쓸 때 `group_id`가 항상 있다고 가정하지 마라.

제품 정의 전체는 [`docs/SPEC.md`](docs/SPEC.md), 특히 **§9 "만들지 않는 것"** —
거기 적힌 걸 구현하면 범위 위반이다.

---

## 2. 읽는 순서

1. [`docs/SPEC.md`](docs/SPEC.md) — 제품 정의, 도메인 모델, 화면, 상태 전이
2. [`docs/ERD.md`](docs/ERD.md) — 스키마 요약. **진실은 `server/src/main/resources/db/changelog/`의
   Liquibase YAML이다** — ERD.md는 그걸 사람이 읽기 좋게 옮긴 것이다. 스키마를 바꿀 땐
   changelog를 먼저 고치고 ERD.md를 그에 맞춰 갱신한다.
3. [`docs/CALC_RULES.md`](docs/CALC_RULES.md) — 계산 규칙 + 검증된 테스트 케이스.
   **`core` 모듈 작업은 여기서 시작한다.**
4. [`docs/API.md`](docs/API.md) — 엔드포인트·요청·응답·오류 코드 계약.
   **`server` 모듈 작업은 여기서 시작한다.**
5. [`docs/ADR/000-index.md`](docs/ADR/000-index.md) — 왜 이렇게 정했는지.
   특히 [001](docs/ADR/001-rational-not-bigdecimal.md)(BigDecimal 금지),
   [005](docs/ADR/005-no-stored-settlement.md)(계산 결과 미저장),
   [006](docs/ADR/006-single-vm-no-kubernetes.md)(단일 VM 배포),
   [009](docs/ADR/009-group-persistent-membership.md)(Group을 얹은 방식),
   [014](docs/ADR/014-monolith-first-feature-package.md)(모놀리스·feature 패키지 — **가장 최근 결정**)
6. [`DEVLOG.md`](DEVLOG.md) — 최근 결정 이력
7. [`docs/DEPLOY.md`](docs/DEPLOY.md) — 배포 절차 (배포를 건드릴 때만)

> **ADR-013(MSA)과 ADR-010(채팅 분리)은 "보류"이지 "폐기"가 아니다.** 설계는 살아
> 있지만 **지금 코드는 014(모놀리스) 기준으로 쓴다.** 저장소 안에 "ADR-013의 REST API
> 서비스" 같은 오래된 주석이 남아 있는데(예: `GatheringController.kt`), 013이 유효하던
> 시점에 쓴 것이고 지금은 014가 대체했다. **보류된 ADR에 설계가 있다는 이유로 구현하지 마라.**

---

## 3. 지금 상태 (2026-08-27 스냅샷)

이 표는 며칠이면 낡는다. 작업 전에 `git log --oneline -10`과 실제 컨트롤러 파일로 교차 확인해라.

| 영역 | 상태 |
|---|---|
| `core` 계산 엔진 | ✅ 완성. `Settlement.settle()` 동작, Kotest 5개 스펙 통과 |
| DB 스키마 | ✅ changelog 011까지 적용. 실제 MySQL 8.4에서 실행 검증됨 |
| 카카오 로그인 | ✅ 동작. OAuth2 → httpOnly JWT 쿠키(`jeongsan_token`) → `GET /auth/me` |
| `Group` (모임) | ✅ `GET/POST /api/v1/groups`, `GET /api/v1/groups/{id}`. FLASH 생성 시 술자리 1개 동시 생성 |
| 모임 가입 `/gr/{token}` | ❌ `API.md` §3-b.4에 계약만 있고 컨트롤러 없음 |
| 멤버 제거 `DELETE /groups/{id}/members/{userId}` | ❌ 계약만 있음 |
| `Gathering` (술자리) | 🚧 `GET /api/v1/gatherings`만 존재. **인증 안 걸림 + 응답 필드 4개뿐**인 옛 뼈대 코드 — 계약(`API.md` §3.1)에 한참 못 미친다 |
| 차수·기타항목·출석 체크·확정·정산·입금 | ❌ 엔드포인트 없음. 프론트는 전부 목데이터로 동작 중 |
| 실시간 채팅 | ⏸️ 보류(`ADR-010`) |
| 배포 | 🚧 GitHub Actions → OCI SSH 파이프라인 작성·로컬 검증 완료. **실제 배포는 아직 안 함** |

**컨트롤러는 지금 3개뿐이다** — `AuthController`, `GroupController`, `GatheringController`.
그 외 `API.md`에 적힌 모든 엔드포인트는 아직 없다.

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
8. **`application-prod.yml`에 기본값을 넣지 않는다.** 환경변수가 없으면 부팅이 즉시
   실패해야 한다(fail-fast). 빈 값으로 떠서 나중에 조용히 깨지는 게 훨씬 나쁘다.
9. **N+1을 만들지 않는다.** 목록에 여러 항목이 있으면 항목마다 쿼리를 날리지 말고
   `IN :ids` + `GROUP BY` 배치로 한 번에 모은다
   (`GroupRepository.kt`의 `countByGroupIds`, `GroupService.listMyGroups` 참고).
10. **새 의존성을 임의로 추가하지 않는다.** 이 저장소는 스택 선택을 ADR로 관리한다.
    라이브러리가 필요하면 추가하지 말고 PR 설명에 "무엇이·왜 필요한지"를 적어 물어라.

---

## 5. 자주 걸리는 함정

실제로 걸렸던 것들이다.

- **`GROUPS`는 MySQL 예약어다.** `USERS`는 통했는데 `GROUPS`는 복수형도 예약어라서
  (윈도우 함수 프레임용) 테이블을 `user_groups`로 리네임해야 했다(changelog `011`).
  엔티티는 `@Table(name = "user_groups")`이지만 클래스명은 `Group` 그대로다.
  **테이블명을 새로 지을 때 "복수형이니 안전하겠지"라고 가정하지 말고
  `SELECT * FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD='...' AND RESERVED=1`로 확인해라.**
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
011-rename-groups.yaml             →  id: 017
```

**따라서 다음 파일은 `012-*.yaml`이고, 그 안의 첫 changeSet id는 `018-...`이다.**
파일 번호를 id에 그대로 쓰면 이미 적용된 번호와 충돌한다.

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
- **테스트를 지우거나 `@Disabled`로 막아서 통과시키지 마라.** 깨졌으면 원인을 고치거나,
  못 고치겠으면 그대로 두고 PR에 적어라.

---

## 10. 작업 방식 (브랜치 + PR)

- 브랜치: `feat/짧은-설명`, `fix/짧은-설명`, `docs/짧은-설명`. 한글 가능.
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
- [ ] 스키마를 바꿨다면: 새 changelog + **전역 연속 changeSet id**(§7) +
      `db.changelog-master.yaml` include 추가 + 엔티티가 그걸 따라감(거꾸로 아님) +
      모든 컬럼에 `remarks`
- [ ] API를 바꿨다면 `docs/API.md`를 같은 PR에서 갱신. 스키마를 바꿨다면 `docs/ERD.md`도.
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

`docs/SPEC.md` §9와 동일하다. 특히:

- 참여자용 네이티브 앱 (`ADR-003` — 참여자는 웹에 남는다)
- 실시간 채팅 (`ADR-010` 보류 — 재검토 조건 전엔 손대지 않는다)
- MSA·k3s 전환 (`ADR-013` 보류 — `ADR-014` 재검토 조건 전엔 손대지 않는다)
- 계산 결과를 DB에 저장하는 모든 형태 (`ADR-005`)
- Spring Security 도입 (§4-6)
- `ddl-auto`를 켜서 스키마 문제를 우회하는 것 (§4-4)
