# 비회원 응답 PR 학습 위치

상태는 **UNRECORDED**다. 학습 완료 기록이 아니라 PR diff에서 지금 살펴볼 위치다.
base `09a33f023a8713d1b328ae609123de56f56d72ca`, 실행 코드 head
`9da531495af9876aa47ddd67f81e20c35a54da4e`를 고정한다. 이후 테스트·기록 보강은 별도 커밋이다.
목차는 확인한 원격 main `8d27496270e6c8adcb78b4580d68c150f9c3cce6`의 STUDY_CATALOG를 따른다.
책 본문을 읽었다는 의미는 아니다.

| 분류 | 목차 또는 기술 | 실제 변경 위치 | 지금 볼 관점 |
|---|---|---|---|
| Effective Java | EJ-49 · 8장 메서드 · 49 · 매개변수가 유효한지 검사하라 | guest/javaimpl/GuestPolicy.java:displayName, requireOrigin | 길이의 UTF-16/코드포인트 차이, 경계 검증과 업무 검증의 위치. Java/Kotlin이 같은 결과라는 것과 올바른 결과인 것은 별개 |
| Effective Java | EJ-76 · 10장 예외 · 76 · 가능한 한 실패 원자적으로 만들라 | gathering/GatheringService.kt:guestJoin·respondAs | 초기 INSERT 후 뒤쪽 답변 실패 때 신원·명단·알림까지 함께 롤백되는지. DB 원자성은 실제 MySQL probe로 별도 확인 |
| Kotlin 기본 | KB-02 · 2강 · 코틀린에서 null을 다루는 방법 | guest/GuestTokens.kt:identity, guest/GuestBrowser.kt:resolveArgument | 플랫폼 타입 claim, nullable 결과와 Elvis 예외가 인증 실패를 어떻게 전달하는지 |
| 기술별 학습 포인트 | JWT의 용도 분리와 자격 혼동 방지 | guest/GuestTokens.kt:issue·identity, GuestTokensSpec | 같은 서명 키를 쓰면서도 회원/비회원 토큰을 서로 수용하지 않는 이유, purpose·subject·expiration 검사 |
| 기술별 학습 포인트 | 쿠키 Path·SameSite·Origin과 CSRF | guest/GuestController.kt:startSession·join·respond | httpOnly는 JS 접근 제한이고 요청 출처 검사가 아님. 같은 사이트의 다른 호스트도 Origin이 다르면 거절하는 이유 |
| 기술별 학습 포인트 | Spring 트랜잭션 프록시와 내부 호출 | gathering/GatheringService.kt:guestJoin→guestMe | 내부 readOnly 메서드 호출이 새 트랜잭션을 만들지 않는 점과 외부 클래스 수준 쓰기 트랜잭션의 적용 경계 |
| 기술별 학습 포인트 | MySQL 행 잠금·유일 키와 멱등성 | guestJoin, 021-guest-participations.yaml, scripts/verify-guest-participation.py | 동일 단위 중복과 다른 단위 동시 참여에서 unit→gathering 잠금이 무엇을 직렬화하는지, 이름을 유일 키로 쓰지 않는 이유 |

관련 경로의 접두어는 Kotlin `server/src/main/kotlin/app/jeongsan/server/`, Java
`server/src/main/java/app/jeongsan/server/`다. 줄 번호보다 고정 commit의 함수와 실제 diff를 함께 본다.
