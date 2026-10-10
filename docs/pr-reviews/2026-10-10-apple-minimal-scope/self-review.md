# Apple scope 수정 구현자 자체 점검

PR #30 · 기준 `a2586e9a462de52d5a30b2d53e45517935e7b700` → `250b9848df38e3d5766c7214bab4d12049f7b9bd`. 구현자의 자체 점검이며 사람 승인이나 자동 리뷰 원문으로 표시하지 않는다.

- 개인정보: ProviderGateway.identity는 검증한 sub와 빈 nickname/null image를 사용한다. AppleTokens.verify는 sub만 반환하고 AuthFlowService는 이름·이메일을 읽거나 저장하지 않는다. 탈퇴 연결 해제용 refresh token은 별도로 암호화 저장하며 이번에 변경하지 않는다.
- 수정: Java/Kotlin AuthPolicy가 각각 최소 인가 인자를 구현한다. Java가 Kotlin 정책을 호출하지 않는다. Spring HTTP 어댑터는 공유해 Bean/URL 중복을 만들지 않는다.
- 권한·보안: state·nonce·form_post·상관 쿠키·서명/issuer/audience/expiry 검증은 보존했다. 신규 웹/앱 테스트 및 기존 실패/인증 가드 테스트가 통과했다.
- 중복·동시성: 인자 함수는 요청별 새 값만 반환하며 공유 가변 상태·DB 트랜잭션 변경이 없다. 기존 로그인 challenge 잠금·일회용 티켓 정책은 변경하지 않았다.
- 가용성·조회: 새 외부 호출·재시도·쿼리·의존성을 추가하지 않았다. Apple 미설정 503과 잘못된 운영 키의 기동 실패 테스트를 유지했다.
- 호환성: API v10·AUTH_RELEASE·FC-022를 함께 갱신했다. 이전 동의 이메일 claim이 전달될 수 있어 미수신을 보장하지 않고 사용·저장하지 않는 것을 구분한다. 프론트 방침 파일은 이 저장소에 없어 법적 검수 완료를 주장하지 않는다.
- SOLID: 인가 인자 선택을 순수 정책에 두고 HTTP 구성은 기존 어댑터에 둔다. 구조 확대·인터페이스 추가는 없다.
- 실행: 서버 130건, 실패/오류/건너뛰기 0, 서버 컴파일 성공. 실제 계정 인증·콜백·티켓 교환은 미검증이다. 최종 원격 CI·운영 적용은 별도 기록한다.

확인한 열린 FC: 005·009·010·011·014·015·020·021. 이번은 014/021의 인증 후속과 022만 반영한다. 알림·면제 보류·총무 단위·자동 정산 프론트 연결은 관련 없는 후속으로 유지한다. 미결 CTO 결정은 없다.

외부 기준: [Apple 인가 요청](https://developer.apple.com/documentation/signinwithapplerestapi/request-an-authorization-to-the-sign-in-with-apple-server) 및 [Apple 사용자 인증](https://developer.apple.com/documentation/signinwithapple/authenticating-users-with-sign-in-with-apple). 공식 검색 결과에서 scope가 요청할 사용자 정보이고 이전 동의 이메일이 후속 토큰에 포함될 수 있음을 확인했다. 자동 도구로 동의 화면이나 실제 계정 인증 완료를 확인하지 않았다.
