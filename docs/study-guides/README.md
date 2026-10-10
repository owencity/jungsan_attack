# PR 리뷰·스터디 기록

최신 Apple 설정 발급 대기 변경은 [가이드](apple-provider-v1/guide.md) · [diff](apple-provider-v1/changes.patch) · [리뷰](../pr-reviews/2026-10-10-apple-provider/README.md) · [진도](progress/apple-provider.md)를 본다.

현재 출시 통합 기준은 [PR #27](https://github.com/owencity/jungsan_attack/pull/27)이다.
아래 전체 조사 표는 2026-10-10 병합 전 스냅샷이며 이후 상태와 구분한다.
실제 리뷰 댓글과 재검토 결과는 [출시 리뷰 기록](../pr-reviews/2026-10-10-release/README.md)에 보존한다.
최신 운영 호스트 정정 [PR #28](https://github.com/owencity/jungsan_attack/pull/28)은 [고정 가이드](production-domain-v1/guide.md) · [diff](production-domain-v1/changes.patch) · [진도](progress/production-domain.md) · [리뷰](../pr-reviews/2026-10-10-production-domain/README.md)를 본다.
과거 추가 운영 DNS 구현은 [고정 가이드](release-dns-v1/guide.md) · [진도](progress/release-dns.md)를 본다.

개발 완료 → PR → 자동 리뷰/스터디 생성 → 사람이 나중에 diff를 읽고 학습하는 흐름이다.
구현 완료와 PR 병합·리뷰 완료·학습 완료는 별도로 기록한다. 과거 가이드는 새 main 코드로 덮어쓰지 않는다.

| 구현 | 가이드 | 코드 기준 | 학습 |
|---|---|---|---|
| backend-v4 같은 술자리·총무별 독립 정산 | [guide.md](backend-v4-v1/guide.md) · [review.md](backend-v4-v1/review.md) | [manifest.json](backend-v4-v1/manifest.json)의 base/head와 [changes.patch](backend-v4-v1/changes.patch) | [progress.md](backend-v4-v1/progress.md) |
| auth-release Apple·앱 인증·로그아웃·탈퇴 | [guide.md](auth-release-v1/guide.md) · [review.md](auth-release-v1/review.md) | [manifest.json](auth-release-v1/manifest.json)의 고정 base/head와 [changes.patch](auth-release-v1/changes.patch) | [progress.md](auth-release-v1/progress.md) |
| [PR #21 CI·배포 실행 권한](https://github.com/owencity/jungsan_attack/pull/21) | [guide.md](actions-ci-v1/guide.md) · [review.md](actions-ci-v1/review.md) | [manifest.json](actions-ci-v1/manifest.json) · [changes.patch](actions-ci-v1/changes.patch) | [progress.md](actions-ci-v1/progress.md) |

각 디렉터리에는 원격 main 정책/목차의 commit SHA, 고정 diff, 관련 source snapshot, 클래스/함수/줄/관점, 검증과 미검증 항목을 남긴다.
자동화가 받는 JSON 모양은 [n8n-format.json](backend-v4-v1/n8n-format.json)에 사용자 제공 스키마 그대로 보존했다.
`review_findings=[]`는 자동 AI 리뷰를 실행했다는 뜻이 아니다. AI 리뷰 원문은 실행 시각/source SHA를 붙여 별도 파일로 추가한다.
가이드 생성·소스 복사·diff archive 자체를 새 기능 학습 후보로 취급하지 않는다. 원본 sourceHead의 코드 diff만 근거로 쓴다.
Slack 전송이나 n8n 설정 변경은 이 문서 저장과 별개이며 이번 작업에서는 하지 않았다.

## 출시 PR · 자동 정산과 운영 배포

- [고정 가이드](release-ready-v1/GUIDE.md) · [전체 PR diff](release-ready-v1/pr-main.diff.patch) · [추가 구현 diff](release-ready-v1/release-changes.diff.patch) · [개인 진도](progress/release-ready.md).
- 이전 전체 백엔드 이력 정리는 [PR #26](https://github.com/owencity/jungsan_attack/pull/26)에 보존되어 있다.

# 백엔드 PR 스터디 기록

조사 기준: 2026-10-10, GitHub 전체 25개 PR. 백엔드 코드 7개와 백엔드 설계·운영 문서 6개를 정리했다. 프론트 FC·화면 문서 및 목차 등록 PR은 구현 학습 대상에서 제외했다.

PR 생성·병합·운영 배포·학습 완료는 다른 상태다. 현재 코드 PR 7개는 모두 미병합이다. 학습 상태는 사용자 기록이 없어 모두 UNRECORDED로 둔다.

| PR | 제목 | 병합 상태 | 스터디 | 보완 |
|---|---|---|---|---|
| [#1](https://github.com/owencity/jungsan_attack/pull/1) | chore(automation): n8n PR 자동 코드리뷰 파이프라인 설계 메모 추가 | 미병합 | [가이드](archive/pr-1/9f791c01-v1/guide.md) · [고정 기준](archive/pr-1/9f791c01-v1/manifest.json) · [진도](progress/pr-1.md) | 백엔드 문서 PR · 별도 핵심 학습 없음 |
| [#2](https://github.com/owencity/jungsan_attack/pull/2) | Feat/local git flow web | 병합됨 2026-09-19 | [가이드](archive/pr-2/96d6a973-v1/guide.md) · [고정 기준](archive/pr-2/96d6a973-v1/manifest.json) · [진도](progress/pr-2.md) | 백엔드 문서 PR · 별도 핵심 학습 없음 |
| [#3](https://github.com/owencity/jungsan_attack/pull/3) | Feat/local git flow web | 미병합 | [가이드](archive/pr-3/0da394a2-v1/guide.md) · [고정 기준](archive/pr-3/0da394a2-v1/manifest.json) · [진도](progress/pr-3.md) | 백엔드 문서 PR · 별도 핵심 학습 없음 |
| [#4](https://github.com/owencity/jungsan_attack/pull/4) | feat(core): 차수별 결제자 정산 엔진 v2 적용 | 미병합 | [가이드](archive/pr-4/51e35a93-v1/guide.md) · [고정 기준](archive/pr-4/51e35a93-v1/manifest.json) · [진도](progress/pr-4.md) | 최신 diff로 신규 보완 |
| [#5](https://github.com/owencity/jungsan_attack/pull/5) | Docs/simplify onetime gathering | 병합됨 2026-10-03 | [가이드](archive/pr-5/061d726c-v1/guide.md) · [고정 기준](archive/pr-5/061d726c-v1/manifest.json) · [진도](progress/pr-5.md) | 백엔드 문서 PR · 별도 핵심 학습 없음 |
| [#7](https://github.com/owencity/jungsan_attack/pull/7) | feat(core): 제품 v3 기준 Core v2 계산 엔진 선별 보존 | 미병합 | [가이드](archive/pr-7/7cd7ff1c-v2/guide.md) · [고정 기준](archive/pr-7/7cd7ff1c-v2/manifest.json) · [진도](progress/pr-7.md) | 최신 diff로 신규 보완 |
| [#10](https://github.com/owencity/jungsan_attack/pull/10) | feat(user): 최초 실명 등록과 인증 가드 구현 | 미병합 | [가이드](archive/pr-10/d3407878-v2/guide.md) · [고정 기준](archive/pr-10/d3407878-v2/manifest.json) · [진도](progress/pr-10.md) | 최신 diff로 신규 보완 |
| [#16](https://github.com/owencity/jungsan_attack/pull/16) | feat(core): Java·Kotlin 정산 엔진을 함께 구현하고 검증 | 미병합 | [가이드](archive/pr-16/core-v2-java-v1/guide.md) · [고정 기준](archive/pr-16/core-v2-java-v1/manifest.json) · [진도](progress/pr-16.md) | 원격 커밋의 기존 고정 기록 보존 |
| [#17](https://github.com/owencity/jungsan_attack/pull/17) | docs(settlement): 같은 술자리의 총무별 독립 정산 계약을 정의 | 미병합 | [가이드](archive/pr-17/6269774c-v1/guide.md) · [고정 기준](archive/pr-17/6269774c-v1/manifest.json) · [진도](progress/pr-17.md) | 백엔드 문서 PR · 별도 핵심 학습 없음 |
| [#18](https://github.com/owencity/jungsan_attack/pull/18) | feat(backend): 같은 술자리의 총무별 독립 정산 구현 | 미병합 | [가이드](archive/pr-18/backend-v4-v1/guide.md) · [고정 기준](archive/pr-18/backend-v4-v1/manifest.json) · [진도](progress/pr-18.md) | 원격 커밋의 기존 고정 기록 보존 |
| [#20](https://github.com/owencity/jungsan_attack/pull/20) | feat(auth): 앱 인증·Apple 로그인·로그아웃·계정 탈퇴 | 미병합 | [가이드](archive/pr-20/auth-release-v1/guide.md) · [고정 기준](archive/pr-20/auth-release-v1/manifest.json) · [진도](progress/pr-20.md) | 원격 커밋의 기존 고정 기록 보존 |
| [#21](https://github.com/owencity/jungsan_attack/pull/21) | fix(ci): PR 자동 검증과 배포 실행 권한 복구 | 미병합 | [가이드](archive/pr-21/actions-ci-v1/guide.md) · [고정 기준](archive/pr-21/actions-ci-v1/manifest.json) · [진도](progress/pr-21.md) | 원격 커밋의 기존 고정 기록 보존 |
| [#23](https://github.com/owencity/jungsan_attack/pull/23) | docs(backend): 시니어 검증·FC 추적·SOLID·목차 활용 기준 | 미병합 | [가이드](archive/pr-23/backend-quality-rules-v1/guide.md) · [고정 기준](archive/pr-23/backend-quality-rules-v1/manifest.json) · [진도](progress/pr-23.md) | 원격 커밋의 기존 고정 기록 보존 |

## 읽는 순서와 병합 의존 관계

- 학습: Core Kotlin #7 → Java 비교 #16 → 실명·인증 가드 #10 → 총무 단위 계약 #17 → 서버 구현 #18 → 인증·탈퇴 #20. CI #21은 별도다.
- #4와 #7의 core/src 트리는 동일하다. 과거 #4를 보존하되 계산 학습은 #7부터 읽어도 된다. 별도 Git 이력이라 중복 PR의 병합·종료 판단은 CTO가 한다.
- #10 head는 #17의 조상이고 #17 head는 #18의 조상이며 #18 head는 #20의 조상이다. 현재 PR 대상도 #10 → #17 → #18 → #20 순으로 연결되어 있다.
- #16은 #18의 조상이 아니지만 #16의 Java 엔진과 비교 테스트 소스는 #18에 포함된다. core/src의 차이는 #18이 추가한 Java/Kotlin SettlementPresentation 두 파일뿐이다. 전부 개별 병합해야 한다는 뜻은 아니다.
- #21은 main 대상 CI 수정, #23은 main 대상 개발 규칙 문서다. main 병합·배포 여부는 CTO가 결정한다.
- 같은 작업 브랜치를 위 대상 브랜치에 먼저 병합하거나 squash/rebase할 경우 후속 PR의 기준·충돌·검사를 다시 확인해야 한다. 이 문서는 병합을 실행하거나 병합 가능성을 보장하지 않는다.

## 보존 방식

- 신규 가이드: 실제 PR merge-base→head diff, before/after 파일, 생성 시 origin/main의 목차·정책, 코드 줄·함수·관점, 진도, 해시를 저장한다.
- 기존 고정 가이드 #16·#18·#20·#21·#23은 원격 커밋의 파일을 그대로 가져왔다. 당시 catalogCommit을 유지하고 해시가 있는 기록은 검증했다.
- 로컬에만 있던 #7·#10 과거 버전은 [legacy](legacy/README.md)에 별도로 보존했다. 새 head/목차로 과거 기록을 덮어쓰지 않는다.
- 과거 PR의 Kotlin 단독 구현을 Java 동시 구현으로 표시하지 않는다. Java 없는 diff에 자바의 정석·Effective Java 목차를 억지로 붙이지 않는다.
- 가이드의 링크가 달라져도 보존된 changes.patch와 before/after 또는 source 파일로 당시 코드를 읽을 수 있다.
- 전체 조사 결과는 [audit.md](audit.md), 구조화된 기준은 [study-index.json](study-index.json)이다.

진도는 progress/pr-N.md에서 기록한다. archive 안의 progress.md는 생성 당시의 고정 템플릿이다. 학습하면서 보존 해시가 깨지지 않도록 실제 진도 파일은 archive 밖에 두었다.

이번 스터디 정리 PR 자체의 [가이드](backend-study-audit-v1/guide.md)·[고정 기준](backend-study-audit-v1/manifest.json)도 보존했다. 별도의 핵심 학습 주제는 없다.
