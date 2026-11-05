# FC-034 · iOS 카카오톡 간편 로그인

- **상태:** 열림 · CTO 결정, 구현/검증/운영 설정 후속.
- **바뀐 흐름:** 카카오톡이 설치돼 있으면 공식 iOS SDK로 카카오톡을 열어 로그인한다. 미설치면 설치 안내와 `카카오톡 설치하기 / Apple로 계속하기 / 취소`를 표시한다. iOS 카카오 계정 웹 로그인으로 자동 전환하지 않는다. 설치 버튼을 눌렀을 때만 App Store를 연다. Apple 로그인·Android 기존 동작은 유지한다.
- **백엔드 영향:** 공개 JSON `POST /api/v1/auth/kakao/native`, 요청 `{accessToken}`. 서버가 Kakao 토큰 정보에서 app_id/만료/사용자 ID를 검증하고 `/v2/user/me` ID와 대조한 뒤 기존 KAKAO 계정으로 APP 전용 JWT를 반환한다. 클라이언트의 사용자 ID·닉네임을 신뢰하지 않는다. DB 스키마·정산·스푼 변경 없음.
- **기존 문서와 충돌:** FC-014/027의 iOS 카카오 웹 인가 시작은 이 경로로 대체한다. 기존 웹/Android OAuth·PKCE와 Apple 시스템 인증은 유지한다. SDK 키는 공개 Native app key이며 REST Client Secret을 앱에 넣지 않는다.
- **결정 근거:** CTO가 이메일/비밀번호 입력의 불편을 지적하고 카카오톡 미설치 시 설치 안내를 선택했다. 브라우저의 내장/외부 여부를 검사한 것만으로 이 요구를 구현했다고 보지 않는다.
- **운영 준비:** 동일 카카오 개발자 앱의 Native app key, 앱 ID, iOS 번들 `app.jeongsan` 등록을 확인한다. 서버 `KAKAO_APP_ID`와 앱 빌드의 공개 `KAKAO_NATIVE_APP_KEY`를 각각 설정한다. 설정 누락 상태로 운영 배포/업로드하지 않는다.
- **반영할 곳:** API v23, AUTH_RELEASE, 앱 APP_REVIEW/네이티브 로그인 안내, DEVLOG, 인증 가드, 배포 환경.
- **백엔드 반영·근거:** 브랜치 구현 및 로컬 테스트 완료. 운영 설정·배포와 실제 카카오톡이 설치된 iPhone/iPad의 앱 전환·동의/취소·복귀·기존 계정 유지 및 로그인 완료는 미완료다. Apple Guideline 4 해소 여부는 재심사 결과로 별도 확인한다.

## 계약

성공200: `{token, expiresAt}`(기존 앱 티켓 교환과 같은 응답). `Cache-Control: no-store`, 웹 쿠키 미발급.
필드 검증400 `VALIDATION_FAILED`, 잘못된 JSON/토큰 문자400 `MALFORMED_REQUEST`, 만료·잘못된 토큰/다른 앱 ID/사용자 ID 불일치401 `UNAUTHENTICATED`, 제공자 일시 실패503 `AUTH_PROVIDER_UNAVAILABLE`.
토큰/프로필/인가 코드/쿠키는 로그에 남기지 않는다. 제공자 토큰은 DB에 보관하지 않는다.
토큰 확인 네트워크는 DB 트랜잭션 바깥에서 수행하며, 기존 계정 upsert와 APP JWT 발급만 트랜잭션에 넣는다.

근거: [Kakao iOS 로그인](https://developers.kakao.com/docs/ko/kakaologin/ios), [토큰 정보 조회](https://developers.kakao.com/docs/ko/kakaologin/rest-api#access-token-info).
