# FC-027 앱 인수·공개 데모의 후속 학습 위치

확정 비교는 `source.json`의 두 저장소 base/head와 `backend.patch`·`app.patch`다. backend는1198a49→b753586, app은95c4985→98d9877이다. 기존 ios-review-v1은 수정하지 않는다. 정책/카탈로그의 main 기준8d27496과 원문 SHA256을 source.json에 기록한다. 학습 상태는 UNRECORDED다.

### Java 기본기
- 해당 없음

### Effective Java
- 해당 없음 — 이번 후속은 Java 업무 정책을 새로 구현하지 않았다.

### 함수형 Java
- 해당 없음

### Kotlin 기본
- 12강, 코틀린에서 object 키워드를 다루는 방법 — 앱 `AppLinks.kt:8`·`AuthBrowser.ios.kt:4`의 싱글턴: 앱 진입 URL과 보류 중 인증 callback의 수명·공유 범위를 보기 위해.
- 14강, 코틀린에서 다양한 클래스를 다루는 방법 — 앱 `AuthBrowser.kt:3`의 sealed interface: 완료·취소·실패를 App.kt의 when에서 빠짐없이 처리하는 결과 모델을 보기 위해.
- 17강, 코틀린에서 람다를 다루는 방법 — 앱 `AuthBrowser.ios.kt:9`의 함수 타입과 completion 저장: Swift가 호출하는 람다·캡처한 화면 상태·한 번만 소비하는 순서를 보기 위해.
- 18강, 코틀린에서 컬렉션을 함수형으로 다루는 방법 — 앱 `DemoFixtures.kt:8`의 map·associateBy: 가상 사용자 목록을 참여자·세 시나리오 상태로 조립하는 변환을 보기 위해.

### Kotlin 고급
- 해당 없음

### 기술별 학습 포인트

#### Compose · UNMATCHED
- key와 LaunchedEffect의 수명 — 앱 `App.kt:50`·`App.kt:66`: 실제 클라이언트는 유지하고 데모 초기화 때 화면·스토어·내비게이션을 함께 다시 만드는 경계를 보기 위해.
- 측정 제약 전달 — 앱 `App.kt`의 Column/weight/Box/NavHost: 단위 테스트는 통과해도 화면 영역이 비는 문제를 실제 실행 검사에서 발견한 이유를 보기 위해.

#### Kotlin/Native · Swift · UNMATCHED
- expect/actual과 Swift 함수 타입 연결 — 앱 `AuthBrowser.kt`·`AuthBrowser.ios.kt`·`iOSApp.swift`: Kotlin Unit 반환 callback이 Swift에서 Void로 노출돼 컴파일이 실패했던 경계를 보기 위해.
- Foundation 확장 import — 앱 `DemoMessagePolicy.ios.kt`: NFKC 정규화 API의 명시적 import가 iOS 컴파일에 필요한 이유를 보기 위해.

#### 인증 · UNMATCHED
- ASWebAuthenticationSession 수명과 취소 — 앱 `AuthenticationBrowser.swift:13`·`:46`: presentation anchor·보류 세션·취소 뒤 늦은 completion을 UUID로 구분하는 경계를 보기 위해.
- PKCE와 중복 callback 소비 — 앱 `AppAuth.kt:32`·`:45`: verifier 없는 지연 전달을 거절하고 한 번의 티켓 교환만 수행하는 조건을 보기 위해.

#### HTTP · 데모 격리 · UNMATCHED
- 고정 HTML과 CSP — 서버 `ReviewDemoController.kt#demo`: 공개 링크의 query 미반영·쿠키 미발급·스크립트/외부 자원 제한을 보기 위해.
- 인증 가드 허용 목록 — 서버 `AuthenticationGuardSpec.kt#publicMethods`: 공개 안내 한 메서드를 추가하면서 다른 핸들러의 인증 누락은 계속 차단하는지 보기 위해.
- 데모와 실제 데이터 경계 — 앱 `V3Gateway.kt:43`·`DemoReviewTest`: API 클라이언트 금지·별도 스토어·실제 토큰 불변·미출시 기능 제한을 실제 기대값으로 검사하는 이유를 보기 위해.

구현·단위 테스트·시뮬레이터·실제 iPhone·배포·심사 승인은 서로 다른 검증 범위다. 사용자 학습 완료나 외부 리뷰 승인은 기록하지 않았다.
