# Apple 로그인 개인정보 요청

## FC-022 Apple 이름·이메일 동의 요청 제거

- **상태:** 반영됨 (API v10·양쪽 언어 정책·서버 130건 검증, PR 병합·운영 적용·실계정 검증은 별도)
- **바뀐 흐름:** Apple 로그인에서 이름·이메일 제공 동의를 요청하지 않는다. 실명은 기존 L2 이름 등록 화면에서 받는다.
- **백엔드 영향:** Apple 인가 URL에서 `scope=name email`을 제거한다. 계정 연결에는 검증된 `sub`만 사용하고 이름·이메일은 사용하거나 저장하지 않는다. 이전 동의 이메일 claim은 무시한다.
- **기존 문서와 충돌:** AUTH_RELEASE가 이름·이메일 요청을 명시했지만 서버 사용 범위 및 CTO가 전달한 프론트 개인정보 안내는 식별자 기반이다. 프론트 방침 파일 자체는 이 저장소에 없어 문구 검수 완료로 표시하지 않는다.
- **결정 근거:** 2026-10-10 CTO 요청 — 서버가 사용하지 않으면 scope를 제거한다. 실제 ProviderGateway·AppleTokens·AuthFlowService·User 확인에서 이름·이메일 사용·저장 없음.
- **반영할 곳:** API v10·AUTH_RELEASE·AuthPolicy Kotlin/Java·ProviderGateway·회귀 테스트. 프론트 네이티브 구현이 별도 scope를 요청한다면 프론트에서도 확인한다.
- **백엔드 반영·근거:** AuthPolicy.kt/java는 인가 인자를 독립 구현하고 공유 ProviderGateway가 사용한다. ApplePrivacySpec 5건과 AppleTokensSpec의 이전 동의 이메일 1건을 추가했으며 서버 전체 130건에서 실패·오류·건너뛰기 0, 서버 컴파일 성공. state·nonce·form_post·안전한 상관 쿠키·서명 검증·탈퇴용 암호화 refresh token 저장은 유지한다. PR 병합·운영 배포와 실제 사용자 로그인은 별도 확인한다. FC-021 전체 연결 시험 완료로 표시하지 않는다.
