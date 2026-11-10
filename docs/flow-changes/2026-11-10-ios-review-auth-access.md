# FC-036 · 추가 앱 설치와 이름 재입력 없이 로그인

- **상태:** 서버·웹 운영 반영됨. iPhone·iPad 자동 검증과 TestFlight 19 업로드 완료. 실계정 로그인 완료·TestFlight 목록 노출·Apple 재심사 승인은 미확인.
- **근거:** 2026-11-10 Apple이 0.1.0(18)을 심사해 카카오톡 설치 요구(4.2.3(i))와 Apple 로그인 뒤 이름 재입력(4)을 지적했다. CTO가 두 항목의 수정을 승인했다.
- **바뀐 흐름:** 카카오톡 설치자는 기존 간편 로그인을 사용한다. 미설치자는 정산어택 안의 시스템 인증창에서 카카오 계정으로 로그인한다. Apple 로그인은 제공받은 이름을 사용하며 이름을 다시 입력해야만 앱을 사용할 수 있는 조건을 없앤다. 앱 로그인 화면에서 약관에 명시적으로 동의한 뒤 소셜 로그인을 시작한다.
- **백엔드 영향:** Apple 인가에 `scope=name`을 추가하고 콜백의 선택 `user` JSON에서 이름을 읽는다. 계정 식별은 기존처럼 검증된 Apple `sub`만 사용한다. 이름이 없는 계정에만 표시 이름을 저장한다. API v25이며 URL·응답 필드·DB 스키마는 유지한다.
- **기존 문서와 충돌:** FC-022의 이름 동의 제거와 FC-034의 미설치 설치 안내를 이 범위에서 대체한다. FC-013의 카카오 최초 수동 이름 등록은 유지한다. FC-035/API v24는 별도 차수별 인원 변경 브랜치이므로 이 로그인 수정에 포함하지 않는다.
- **반영할 곳:** REQUIREMENTS, DOMAIN_DB_DESIGN_V2, API, AUTH_RELEASE, FC-022/034, DEVLOG, 앱 로그인 화면·인증 브리지·회귀 검사.
- **백엔드 반영·근거:** `AuthPolicy`, `AuthController`, `AuthFlowService`, `AppleDisplayName`, `UserService`에 반영했다. `:server:test :server:bootJar` 성공, 250건 실패·오류·건너뜀 0. [Backend CI](https://github.com/owencity/jungsan_attack/actions/runs/42826333190)에서 실제 MySQL 인증 검사 29건과 HTTP·복원·Caddy 검사도 통과했다. `02e8557`을 운영에 배포해 health200·Apple `scope=name`·form_post·Secure/HttpOnly/SameSite=None을 확인했다. 실계정 인증 완료와 App Review 승인 여부는 자동 검사 결과와 구분한다.
- **앱 검증:** `b11e0cc`에서 공통·Android 테스트 244건을 통과했다. [인증 UI 검사](https://github.com/owencity/jungsan_app/actions/runs/42826963795)는 iPhone·iPad의 Apple·카카오 계정 입력 화면과 각 2회 취소·재시도 4건을 통과했다. [일반 CI](https://github.com/owencity/jungsan_app/actions/runs/42826963465)에서 이름 정정·데모·공유·이미지 저장 9건도 통과했다. 제공자 계정 입력 화면까지만 확인했으며 실계정으로 인증을 완료한 검사는 아니다.
- **iOS 배포:** [TestFlight 19](https://github.com/owencity/jungsan_app/actions/runs/42833357915)가 같은 `b11e0cc`로 성공했다. 2026-11-10 23:39 KST에 `Upload succeeded`·`EXPORT SUCCEEDED`와 Apple 처리 시작을 확인했다. 사용자에게 TestFlight 0.1.0(19) 표시 후 카카오톡 미설치 로그인 완료와 Apple 이름 재입력 생략을 확인하도록 안내한다. 심사 제출은 수행하지 않았다.
- **웹 반영:** `346c4d6`에서 270개 테스트, TypeScript·빌드·변경 파일 ESLint를 통과했다. 가상 계정으로 약관 확인과 계정 분리를 브라우저에서 확인했다. [Vercel 배포](https://vercel.com/kdks-projects-8235f941/jungsan/H2vW9CRvwuHvaPKuYHPjBRD4ZBsy)가 Production·Ready이며 실제 운영 약관·개인정보 페이지에 Apple 이름 처리 안내가 표시된다.
- **PR:** [백엔드 #58](https://github.com/owencity/jungsan_attack/pull/58), [앱 #11](https://github.com/owencity/jungsan_app/pull/11), [웹 #4](https://github.com/owencity/profile/pull/4)를 학습용으로 열어 두고 병합하지 않았다. 운영에는 위 검증 커밋을 직접 배포했다.
- **별도 확인 필요:** 작업 시작 시 웹 운영본 `02e98c4`에는 이전 비회원 응답·정정 `GuestFlow`와 차수별 예상 인원 수정이 포함되지 않았다. 이번 변경은 해당 운영본에 로그인·약관 처리를 추가했다. 다음 배포 전에 개발 브랜치와 운영 반영 경로를 대조해야 하며, 기존 기능이 빠진 시각이나 어떤 배포가 교체했는지는 아직 확인하지 못했다.

## 카카오톡 미설치

앱은 기존 `GET /api/v1/auth/kakao/login?client=app&appReturn=session&codeChallenge=...`을 시스템 인증창에서 연다. 서버는 기존 state·브라우저 쿠키·일회용 티켓·PKCE 검증을 유지한다. 성공 시 기존 카카오 계정으로 복귀한다. 설치 안내나 Apple 계정 전환을 카카오 계정 접근의 필수 조건으로 두지 않는다.

Kakao SDK의 계정 로그인 API만 호출하면 앱 안에 표시된다고 가정하지 않는다. 공식 문서가 기본 브라우저 실행을 안내하므로 기존 `ASWebAuthenticationSession` 경로를 사용하고, iPhone·iPad에서 계정 입력 화면과 취소 복귀를 확인한다.

## Apple 이름

1. 인가 요청은 `scope=name`만 추가한다. 이메일은 서비스에서 사용하지 않으므로 요청·저장하지 않는다.
2. 콜백 `POST /api/v1/auth/apple/callback`은 form-urlencoded의 `code`, `state`와 선택 `user`를 받는다. `user`는 Apple이 최초 동의 때 전달할 수 있는 JSON 문자열이다.
3. state·브라우저 쿠키 및 Apple 토큰의 서명·issuer·audience·exp·nonce·sub 검증이 먼저다. `user` 안의 ID·이메일로 계정을 찾거나 연결하지 않는다.
4. 이름은 정규화하고 제어문자를 제거해 기존 `display_name` 길이에 맞는 최대 10 Unicode 코드포인트로 저장한다. Apple이 제공한 한 글자 이름도 허용한다. 수동 최초 이름 등록 API의 2~10자 규칙은 유지한다.
5. 기존 표시 이름이 있으면 그대로 둔다. 이름이 없거나 선택 JSON에서 읽을 수 없으면 `총무`와 사용자 ID 끝 8자리를 조합해 기본 표시 이름을 만든다. 표시 이름은 중복될 수 있으며 계정·권한 식별에는 쓰지 않는다.
6. `needsName = displayName == null`은 유지한다. Apple 로그인에서 이름을 채운 뒤 응답하므로 이름 입력 화면을 강제하지 않는다. 이전 로그인 토큰으로 `GET /auth/me`만 호출하는 Apple 계정도 이름이 NULL이면 기본 이름을 저장한 뒤 응답한다. 이 조회는 쓰기 트랜잭션과 `READ_COMMITTED`를 사용해 동시 콜백이 먼저 저장한 이름을 재조회한다.
7. 사용자 이름·원문 JSON·인가 코드·토큰은 로그에 남기지 않는다. 새 스키마와 기존 계정 일괄 갱신은 없다. 이미 등록된 이름과 정산 데이터는 변경하지 않는다.

## 검증할 사용자 동작

- 카카오톡 없이 카카오 계정 입력 화면을 앱 안에서 열고, 취소하면 로그인 화면으로 돌아온다.
- 카카오톡 설치 기기는 기존 간편 로그인과 계정 정보를 유지한다.
- Apple 최초 로그인에서 이름을 받고, 재로그인 때 이름이 오지 않아도 저장한 이름을 유지한다.
- 과거에 이름을 저장하지 못한 Apple 계정도 별도 이름 입력 없이 사용할 수 있다.
- 약관 동의 전에는 소셜 로그인을 시작하지 않으며 데모의 기존 동의 화면은 유지한다.
- 앱은 사용자별 약관 확인 기록을 기기에 보관한다. 저장된 로그인 상태를 복원했는데 확인 기록이 없으면 이름 입력 없이 약관에 동의하거나 로그아웃하도록 한다. 이 기록은 서버의 약관 이력이 아니며 다른 기기에서는 다시 확인할 수 있다.
- 웹도 자동 이름 때문에 기존 이름 화면을 생략하므로, 실제 로그인한 계정에 브라우저 내 약관 확인 기록이 없으면 이름 없는 약관 화면을 표시한다. 개인정보 안내는 Apple 첫 동의 이름 수신과 기본 표시 이름 사용을 반영한다. 서버의 동의 이력을 추가한 것은 아니다.
- 잘못된 state·브라우저 쿠키·nonce·토큰과 재사용한 티켓은 여전히 거절한다.

근거: [Apple 심사 기준](https://developer.apple.com/app-store/review/guidelines/#minimum-functionality), [Apple 사용자 정보 응답](https://developer.apple.com/documentation/signinwithapple/configuring-your-webpage-for-sign-in-with-apple), [Kakao iOS 로그인](https://developers.kakao.com/docs/ko/kakaologin/ios).
