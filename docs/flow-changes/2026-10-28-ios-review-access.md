# 2026-10-28 — iOS 로그인 화면과 App Review 기능 접근

## FC-027 iOS 인증 세션 복귀·다중 사용자 데모

- **상태:** 열림 — 백엔드 PR48·앱 PR1 구현·CI 통과. iOS 시뮬레이터의 데모 진입·역할·초기화·종료 조작도 통과했다. 운영 배포·iPhone 제공자 로그인·외부 데모 링크 확인·심사 제출은 후속.
- **근거:** CTO가 전달한 App Review Guideline 4와 2.1(a) 피드백. 기본 브라우저로 로그인하는 UX를 수정하고 다른 사용자와 기능을 확인할 데모 링크·접근 방법이 필요하다.
- **기존 흐름:** 앱 main `95c4985`의 App.kt는 `LocalUriHandler.openUri`로 로그인 주소를 연다. FC-026의 HTML 복귀 페이지는 Android 수동 복귀를 위한 것이며 iOS의 외부 브라우저 전환 자체를 해결하지 않는다.

### 백엔드 계약

1. 기존 `client=web|app`를 유지한다. iOS 인증 세션은 인가 시작에 **`client=app&appReturn=session&codeChallenge=...`**을 보낸다. 생략 또는 `appReturn=page`는 기존 앱200 HTML이다.
2. `appReturn=session`은 앱만 허용하며 잘못된 값·웹과의 조합은 400이다. 선택한 state에만 `auth_session_returns` 표식을 저장하고 부모 state 삭제 시 함께 제거한다. 콜백 query·UA로 모드를 변경하지 않는다.
3. session 성공 콜백은 **303 `Location: jeongsan://auth?ticket=<43자 일회용 티켓>`**과 빈 본문을 반환한다. URL·스킴·host는 고정이며 JWT를 URL에 넣지 않는다. `returnTo`로 네이티브 복귀 주소를 지정하지 않는다.
4. no-store/no-referrer·상관 쿠키 만료, Apple POST CORS·SameSite=None/Secure, state·nonce·PKCE·티켓300초·일회용 잠금은 유지한다. WEB 쿠키를 앱에 발급하지 않는다.
5. 웹·기존 Android/이전 iOS 앱은 기존 계약을 유지한다. 스키마는020/changeSet030의 표식 테이블이며 적용된 changelog는 고치지 않는다. 기존 바이너리가 auth_challenges에 컬럼 목록 없는 INSERT를 하므로 부모 컬럼 추가 대신 FK CASCADE 표식 테이블을 사용해 롤백 호환성을 유지한다.

### 앱 담당자 전달 사항 — 앱 PR1에 구현, CI·기기 확인 필요

CTO가 앱 담당자 휴가로 Codex에 앱 구현도 요청했다. [앱 PR1](https://github.com/owencity/jungsan_app/pull/1)에 Swift 인증 세션과 공개 데모를 구현했다. 앱 head2ed2523의 Android·iOS CI와 XCTest 화면 조작1건이 통과했다. 아래 계약은 이 PR의 검증 기준이며 실제 iPhone 제공자 로그인 성공은 아직 확인하지 않았다.

- iOS 로그인 진입을 `ASWebAuthenticationSession`으로 분리하고 공통 `openUri`를 그대로 쓰지 않는다. Apple이 안내한 `SFSafariViewController`도 선택지지만 인증·콜백 처리는 인증 세션 API를 우선 검토한다.
- 기존 로그인 시작 URL에 `appReturn=session`을 추가한다. callback scheme은 `jeongsan`, 기대 host는 `auth`다. completion의 callback URL을 기존 AppAuth 티켓 교환 흐름으로 한 번만 전달한다.
- 세션 객체 수명·presentation anchor·취소·중복 클릭·completion과 onOpenURL 중복 전달을 관리한다. verifier는 세션 시작부터 교환까지 유지하며 서버로 원문을 보내는 단계는 기존 exchange뿐이다.
- 카카오·Apple 양쪽에서 외부 Safari가 열리지 않는지, 취소 후 재시작·첫 가입 실명 입력·기존 로그인·로그아웃·탈퇴를 iPhone에서 확인한다.
- Android는 이번 iOS 변경을 이유로 FC-026의200 HTML·Intent 수동 복귀를 바꾸지 않는다.

### 심사용 접근 준비

CTO의 앱 인수 요청에 따라 **소셜 계정 비밀번호 공유 없이 명시적인 데모 체험 모드**를 앱 PR1에 구현했다. 빌드·기기에서 기능 지원 여부를 확인한 뒤 심사 정보에 실제 진입 절차를 적는다. 기존 `apiBaseUrl` 공백 목데이터 모드를 배포 설정 전체에 적용하거나 로그인 보호를 끄지 않는다.

- 앱 PR1의 로그인 화면에 별도 데모 진입을 두고 샘플 데이터와 역할 전환을 제공한다. 실제 계정·토큰과 분리하고 데모임을 표시한다.
- 총무·참여자2명 이상의 예시, 응답·명단·차수·정산·입금 상태·메시지·신고·차단을 조작할 수 있어야 한다. 단순 읽기 화면·영상만으로 완료 처리하지 않는다.
- 메일이 요청한 데모 링크는 `GET /api/v1/review/demo`의 공개200 HTML 안내와 고정 `jeongsan://demo` 버튼으로 준비한다. 새 앱은 정확한 데모 스킴만 받아 가상 상태를 연다. query는 안내에 반영하지 않고 인증·계정·티켓을 발급하지 않는다. 운영 주소는 `https://jungsan-api.devkdk.com/api/v1/review/demo`이며 백엔드 배포와 새 앱 설치·기기 확인 전에는 준비됐다고 제출하지 않는다.
- 데모만으로 제공하지 못하는 기능은 실제 계정으로 접근 가능한 방법을 함께 제공한다. 계정 삭제는 별도 실제 계정의 생성·삭제 경로를 안내한다.
- 이 서버 PR은 데모 로그인 API·고정 비밀번호·임의 JWT 발급·운영 인증 우회를 추가하지 않는다. 앱 PR1은 가상3명·응답/입금/완료 예시·역할 전환·초기화·체험 종료를 구현하고, 실제 API 클라이언트 금지·미출시 스푼 제한·신고/차단/금칙어 테스트를 추가했다. 로컬 데모가 서버 동시성·실제 제공자 인증까지 증명하지는 않는다.

자세한 제출·검증 절차는 [운영 준비](../operations/app-review-access.md)를 따른다. 코드 검증·운영 배포·앱 빌드·실기기 성공·심사 승인은 각각 구분한다.

### 근거 문서

- [Apple 인증 세션](https://developer.apple.com/documentation/authenticationservices/authenticating-a-user-through-a-web-service)
- [심사 접근 정보](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information#app-review-information)
- [계정 삭제 안내](https://developer.apple.com/help/app-review/guideline-reference/5-1-1-account-deletion)
