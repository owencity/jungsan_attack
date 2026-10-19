# 10/18 백엔드 출시·웹/앱 연결 확인

백엔드 기준은 PR #36의 Boot 4.1.1 위에 쌓은 [PR #37](https://github.com/owencity/jungsan_attack/pull/37)이다.
운영 적용·실계정 승인·최종 기기 빌드 실행과 자동 검증을 구분한다.

## 운영 공개 요청

2026-10-18 02:35:17 KST, `https://jungsan-api.devkdk.com`. CA·호스트 검증을 활성화한 HTTPS 요청이다.
계정·JWT를 사용하거나 로그인 시도를 생성하지 않았다. 원문은 `pr-reviews/2026-10-18-release/public-probe.json`이다.

| 요청 | 결과 | 판단 |
|---|---|---|
| GET /actuator/health | 200 · UP | 공개 HTTPS·서버 기동 확인 |
| GET /api/v1/auth/me | 401 · UNAUTHENTICATED | 올바른 사용자 경로의 인증 거절 |
| GET /api/v1/me | 500 · INTERNAL_ERROR | PR #33의 없는 경로 404 수정은 운영 미적용 |
| GET /actuator/metrics | 404 | 외부 metrics 차단 유지 |
| OPTIONS /api/v1/users/me/display-name | 200 | 운영 웹 origin·credentials·PUT/PATCH 허용 |
| OPTIONS /api/v1/me/gatherings | 200 | 운영 웹 origin·credentials·Authorization 허용 |

응답은 단건 0.375~0.640초였다. 부하/반복 성능 시험이 아니며 로그인 성공을 의미하지 않는다.

## 웹·앱 소스 계약 대조

앱은 `D:/dev/jungsan/jungsan_app`의 로컬 파일을 읽기 전용으로 확인했다. 파일 해시·발췌는 `app-source-evidence.json`에 보존했고 dirty 상태·출시 산출물과의 동일성은 확인하지 못했다.
웹은 GitHub `owencity/profile` 기본 main의 api.ts·serverModel.ts·gateway.ts·store.ts·RoomPage.tsx를 확인했다. 파일별 blob SHA·발췌는 `web-source-evidence.json`에 보존했다. 현재 Vercel 배포의 소스/환경변수와 같다고 보지 않는다.

| 항목 | 확인한 코드 | 남은 범위 |
|---|---|---|
| 웹 인증 | credentials include, /api/v1/auth/me, returnTo 전달, 로그아웃·탈퇴 경로 | 실제 계정·쿠키 복귀 |
| 앱 인증 | APP Bearer, 티켓+verifier 교환, /api/v1/auth/me | Android/iOS 브라우저·앱 스킴 왕복 |
| 단위별 정산 | 상세·명단·round/transfer의 settlementUnitId, revision/hash, 알림 DTO | 실제 화면·다중 사용자 흐름 |
| 서버 주소 | 웹 VITE_JEONGSAN_API_BASE_URL, 앱 JS_API_BASE_URL 사용 | 출시 빌드 값·목 모드 해제 확인 |
| 본인 단독 다음 차 | 웹/앱 API 모드가 목표 인원 1 전송, 목 모드는 최소 2 | FC-023 프론트 수정·실제 빌드 재검증 |

API base는 `https://jungsan-api.devkdk.com`이며 `/api/v1`을 덧붙이지 않는다. 클라이언트가 각 경로에 이미 프리픽스를 붙인다.

## 찾은 연결 문제와 수정 방법

[FC-023](flow-changes/2026-10-18-app-connection.md): 웹/앱의 [나 혼자 시작하기]가 API 모드에서는 `headcount=1`을 보낸다. 서버의 자동 정산 목표 인원은 2~50이며, 명단이 현재 1명인 것과 구분한다.
권장 수정은 웹 `gateway.createUnit`·앱 `V3Gateway.createUnit`의 목표 인원 초기값을 각 목 스토어의 최소 2 기준과 맞추는 것이다. 본인만 선택하는 버튼과 서버의 기존 계약을 유지할 수 있다.
서버 MVC 및 실제 DB HTTP에서 1 거절/삽입 없음, 본인 단독 명단+2 허용을 검증한다. 실행 결과는 PR의 CI 기록을 따른다.

프론트 저장소 파일은 수정하지 않았고 담당자에게 메시지를 보내지 않았다. 실제 앱/웹 화면에서 재현했다고 보고하지 않는다.

## 기존 데이터 전환·남은 출시 확인

Boot 3.5.16 소스를 별도 빌드해 계좌·정산·미리보기를 만든 뒤 같은 DB에서 Boot 4.1.1로 교체한다. 이전 APP 토큰은 구버전 HTTP 티켓 교환으로 발급하고 WEB은 서명 테스트 쿠키를 사용한다.
기존 체크섬·암호문·JSON 보존, 이전 미리보기로 정산, 이전 송금의 보냄/확인, 로그아웃 재사용 거절을 검증한다. 운영 DB 사본을 검증한 것은 아니다.

최종 코드 e9a3d50의 [CI run 43](https://github.com/owencity/jungsan_attack/actions/runs/39055094203)에서 Core 54·서버 146, 기존 데이터 전환 12건과 앱 요청 경계 2건, 기존 실제 DB 회귀·prod 재기동·배포 복원·DNS가 모두 통과했다.

웹·Android·iOS의 실제 계정 로그인 → 실명 → 링크 참여 → 복수 단위/자동·수동 정산 → 계좌/송금 → 완료 → 로그아웃·탈퇴는 여전히 실계정·최종 빌드 확인이 필요하다.
PR #32/#33/#35/#36/#37의 병합·배포, FC-023 프론트 수정과 실제 빌드 확인을 각각 구분해 진행한다. 면제·복수 총무 스푼·푸시·WebSocket은 이번 검증으로 확정하지 않는다.
