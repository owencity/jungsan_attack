# 10/14 백엔드·앱 연결 확인

## 확인 범위

실행 결과·현재 소스 대조·실계정/실기기 확인을 구분한다. 실제 카카오/Apple 계정 승인, TestFlight·Android·웹 전체 흐름을 완료로 표시하지 않는다.

## 운영 API 공개 점검

2026-10-14 01:03:41 KST, `https://jungsan-api.devkdk.com`. 인증서·호스트 검증을 활성화한 HTTPS 요청으로 확인했다.

| 요청 | 결과 | 의미 |
|---|---|---|
| GET /actuator/health | 200, UP | HTTPS·서버 기동 확인 |
| GET /api/v1/auth/me (미인증) | 401, UNAUTHENTICATED | 정상 사용자 경로는 인증을 요구 |
| GET /api/v1/me | 500, INTERNAL_ERROR | PR #33 수정은 운영 미적용. 올바른 사용자 경로는 /api/v1/auth/me |
| GET /actuator/metrics | 404 | Caddy 외부 허용 범위 밖 차단 |
| OPTIONS /api/v1/users/me/display-name | 200 | 운영 웹 origin 허용·credentials true·PUT/PATCH 포함 |

각 요청은 0.438~0.797초였다. 연결 재사용·반복 측정이나 부하 시험이 아니므로 성능 보장으로 사용하지 않는다.
카카오/Apple 콜백 성공과 인증 후 금액·송금 흐름은 이 공개 점검으로 확인되지 않는다.

## 앱 소스와 서버 계약 대조

`D:/dev/jungsan/jungsan_app`의 현재 로컬 파일을 읽기 전용으로 확인했다. `git log -1`은 `cd559f7`이나 작업 트리 상태 조회가 실패해 아래 대조를 해당 커밋의 배포/빌드 결과로 간주하지 않는다. 프론트 파일은 수정하지 않았다.

| 항목 | 앱 소스 | 서버 계약 대조 |
|---|---|---|
| 내 정보 | ApiClient.me → /api/v1/auth/me | 경로 일치. /api/v1/me를 사용하지 않음 |
| 앱 로그인 시작 | loginUrl → client=app, codeChallenge | AuthController·AuthPolicy 계약 일치 |
| 복귀 | Android MainActivity 초기 intent/onNewIntent, iOS onOpenURL → jeongsan://auth | 서버의 앱 복귀 주소와 일치. 실기기 스킴 실행은 미확인 |
| 티켓 교환 | POST /api/v1/auth/app/exchange, ticket/codeVerifier | 서버 요청 DTO 일치. TokenResponse의 token/expiresAt 필드 일치 |
| 인증 | bearerAuth, 웹 쿠키 사용하지 않음 | APP/WEB 구분·Bearer 계약 일치 |
| 로그아웃·탈퇴 | POST /api/v1/auth/logout, DELETE /api/v1/users/me | 서버 경로 일치. 실제 제공자 연결 해제는 별도 |
| 알림 | /api/v1/me/notifications, 읽음·모두 읽음 | 기존 DTO의 type/gatheringId/settlementUnitId/title/body 필드 일치 |
| 운영 주소 | Android JS_API_BASE_URL → BuildConfig, iOS JS_API_BASE_URL → Info.plist | 출시 산출물 값은 확인하지 못함. https://jungsan-api.devkdk.com, /api/v1 접미사 없이 설정 필요 |

앱 ApiTest/AppAuthTest는 MockEngine 기반이다. 소스 대조와 가짜 제공자 서버 검증을 실제 카카오·Apple 동의 및 실계정 인증 성공으로 바꾸어 기록하지 않는다.
웹 저장소는 현재 작업공간에서 확인되지 않아 소스 대조도 미확인이다. 위 CORS는 운영 웹 origin의 서버 사전 요청 결과만 확인한다.

## 10/18 전에 남은 실제 연결 시험

1. 각 출시 빌드가 목데이터 모드가 아닌 운영 주소를 사용하는지 확인한다.
2. 카카오/Apple 승인 → 앱 스킴/웹 returnTo → 앱 티켓 교환/웹 쿠키 → /auth/me 순서를 실제 계정으로 통과한다.
3. 실명 입력·링크 참여·두 정산 단위·자동/수동 정산·계좌 등록·보냄/미수취/확인·전체 완료를 확인한다.
4. 로그아웃 후 이전 토큰 거절, 진행 중 탈퇴 거절, 모든 참여 단위 완료 후 탈퇴를 확인한다.
5. PR #33/#35 운영 반영 뒤 오류 응답과 신규 내부 알림 문구를 다시 확인한다. FCM·면제·스푼은 별도 후속이다.

## 동시에 열린 PR의 문서 충돌

별도 작업의 draft PR #34(영수증 OCR)는 API v11·ADR-021을 사용한다. 이번 PR #33은 API v11, #35는 v12이며 Caddy PR #32도 ADR-021을 사용한다. 향후 #34 병합 전 최신 main에 맞춰 버전·변경표·ADR 번호를 조정해야 한다. OCR 코드나 그 작업 브랜치를 이번 연결 보완에서 수정하지 않는다.
