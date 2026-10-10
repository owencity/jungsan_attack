# 자동 리뷰 댓글의 구현자 검토

검토 head `9004ad819656b19e91956f6d8cc159d8997ee529`. [조회 원문](github-review-snapshot-02.json)에는 일반 댓글 2건, 정식 승인 리뷰 0건이 있다. 댓글의 작성 계정은 원문 그대로 보존하며 사람이 승인했다고 표시하지 않는다. 댓글에는 reviewed commit SHA가 없어 최신 diff를 검토했다고 단정하지 않는다.

| 자동 리뷰 관찰 | 실제 코드·검증 대조 | 처리·추천 |
|---|---|---|
| Java 설정 문자열 null이면 NPE | `AuthPolicy.java`의 입력 계약과 Kotlin 대응은 `List<String>`이며 실제 `RestProviderGateway`는 non-null `@Value String` 3/5개로 목록을 구성한다. 새 Java 구현은 학습·동등성 시험용이며 공개 HTTP 입력으로 직접 호출되지 않는다. 계약을 위반해 null을 넣을 경우 NPE 가능성 자체는 맞다. | 현재 설정 경로의 병합 차단 결함으로 보지 않는다. 향후 nullable Java 설정 API를 추가하면 두 언어에서 null 거절/false 반환 계약을 먼저 맞추고 동등성 테스트를 추가할 것을 권한다. 이번 운영 설정 변경에서 nullable 계약을 임의로 늘리지 않았다. |
| 임의 제공자 요청이 503일 수 있음 | `AuthController.login`·`appleLogin`은 각각 KAKAO·APPLE 상수만 gateway에 전달한다. 외부 provider 경로 변수나 Google 로그인 핸들러가 없다. 내부 `providerAvailable`의 unknown=false는 기존 신규 테스트로 검증했다. | 현재 공개 인가 API에서 이 경로는 도달하지 않는다. 나중에 범용 제공자 핸들러를 만들면 미지원 값 400/404와 설정 미완료 503을 분리할 것을 권한다. 현재 없는 핸들러의 응답을 시험 성공으로 표시하지 않는다. |
| Apple 일부 설정만 있으면 기동 가능 | 미완료 Apple만 503으로 분리하는 CTO 결정대로 한 값이라도 비면 사용 불가다. 신규 테스트는 각 필드 누락·공백·NBSP를 검사하고 완전한 값의 잘못된 개인키는 기동 시 거절한다. | 의도된 발급 대기 정책이다. 누락 상태는 설정 점검에서 키 이름만 보고한다. 초기 입력을 단계적으로 진행하는 동안 카카오를 중단시키는 별도 정책으로 바꾸지 않았다. |
| Apple revoke 실패로 탈퇴/재시도 적체 가능 | `AccountDeletionService.delete`는 저장된 `auth_credentials`가 있을 때만 암호화 revoke 작업을 생성하며 외부 revoke를 탈퇴 트랜잭션에서 호출하지 않는다. `AuthMaintenance.run`은 별도 작업에서 실패를 잡아 attempts·next_attempt_at을 갱신한다. 대기는 지수형이 아닌 60×attempts초, 최대 3600초의 선형 증가다. 작업마다 SKIP LOCKED, 조회는 20개 제한, 로그는 작업 ID만 남긴다. 기존 실제 DB probe가 실패 후 계정 복구 없이 재시도 보존·이후 성공 삭제를 검증한다. | 카카오 탈퇴가 Apple 미설정 503으로 롤백되는 경로는 없다. 초기 운영 DB에는 기존 Apple 계정이 없다. 향후 Apple 사용 이후 키를 제거하면 재시도가 지속될 수 있으므로 작업 수·키 복구를 운영 점검 대상으로 둔다. 설정 공백을 영구 실패로 보고 작업을 삭제하는 변경은 하지 않았다. |
| Compose unset 변수는 거절 | `${VAR?}`는 이름을 필수로 선언하게 하고 빈 값은 허용한다. 새 OCI .env는 Apple 네 키를 모두 빈 값으로 선언했고 실제 OCI compose config와 값 노출 없는 설정 점검을 통과했다. `core_ready=true`, `apple_ready=false`, 권한 600이었다. | 의도된 fail-fast다. DEPLOY 템플릿과 실제 .env 선언을 맞췄다. 불필요한 prod 기본값을 추가하지 않았다. |

실제 최초 DB 인증 실패는 별도 [후속 점검](followup-01.md)으로 수정했으며 [CI 성공](ci-success-02.json)을 확인했다. Core/Server 테스트·실제 MySQL/HTTP·prod 제공자 6건·DNS 검증 모두 통과했다. 리뷰 기록만 저장한 다음 커밋도 최신 CI 전체 성공 후에만 병합한다.
추가 CTO 결정 항목과 현재 확인된 병합 차단 결함은 없다. 실제 운영 배포·카카오 사용자 콜백·Apple 로그인 활성화는 아직 별도다.
