# AGENTS.md — 정산어택 백엔드 (jungsan_attack)

이 문서는 이 저장소에서 코드를 쓰는 에이전트(Codex)를 위한 것이다.
**이 문서의 대상은 `server`·`core` 모듈, 즉 백엔드뿐이다.** 참여자 웹
프론트엔드는 별도 저장소(`profile`)에서 사람이 직접 작업한다 — 여긴
건드리지 않는다.

## 역할 관계

- **CTO(사용자)** — 방향을 정하고 우선순위를 매긴다.
- **시니어(Claude)** — PR을 리뷰하고, 애매한 요청을 구체화하고, 최종 병합 여부를 판단한다.
- **Codex(너)** — 브랜치에서 구현한다. **`main`에 직접 커밋·푸시하지 않는다.**
  항상 새 브랜치 → PR. 병합은 CTO·시니어가 리뷰한 뒤에만 한다.

애매한 지점을 만나면 추측으로 코드를 쓰지 말고 PR 설명(또는 커밋 메시지)에
질문을 적어 남긴다 — 아래 [애매하면 먼저 묻는다](#애매하면-먼저-묻는다) 참조.

---

## 1. 무엇을 만드는가

**정산어택** — 술자리 정산 앱. 총무 혼자 정산 몫을 입력하지 않는다. 참여자
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

**용어 주의 — `Group` = 모임, `Gathering` = 술자리.** 모임 하나가 술자리
여러 개를 담는다. 번개(FLASH)만 예외로 정확히 하나만 담는다.

제품 정의 전체는 [`docs/SPEC.md`](docs/SPEC.md), 특히 **§9 "만들지 않는 것"** —
거기 적힌 걸 구현하면 범위 위반이다.

---

## 2. 읽는 순서

`docs/00-README.md`를 최신화한 버전이다. 이 순서대로 본다.

1. [`docs/SPEC.md`](docs/SPEC.md) — 제품 정의, 도메인 모델, 화면, 상태 전이
2. [`docs/ERD.md`](docs/ERD.md) — 스키마. **단, 진실은 `server/src/main/resources/db/changelog/`의
   Liquibase YAML이다** — ERD.md는 그걸 사람이 읽기 좋게 옮긴 요약이다.
   스키마를 바꿀 땐 changelog를 먼저 고치고 ERD.md를 그에 맞춰 갱신한다.
3. [`docs/CALC_RULES.md`](docs/CALC_RULES.md) — 계산 규칙 + 검증된 테스트 케이스.
   **`core` 모듈 작업은 여기서 시작한다.**
4. [`docs/API.md`](docs/API.md) — 엔드포인트·요청·응답·오류 코드 계약.
   **`server` 모듈 작업은 여기서 시작한다.** 오류 코드는 `core`의
   `Validation.kt`의 `ErrorCode` enum과 동기화돼야 한다 — 한쪽을 고치면
   반드시 §1.4도 같이 고친다.
5. [`docs/ADR/000-index.md`](docs/ADR/000-index.md) — 왜 이렇게 정했는지.
   특히 [001](docs/ADR/001-rational-not-bigdecimal.md)(계산에 BigDecimal 금지),
   [005](docs/ADR/005-no-stored-settlement.md)(계산 결과 미저장),
   [006](docs/ADR/006-single-vm-no-kubernetes.md)(단일 VM 배포),
   [009](docs/ADR/009-group-persistent-membership.md)(Group을 얹은 방식),
   [014](docs/ADR/014-monolith-first-feature-package.md)(모놀리스·feature 패키지 — **가장 최근 결정, 013을 뒤집음**)는 먼저 읽는다.
6. [`DEVLOG.md`](DEVLOG.md) — 최근 결정 이력. 최신 항목부터.

> **013과 010은 "보류"이지 "폐기"가 아니다.** MSA 전환(013)과 채팅 분리(010)
> 설계는 그대로 남아 있고, 재검토 조건이 오면 그때 꺼내 쓴다. **지금 코드를
> 쓸 때는 014(모놀리스)를 기준으로 한다** — 저장소 안에 "ADR-013의 REST API
> 서비스"라고 적힌 오래된 주석이 몇 군데 있는데, 그건 013이 아직 유효하던
> 시점에 쓴 것이고 지금은 014가 그걸 대체했다.

---

## 3. 지금 상태 (2026-08-27 스냅샷 — 정확한 최신 상태는 `git log`와 `DEVLOG.md`를 봐라)

| 영역 | 상태 |
|---|---|
| 카카오 로그인 | ✅ 동작. OAuth2 → httpOnly JWT 쿠키 → `/auth/me` |
| `Group` (모임) | ✅ `GET/POST /groups`, `GET /groups/{id}` 동작. FLASH 생성 시 술자리 1개 동시 생성 |
| 모임 가입 (`/gr/{token}`) | ❌ API.md §3-b.4에 계약만 있고 컨트롤러 없음 |
| `Gathering` (술자리) | 🚧 `GET /gatherings`만 있고 **인증 안 걸림, 필드 최소치**(옛 뼈대 코드) |
| 차수·기타항목·출석 체크·확정/정산 | ❌ 엔드포인트 없음. 프론트는 전부 목데이터로 동작 중 |
| 실시간 채팅 | ⏸️ 보류(ADR-010) |
| 배포 | 🚧 GitHub Actions → OCI SSH 파이프라인 작성·로컬 검증 완료, 실제 배포는 아직 |

이 표는 며칠만 지나도 낡는다 — 작업 시작 전에 실제 컨트롤러 파일과
`git log --oneline -10`으로 교차 확인해라.

---

## 4. 절대 규칙

어기면 안 되는 것들이다. 바꾸고 싶으면 코드를 먼저 쓰지 말고 PR에서 먼저 문제 제기한다.

- **계산은 `core` 모듈의 순수 함수만 한다.** `server`는 `Attendance` 등을
  모아 `core`에 넘기고 결과를 그대로 응답한다 — 재계산하거나 결과를 DB에
  저장하지 않는다(`ADR-005`). 새 계산 로직을 `server`에 직접 짜면 안 된다.
- **금액 누적에 `BigDecimal`을 쓰지 않는다.** 유리수(Rational)로만 한다.
  실측 근거: 랜덤 30,000건 중 210건(0.70%)에서 `BigDecimal` 누적이 틀린
  금액을 냈다(`CALC_RULES.md` §2.1, `ADR-001`).
- **스키마는 Liquibase changelog로만 바꾼다.** `ddl-auto: none`이 고정이라
  엔티티를 고쳐도 테이블은 안 바뀐다 — 순서는 항상 changelog 먼저, 엔티티는
  그걸 따라간다. 새 changelog 파일은 `db.changelog-master.yaml`에 `include`를
  추가해야 실제로 적용된다(잊기 쉽다).
- **패키지는 layer가 아니라 feature로 나눈다** — `server/group`, `server/gathering`,
  `server/user`, `server/common`, `server/config`. `controller/`·`service/`·
  `repository/` 최상위 패키지를 새로 만들지 않는다(`ADR-014`).
- **인증은 httpOnly JWT 쿠키뿐이다.** `Authorization: Bearer` 헤더나
  `localStorage` 토큰 저장 방식을 새로 만들지 않는다. `api.jungsan.devkdk.com`과
  `jungsan.devkdk.com`이 같은 등록 도메인(`devkdk.com`)을 공유해서
  `SameSite=Lax`가 same-site로 동작한다는 전제가 깔려 있다 — 도메인 구조를
  바꾸는 변경은 이 전제를 다시 검토해야 한다.
- **`application-prod.yml`에는 기본값을 넣지 않는다.** 운영 설정은 환경변수가
  없으면 그냥 실패해야 한다(fail-fast) — 로컬처럼 편의 기본값을 넣으면 운영에서
  값이 안 채워졌는데도 조용히 뜨는 사고가 난다.
- **N+1을 만들지 않는다.** 목록 하나에 여러 항목이 있으면 항목마다 쿼리를
  날리지 말고 `IN :ids` + `GROUP BY` 배치 쿼리로 한 번에 모은다
  (`GroupRepository.countByGroupIds` 참고 패턴).

---

## 5. 자주 걸리는 함정

이번 개발 중에 실제로 걸렸던 것들이다. 알고 있으면 안 걸린다.

- **`GROUPS`는 MySQL 예약어다.** `USERS`는 통했는데 `GROUPS`는 복수형도
  예약어라서(윈도우 함수 프레임용) `groups` 테이블을 실제로 `user_groups`로
  리네임해야 했다(`ERD.md` §0, changelog `011`). **테이블명을 새로 지을 때
  "복수형이니 안전하겠지"라고 가정하지 말고,
  `SELECT * FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD = '...' AND RESERVED = 1`로
  직접 확인해라.**
- **Kotlin의 non-null `Long`은 primitive `long`으로 컴파일된다.** `@LoginUser`
  같은 `HandlerMethodArgumentResolver`에서 파라미터 타입을 비교할 때
  `Long::class.java` 하나만 보면 놓친다. `Long::class.javaPrimitiveType`과
  `Long::class.javaObjectType` 둘 다 봐야 한다.
- **`HttpMessageNotReadableException`을 잡아두지 않으면** 깨진 JSON 요청이
  400이 아니라 500(`INTERNAL_ERROR`)으로 나간다. `GlobalExceptionHandler`에
  이미 핸들러가 있다 — 지우지 마라.
- **응답 DTO가 API.md 계약을 실제로 만족하는지 PR 올리기 전에 대조해라.**
  `GET /groups/{id}`가 한동안 멤버 닉네임 없이 `userId`만 내려간 적이 있다 —
  계약과 구현이 슬쩍 갈라지는 건 컴파일 에러가 안 나서 리뷰에서만 잡힌다.

---

## 6. 작업 방식 (브랜치 + PR)

- 브랜치: `feat/짧은-설명`, `fix/짧은-설명` 형식. 한글 설명 가능.
- 커밋 메시지: 기존 로그 스타일을 따른다 — `git log --oneline -15`로 확인.
  제목은 `type(scope): 무엇을`, 본문은 **왜**에 집중한다(무엇을 바꿨는지는 diff가
  이미 보여준다).
- PR 설명에 넣을 것: 무엇을·왜 바꿨는지, 관련 ADR/DEVLOG 항목, 테스트 여부,
  스키마를 바꿨다면 어느 changelog 파일인지.
- **`main`에 직접 커밋하지 않는다.** `--no-verify`, `--force` 푸시, `git rebase -i`
  같은 이력 조작을 하지 않는다.

## 7. 애매하면 먼저 묻는다

`docs/생각하고-개발하기.md`가 원본이다(`CLAUDE.md`도 같은 문서를 요약해서 쓴다).
아래 중 하나라도 해당되면 **코드부터 쓰지 말고 PR 설명이나 커밋 메시지에
질문을 먼저 적어 남긴다** — Codex는 채팅으로 즉답을 못 받으므로, 짐작으로
구현부터 끝내지 말고 "이렇게 두 가지로 해석되는데 A로 가정하고 구현했다,
아니면 알려달라" 식으로 **가정을 명시하고 되돌리기 쉽게** 짠다.

- 에러 로그만 있고 원인 추정·시도해본 것이 없을 때
- 새 기능·구조·API인데 왜 이 방식이어야 하는지 이유가 없을 때
- 라이브러리·프레임워크·DB·아키텍처 선택에 비교한 흔적이 없을 때
- 기존 설계·컨벤션과 다른 방향인데 왜 바꾸는지 설명이 없을 때
- 요청이 두 가지 이상으로 해석 가능해서 임의로 골라야 할 때

반문을 거쳐 내린 결정(혹은 스스로 가정하고 진행한 결정)은 `DEVLOG.md`에
4줄 형식(문제 / 고려한 대안 / 선택 이유·트레이드오프 / 아쉬운 점)으로 남긴다.

## 8. 병합 전 체크리스트 (시니어가 이걸로 리뷰한다)

- [ ] `./gradlew :core:test` — 계산 로직을 건드렸으면 필수, T1~T11 포함해서 통과
- [ ] `./gradlew :server:compileKotlin` (또는 관련 모듈) 컴파일 통과
- [ ] 스키마를 바꿨다면: 새 Liquibase changelog + `db.changelog-master.yaml`
      include 추가 + 엔티티가 그걸 따라감(거꾸로 아님)
- [ ] API를 바꿨다면: `docs/API.md`가 같은 PR 안에서 함께 갱신됨
- [ ] 새 목록/집계 조회에 N+1이 없음
- [ ] `application-prod.yml`에 기본값을 추가하지 않았음
- [ ] 의미 있는 결정을 내렸다면 `DEVLOG.md`에 4줄 기록 추가
- [ ] 시크릿(비밀번호·키)이 커밋에 없음

## 9. 로컬 실행

```bash
docker compose up -d              # MySQL 하나만 뜬다 (docker-compose.yml)
./gradlew :server:bootRun         # application-local.yml 기본값으로 바로 뜬다.
                                   # 환경변수 없이 떠야 정상 — 안 뜨면 로컬 설정이 깨진 것
./gradlew :core:test              # 계산 엔진 테스트만 (Spring 안 띄움, 수 초 내 완료)
```

카카오 로그인을 로컬에서 테스트하려면 카카오 개발자 콘솔에
`http://localhost:8080/api/v1/auth/kakao/callback`을 Redirect URI로 등록하고
`KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET` 환경변수를 넣어야 한다 — 안 넣으면
로그인 버튼 자체는 뜨지만 카카오 콜백에서 실패한다.

## 10. 하지 않는 것

`docs/SPEC.md` §9와 동일하다. 특히:

- 참여자용 네이티브 앱 (`ADR-003` — 참여자는 웹에 남는다)
- 실시간 채팅 구현 (`ADR-010` — 보류, 재검토 조건 오기 전엔 손대지 않는다)
- MSA·k3s 전환 (`ADR-013` — 보류, `ADR-014` 재검토 조건 오기 전엔 손대지 않는다)
- 계산 결과를 DB에 저장하는 모든 형태 (`ADR-005`)
