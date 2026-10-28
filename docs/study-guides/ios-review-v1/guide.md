# FC-027 iOS 인증 세션 복귀 학습 위치

base `8d27496270e6c8adcb78b4580d68c150f9c3cce6`, code head `a31bb2007bd795c04395ff6e5c318c9979047923`. 정책·카탈로그는 동일한 main base 원문을 보존한다. 앱 소스 수정·실기기 성공·데모 완성은 이 백엔드 diff에 포함되지 않는다.

### Java 기본기
- 해당 없음
### Effective Java
- Item 49, 매개변수가 유효한지 검사하라 — `after/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java`의 `appReturnMode`·`appSessionReturnLink`: 복귀 선택과 고정 링크의 티켓 문자·길이 검증을 확인한다.
### 함수형 Java
- 해당 없음
### Kotlin 기본
- 2강, 코틀린에서 null을 다루는 방법 — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt`의 `appReturnMode`: 생략한 모드를 기존page로 선택하는 Elvis 연산의 호환성 의미를 확인한다.
### Kotlin 고급
- 해당 없음
### 기술별 학습 포인트
#### Spring MVC·네이티브 인증 복귀
- 선택형303과 기존200 분리 — `AuthController.finish`, `AuthSessionReturnSpec`: 세션 복귀는 저장된 mode로만 선택하며 콜백query·UA로 바꾸지 않는 이유를 확인한다.
#### MySQL·롤백 호환성
- state FK CASCADE 표식 — `AuthFlowService.start/finish`, `020-auth-session-return.yaml`, `AuthDatabaseProbe`: 만료·소비 때 표식을 함께 제거하고 이전 VALUES 전체 INSERT를 유지하려고 부모컬럼 추가를 피한 이유를 확인한다.
- 실제 업그레이드 이력 검사 — `scripts/verify-boot-upgrade.py`: 과거 checksum 불변과029/030 두개만 순서대로 추가하는 검증을 구분한다.
#### OAuth·검증 범위
- PKCE·일회용·상관 쿠키 유지 — `AuthDatabaseProbe`, `AuthSessionReturnSpec`: DB·HTTP 검증과 iPhone 인증 세션 E2E가 같은 증거가 아닌 이유를 확인한다.

경로가 짧게 쓰인 파일은 after/ 아래 같은 저장소 경로에 있다. 실제 줄 번호와 파일은 고정 스냅샷에서 확인한다. 사용자의 학습 완료는 미기록이다.
