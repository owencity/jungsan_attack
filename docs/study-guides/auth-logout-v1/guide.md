# 앱 로그인 진단·로그아웃 PR 학습 위치

base `560fee34cb704c40c3273238eb2bd38e31edd719` / code head `89fd5ee8f69f8d4f639389047988bba2f5ec384e`. 보존한 정책·카탈로그는 base 원문이다. 사용자 학습 진도는 미기록이다.

### Java 기본기
- 해당 없음
### Effective Java
- 해당 없음
### 함수형 Java
- 해당 없음
### Kotlin 기본
- 2강, 코틀린에서 null을 다루는 방법 — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthFlowService.kt`의 `finish`: 누락된 쿠키를 거절한 뒤 non-null로 사용되는 경로를 확인한다.
- 7강, 코틀린에서 예외를 다루는 방법 — `AuthFlowService.finish`의 `reject`와 catch/rethrow: 진단을 추가하면서 기존401/503과 트랜잭션 실패 동작을 보존하는 지점을 확인한다.
### Kotlin 고급
- 해당 없음
### 기술별 학습 포인트
#### HTTP·로그아웃
- 204 빈 본문과 쿠키 만료·no-store — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthController.kt`의 `logout`: 토큰 무효화와 클라이언트 로컬 상태 정리의 경계를 확인한다.
- 현재 자격과 다른 기기 자격의 분리 — `after/scripts/verify-auth-release.py`의 `logout_web`, `other_device`: 같은 계정의 자격별 무효화 계약을 확인한다.
#### MySQL·동시 요청
- 중복 무효화와 인증 검사 시점 — `verify-auth-release.py`의 `concurrent_logout`:204/401 경합과 무효화 행 한 개를 실제 DB로 검증하는 이유를 확인한다.
#### OAuth·진단 로그
- 상관 쿠키·state·제공자 거절 구분 — `AuthFlowService.finish`: CSRF 검증을 완화하지 않고 실패 위치만 구분하는 경계를 확인한다.
- 메타데이터 허용 목록과 예외 본문 배제 — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthCallbackLog.kt` 및 `after/server/src/test/kotlin/app/jeongsan/server/user/AuthCallbackLogSpec.kt`: 인증 값·예외 본문·로그 주입이 출력되지 않는 근거를 확인한다.
- 트랜잭션 커밋과 브라우저 복귀의 구분 — `AuthController.finish`의 `completed`: 완료 로그가 실제 Android 앱 복귀 증거를 대신하지 않는 이유를 확인한다.
