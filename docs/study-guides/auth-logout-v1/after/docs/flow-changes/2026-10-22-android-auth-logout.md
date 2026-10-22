# 2026-10-22 — Android 카카오 인증 뒤 복귀 실패·기존 로그아웃 연결

## FC-026 Android 카카오 로그인 복귀 실패 확인과 백엔드 로그아웃 준비

- **상태:** 열림 (로그아웃 기존 구현 확인·백엔드 검증 보완, 실제 Android 복귀 실패 원인 미확정)
- **요청:** CTO가 Android에서 카카오 인증 뒤 앱 복귀에 실패한다고 알렸다. 로그아웃은 백엔드부터 준비한다. 오류 문구·실패 화면과 재현 시각은 아직 확인되지 않았다.
- **확인한 기준:** main `560fee34cb704c40c3273238eb2bd38e31edd719`, 앱 원격/로컬 main `95c4985c39bd0bb67fea1c63d5134fedd1c03c98` 읽기 전용 대조. 서버는303 `jeongsan://auth?ticket=...`, 앱 Manifest의 scheme/host와 MainActivity onCreate/onNewIntent가 일치한다. 에뮬레이터 설치 앱에서도 스킴이 MainActivity로 해석된다. 이 정적/등록 확인은 실제 카카오 로그인 성공 증거가 아니다.
- **운영 관찰:** 현재 운영 이미지560fee3. 읽은 앱 기동 이후 최대48시간 로그에 서버 예외가 없었다. 기존401·제공자 오류가 원인별로 기록되지 않아 이 결과만으로 인증 성공·콜백 도달을 판단할 수 없다. 접근 로그는 code·쿠키 보호를 위해 계속 비활성화한다. 운영 계정의 인증 값을 사용하거나 실제 카카오 승인을 수행하지 않았다.
- **백엔드 영향:**
  1. 기존 `POST /api/v1/auth/logout`을 재사용한다. APP Bearer/WEB 쿠키→204, 전달 자격의 해시 무효화, WEB 쿠키 만료. `Cache-Control: no-store`만 추가한다. 자격 없는 요청·이미 무효화된 재호출은 기존401이다. 모든 기기 로그아웃이나 카카오 계정 세션 종료는 요청 범위로 해석하지 않는다.
  2. 실제 DB·HTTP에서 WEB/APP 무효화·쿠키 속성·동시 재시도·다른 기기 유지·무인증 거절을 검사한다. 프론트는204 본문을 JSON으로 읽지 않고 로컬 토큰을 제거하면 된다.
  3. 로그인 시작/콜백 거절 사유/콜백 완료를 허용된 메타데이터만으로 기록한다. 쿠키 검증을 완화하거나 콜백 경로·딥링크·토큰 계약을 임의 변경하지 않는다.
- **남은 확인:** 재현 시각과 실제 마지막 화면의 오류 코드/문구를 받아 로그 단계와 대조한다. COMPLETED 뒤 복귀 실패라면 프론트/브라우저의 딥링크 처리와 설치 빌드를 확인한다. MISSING_BROWSER_COOKIE·STATE_EXPIRED·PROVIDER_REJECTED라면 해당 서버/인가 단계의 원인을 조사한다. 어느 분기도 아직 원인으로 확정하지 않았다.
- **관련 FC:** FC-014·021의 실제 앱 연결 후속이며 FC-025 Apple CORS 예외는 유지한다. 다른 열린 FC의 기능·미결을 이 변경에 섞지 않는다.
- **가정·대안:** 브라우저 제한 추정으로 intent URL·중계 HTML·새 SDK를 추가하는 대신 기존 계약과 거절 응답을 유지하며 먼저 실패 단계를 확인한다. 추가 재현이 필요하다는 한계가 있다.
- **반영할 곳:** AUTH_RELEASE·API v15·AuthController/AuthFlowService·로그아웃 실제 HTTP 검증·PR 자체 리뷰/스터디 기록. 백엔드 반영·CI·병합·배포와 실기기 완료를 구분해 PR에 적는다.
- **참고:** [카카오 REST 로그인](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [Chrome Android 외부 앱 호출 제약](https://developer.chrome.com/docs/android/intents), [Android 딥링크](https://developer.android.com/training/app-links/create-deeplinks).
