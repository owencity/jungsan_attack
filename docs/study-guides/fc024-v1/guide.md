# FC-024 PR #42 학습 방향표

기준: base defd9d11ef90b2a9200090669974faf0f13807a5, code head b71d7766558dc402355f6ec1790b87756ebbbf93. 정책·카탈로그는 base 원문을 original에 고정했다.
changes.diff·before/after·manifest의 원본 blob/해시를 함께 읽는다. 사용자 학습 상태는 UNRECORDED다.
이후 리뷰·FC 상태 문서 커밋은 코드 기준과 구분하며 운영 배포를 이 고정 자료만으로 주장하지 않는다.

## Effective Java
- Item 49, 매개변수가 유효한지 검사하라 — `server/src/main/java/app/jeongsan/server/moderation/javaimpl/ModerationPolicy.java:21`: 메시지 금지·길이·내용 검사 순서와 실패값의 경계를 읽을 위치다.
- Item 76, 가능한 한 실패 원자적으로 만들라 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationService.kt:41`: 신고 저장·전체 숨김·커밋 후 로그가 한 접수 결과를 구성하는 지점이다.

## Kotlin 기본
- 2강, 코틀린에서 null을 다루는 방법 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationService.kt:72`: 탈퇴한 참여자의 nullable 사용자 ID를 허용 가능한 차단 대상으로 오해하지 않게 하는 분기다.
- 12강, 코틀린에서 object 키워드를 다루는 방법 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationPolicy.kt:7`: Spring 서비스와 독립된 정책의 싱글턴 호출 경계를 확인할 위치다.
- 14강, 코틀린에서 다양한 클래스를 다루는 방법 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationDto.kt:16`: HTTP 요청·응답 데이터와 상태 변경 서비스를 구분해서 읽는 위치다.

## Kotlin 고급
- 20강, 코틀린의 어노테이션 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationDto.kt:8`: @field 대상이 Bean Validation에서 사유와 양수 ID 검증에 영향을 주는 지점이다.

## 기술별 학습 포인트
- Unicode 정규화·코드포인트 — `server/src/main/java/app/jeongsan/server/moderation/javaimpl/ModerationPolicy.java:14`: NFKC·문자/숫자 필터와 UTF-16 경계 공백 처리가 단순 바이트 치환과 달라지는 지점이다.
- MySQL 행 잠금·격리·유일 제약 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationService.kt:41`, `server/src/main/resources/db/changelog/019-ugc-safety.yaml:19`: 서로 다른 신고자/같은 신고자의 동시 요청이 집계 결과를 바꾸는 경계를 실제 DB 검증과 대조한다.
- Spring 트랜잭션 이벤트 AFTER_COMMIT — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationLog.kt:15`: 롤백 접수와 확정 로그, 프로세스 종료 때 로그 유실 가능성을 구분하는 지점이다.
- SQL NOT EXISTS·배치 조회 — `server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:376`: 개인별 숨김을 단일 timeline 쿼리에 적용하고 SYSTEM·정산 데이터는 유지하는 조건이다.
- FK CASCADE / SET NULL — `server/src/main/resources/db/changelog/019-ugc-safety.yaml:22`: 작성자·신고자·차단자 탈퇴가 서로 다른 개인정보 삭제 결과를 만드는 부분이다.
- JPA updatable=false — `server/src/main/kotlin/app/jeongsan/server/user/User.kt`의 messageBannedAt: 로그인 save와 운영 SQL의 메시지 금지 소유권을 대조할 지점이다.
- 개인정보 최소 로그 — `server/src/main/kotlin/app/jeongsan/server/moderation/ModerationLog.kt:17`: 확정된 식별 메타데이터와 본문·이름·계좌를 구분하는 출력 경계다.
