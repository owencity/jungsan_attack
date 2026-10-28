# FC-027 앱 인수·데모 안내 후속 자체 점검

이전 고정 보존 자료 `ios-review-v1`과 자체 점검을 덮어쓰지 않는다. 비교 시작점은1198a49, 이번 변경은 공개 안내 HTTP와 앱 PR1 연결이다. 자동 외부 리뷰 승인과 자체 점검을 구분한다.

- **문제·해결:** CTO가 앱 담당자의 휴가로 앱 구현을 요청했다. 앱 PR1은 시스템 인증 세션·공개 체험을 구현하며, 서버는 배포할 수 있는 고정 HTTPS 안내와 앱 딥링크를 제공한다.
- **공개 범위:** `ReviewDemoController#demo` 한 메서드만 인증 가드 허용 목록에 추가했다. DB·사용자·비밀 값·임의 목적지를 받지 않는 공유 HTTP 표시 코드이므로 별도 Java 업무 정책 모델을 만들지 않았다. 기존 Java/Kotlin 인증 정책의 독립 구현은 유지한다.
- **입력·노출:** query/UA를 반영하지 않는다. 고정HTML·고정 앱 스킴·UTF-8·no-store/no-referrer/nosniff·script-src none을 검증한다. 토큰·쿠키·티켓을 발급하지 않는다.
- **영향 없음:** DB 쓰기·외부 요청·목록 조회·트랜잭션·금액 계산이 없어 동시성/멱등성/N+1/스키마 변경은 해당 없다. 기존 auth020/030과 checksum은 수정하지 않았다. 새 라이브러리·prod 기본값 없음.
- **기능·호환:** 앱 PR1의 가상 상태는 운영 API 연결을 금지한다. 공유는 운영에 없는 가상 방 토큰 대신 안내 링크를 쓴다. Android의 기존 인증 복귀는 유지하고 iOS의 선택적303만 사용한다.
- **검증:** 서버204건 실패/오류/건너뜀0, compileKotlin/bootJar 성공, 인증 가드 통과. 신규2건은 공개200·보안 헤더와 공격 query/UA 미반영을 확인한다. 실제HTTP 회귀에 안내200/쿠키 미발급2검사를 추가했다. 실제HTTP 최신 결과는 CI 완료 후 기록한다.
- **남은 범위:** 앱의 iOS Kotlin/Swift 빌드·시뮬레이터, 양쪽 PR 리뷰·병합·배포, iPhone의 카카오/Apple 로그인·데모·실제 탈퇴, App Store Connect 제출. 기기와 운영 성공을 단위 테스트로 대체하지 않는다.

## 후속 학습 위치 · UNRECORDED

이 후속 HTTP 표시에는 Java 기본기·Effective Java·함수형 Java·Kotlin 기본/고급의 새 핵심 항목을 억지로 매핑하지 않는다. 기술별 항목은 Catalog 미등록인 UNMATCHED이며 직접 구현 diff를 기준으로 본다.

- 기술별 학습 포인트 · UNMATCHED · HTTP CSP — `ReviewDemoController.kt#demo`: 외부 리소스·스크립트·폼·frame을 막는 정책과 고정 커스텀 스킴 링크의 브라우저 동작을 함께 보기 위해.
- 기술별 학습 포인트 · UNMATCHED · 인증 가드 — `AuthenticationGuardSpec.kt#publicMethods`: 로그인 이전 안내만 공개하면서 다른 컨트롤러의 인증 누락을 계속 차단하는 허용 목록 검증을 보기 위해.
- 기술별 학습 포인트 · UNMATCHED · 네이티브 인증 수명 — 앱 PR1 `AuthenticationBrowser.start/cancel`: 인증 세션 객체·presentation anchor·취소 후 늦은 callback이 새 로그인에 영향을 주는 경계를 보기 위해.

학습 완료 기록은 없다. 확정 소스 commit과 diff는 같은 폴더의 후속 보존 파일에 별도로 기록한다.
