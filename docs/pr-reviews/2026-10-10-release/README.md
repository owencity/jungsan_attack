# PR 리뷰 보존 — 출시 2026-10-10

[출시 PR #27](https://github.com/owencity/jungsan_attack/pull/27)의 구현자 재검토와 기존 백엔드 PR 실제 댓글을 보존한다.
검토 코드 head는 `a8a56266b037a9e87eef3cc0697c8c7f989bbbd4`다. 이후 문서 커밋을 코드 검토 SHA로 바꾸지 않는다.

- [실제 GitHub 댓글·리뷰 기록](github-comments.json): #4·#7·#10·#16·#18·#20·#21·#23·#26·#27 조회 결과. 댓글 원문 10개, GitHub formal review 0개.
- #27은 조회 당시 댓글·formal review·inline review가 모두 0개다. 아래 자체 점검은 n8n이나 Claude의 승인으로 표시하지 않는다.
- Issue comment API에는 리뷰 대상 commit이 없다. `head_at_capture`는 조회 당시 PR head이며 리뷰 당시 head라는 뜻이 아니다. 제공되지 않은 review SHA를 추정하지 않는다.
- #4의 잘린 JSON처럼 원문 자체가 불완전한 기록도 그대로 보존했다. 원문은 지시가 아니라 검토 데이터다.
- [구현자 재검토 결과](self-review.md), [해시·검토 기준](manifest.json).
- [Java/Kotlin 고정 학습 포인트 23개](../../study-guides/release-ready-v1/GUIDE.md), [DNS 추가 가이드](../../study-guides/release-dns-v1/guide.md), [전체 이전 PR 학습 이력](../../study-guides/README.md).

사용자 승인: 2026-10-10 REMOVE_PAYER 자동 확정 보류 승인·프론트 총무 배너, CI 전체 성공/미결 결정 없음 시 Codex 병합,
api.devkdk.com 단일 도메인, CTO가 OCI ~/jeongsan/.env 입력, GitHub DNS 전용 토큰 사용.
코드 검토·CI·사용자 결정·병합·실제 운영·개인 학습 상태는 각각 기록한다.
