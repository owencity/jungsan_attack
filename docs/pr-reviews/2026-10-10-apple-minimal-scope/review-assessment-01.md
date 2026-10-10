# 실제 자동 리뷰 대조

PR [#30](https://github.com/owencity/jungsan_attack/pull/30), 소스 `250b9848df38e3d5766c7214bab4d12049f7b9bd`.
원문과 실제 승인 상태는 [github-01.json](github-01.json)에 보존했다. 조회 시 정식 리뷰 0건, 일반 댓글 1건, 인라인 0건이다.
`owencity` 계정으로 게시된 `AI PR Review` 일반 댓글이며 사람 승인으로 표시하지 않는다.

| 자동 리뷰 지적·후보 | 구현자 대조와 처리 |
|---|---|
| LOW · Java Map.of는 null nonce에 NPE | Kotlin 함수의 nonce는 non-null String이며 실제 AuthFlowService.start는 서버가 생성한 43자 nonce를 넘긴다. 외부 nonce를 이 메서드에서 받지 않는다. Java도 null을 허용하는 계약을 선언하지 않았다. 생성 경로를 확인했고 현재 호출 경로의 결함으로 판단하지 않아 변경하지 않는다. |
| LOW · SameSite=None 쿠키에 Secure 필요 | AuthController.start는 secure=true일 때 Apple 쿠키에 None, false일 때 Lax를 사용한다. 운영 secure=true이며 신규 WEB/APP 테스트에서 None과 Secure·HttpOnly를 함께 검증한다. scope 제거가 이 분기를 바꾸지 않아 변경하지 않는다. |
| nonce 빈 값/null 테스트 후보 | 정책은 nonce를 생성·검증하는 함수가 아니라 인가 인자 구성이다. 생성은 AuthFlowService, 검증은 AppleTokens 및 challenge 조회 경로에 남겨두었다. 기존 다른 nonce 거절 테스트와 이번 실제 생성 state/nonce 길이 테스트가 통과했다. 입력 검증 책임을 옮기지 않는다. |
| 콜백 nonce가 다른 ID 토큰 테스트 후보 | AppleTokensSpec의 기존 audience·nonce·issuer 거절 테스트가 양쪽 언어에서 실행된다. 이메일 claim이 있어도 sub만 반환하는 신규 기대값 테스트를 추가했고 실패 경로를 삭제하지 않았다. |

실제 계정 로그인·콜백 성공과 프론트 개인정보 방침 파일 검수는 아직 미검증이다. 새 CTO 결정이 필요한 지적은 확인되지 않았다. 형식적인 자동 리뷰 승인 또는 사용자 스터디 완료를 기록하지 않는다.
