# PR 리뷰·스터디 기록

개발 완료 → PR → 자동 리뷰/스터디 생성 → 사람이 나중에 diff를 읽고 학습하는 흐름이다.
구현 완료와 PR 병합·리뷰 완료·학습 완료는 별도로 기록한다. 과거 가이드는 새 main 코드로 덮어쓰지 않는다.

| 구현 | 가이드 | 코드 기준 | 학습 |
|---|---|---|---|
| backend-v4 같은 술자리·총무별 독립 정산 | [guide.md](backend-v4-v1/guide.md) · [review.md](backend-v4-v1/review.md) | [manifest.json](backend-v4-v1/manifest.json)의 base/head와 [changes.patch](backend-v4-v1/changes.patch) | [progress.md](backend-v4-v1/progress.md) |
| [PR #21 CI·배포 실행 권한](https://github.com/owencity/jungsan_attack/pull/21) | [guide.md](actions-ci-v1/guide.md) · [review.md](actions-ci-v1/review.md) | [manifest.json](actions-ci-v1/manifest.json) · [changes.patch](actions-ci-v1/changes.patch) | [progress.md](actions-ci-v1/progress.md) |

각 디렉터리에는 원격 main 정책/목차의 commit SHA, 고정 diff, 관련 source snapshot, 클래스/함수/줄/관점, 검증과 미검증 항목을 남긴다.
자동화가 받는 JSON 모양은 [n8n-format.json](backend-v4-v1/n8n-format.json)에 사용자 제공 스키마 그대로 보존했다.
`review_findings=[]`는 자동 AI 리뷰를 실행했다는 뜻이 아니다. AI 리뷰 원문은 실행 시각/source SHA를 붙여 별도 파일로 추가한다.
가이드 생성·소스 복사·diff archive 자체를 새 기능 학습 후보로 취급하지 않는다. 원본 sourceHead의 코드 diff만 근거로 쓴다.
Slack 전송이나 n8n 설정 변경은 이 문서 저장과 별개이며 이번 작업에서는 하지 않았다.
