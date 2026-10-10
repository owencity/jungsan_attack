# 출시 준비 검증과 남은 조건 — 2026-10-10

이번 브랜치는 PR #10·#16 구현 포함 #18·#20·#21의 기능을 모은 기반에 FC-016~018·020과 운영 배포 수정을 더한다.
기존 PR의 리뷰·고정 학습 자료는 삭제하지 않는다. PR #26은 백엔드 전체 학습 이력 인덱스다.
새 출시 PR의 head를 기준으로 검증하며 과거 CI 성공을 새 커밋 성공으로 대신하지 않는다.

CTO는 실제 `.env`를 OCI의 `~/jeongsan/.env`에 직접 넣는다. 필수 키 목록은 DEPLOY.md에 보존했다.
DNS 전용 `CLOUDFLARE_API_TOKEN`은 GitHub Secret, 확인한 OCI Tunnel ID는 GitHub Variable로 준비한다.
CTO가 OCI Tunnel에 새 호스트 → localhost:18080을 추가한다. 기존 노트북 `api.devkdk.com`과
웹훅 `webhook.devkdk.com` → localhost:8080은 변경하지 않는다. DNS 작업은 새 호스트만 대상으로 한다.
실제 GitHub 리뷰 댓글 10개와 구현자 재검토는 [리뷰 기록](pr-reviews/2026-10-10-release/README.md)에 있다.

## 구현 및 로컬 검증

- Core 54, Server 116: 실패·오류·테스트 건너뛰기 0. 서버 빌드와 인증 가드 포함.
- 실제 MySQL 8.4 + Spring Boot 자동 정산/UTC/알림 20건: 마지막 응답 경합 1회 확정, 초과 명단·응답 제외, 다른 단위·공유 신원 보존, 인원 변경, Core 실패 보류, 수동 AUTO 예비, 삭제 차수 재판정.
- 실제 HTTP 인증 17건: WEB/APP 경계, 로그아웃 재사용 거절, 티켓 4건 중 1건 성공, 탈퇴/추가 단위 생성 4회 경합.
- 실제 DB 인증 probe 15건: state/티켓 만료·rollback, 한 번 교환, 계정 익명화, Apple 해제 실패 재시도. 외부 제공자는 가짜다.
- 배포 분기 모의 6건: 다른 서비스 포트·다른 이미지 거절, 성공 SHA 기록, 새 앱 실패 시 이전 앱 이미지 복원, 잠금 실패 중단. 실제 OCI 배포 시험은 아니다.
- actionlint 1.7.12: 두 workflow 오류 0. 배포·CI 셸 문법 검사 통과.
- 추가 DNS 가짜 API 7건, actionlint workflow 3개 통과. 실제 DNS 변경은 별도다.
- `028`은 테스트 DB에 적용됐으며 이전 migration checksum을 수정하지 않았다. JVM Asia/Seoul에서 응답 UTC와 실제 시각을 비교했다.

## 운영 전 필수 확인

1. 병합 권한: CTO가 2026-10-10에 CI 전체 통과·미결 결정 없음·리뷰/스터디 보존 조건으로 Codex 병합을 승인했다. REMOVE_PAYER 보류도 승인했다. [PR #27](https://github.com/owencity/jungsan_attack/pull/27)은 CI 전체 성공 후 병합됐다. 새 호스트 수정도 별도 PR·최신 CI를 거친다.
2. 운영 `.env`: 카카오 Client ID·Secret, Apple Services ID·Team ID·Key ID·p8, 콜백, 독립 JWT/계좌/인증 암호화 키. 저장소·CI 로그에 값은 남기지 않는다.
3. GitHub OCI_HOST/OCI_SSH_KEY가 CTO가 지정한 OCI와 준비된 키를 가리키는지 확인한다. PR #27 main 배포에서 SSH 전송은 성공했다. 현재 기동 중단 원인은 `~/jeongsan/.env` 부재다.
4. OCI Tunnel에 `jungsan-api.devkdk.com` → `http://localhost:18080` 새 항목 추가. 기존 8080의 n8n/Kafka는 유지한다.
5. 실제 운영 DB·백업과 기존 v1 CONFIRMED 자료 확인. 새 배포 디렉터리가 없다는 사실만으로 다른 위치의 DB가 없다고 단정하지 않는다.
6. 카카오 Redirect URI와 Apple Services ID Return URL은 `https://jungsan-api.devkdk.com/api/v1/auth/{provider}/callback`.
7. main SHA의 Deploy to OCI 성공·정산어택 컨테이너·외부 health 200/UP·비로그인 auth/me 401.
8. 웹 Vercel API base URL 적용·재배포, 실제 카카오/Apple 계정·TestFlight 앱 티켓 교환·로그아웃·탈퇴까지 시험한다.

App Store·Google Play 정식 출시·심사 통과는 이 백엔드 PR이나 자동 테스트 결과로 완료 처리하지 않는다.
면제 API, 복수 총무 스푼 정책, FCM/APNs, WebSocket은 기존 보류·미결 범위를 유지한다.
FC-020 인원 밖 결제자 보류는 CTO가 승인했다. 프론트 `autoSettlementError` 배너 표시·운영 연결 시험은 후속이다.

실제 배포 기록: [실행 38030304729](https://github.com/owencity/jungsan_attack/actions/runs/38030304729)은
테스트·빌드·전송 성공, 배포 단계 실패다. OCI에서 `.env` 부재와 정산어택 컨테이너 미생성을 확인했다.
운영 health·웹/앱 로그인을 성공으로 기록하지 않는다.
