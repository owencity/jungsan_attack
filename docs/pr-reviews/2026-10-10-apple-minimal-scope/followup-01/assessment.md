# 기록 추가 뒤 자동 리뷰 대조

PR #30 · 조회 head `26855a533c69cd43659db1888d59c80468067868`, 업무 소스는 고정 가이드의 250b984와 동일하다. 실제 원문은 github.json에 보존했다. 일반 AI 댓글 2건, 정식 승인·인라인 0건이다. 사람 승인으로 표시하지 않는다.

- 새 LOW nonce 사전 검증: 실제 AuthFlowService.start는 SecureRandom 32바이트를 base64url 43자로 생성해 요청·DB에 같은 nonce를 사용한다. 클라이언트가 빈 nonce를 주입하는 입력이 없다. AppleTokens는 DB nonce와 ID 토큰 nonce 일치를 검사하고 기존 거절 테스트가 통과한다. 인자 구성 함수에 생성·검증 책임을 옮길 현재 결함은 확인되지 않아 변경하지 않는다.
- Java null nonce: 기존 대조와 동일하다. Kotlin non-null 계약과 실제 생성 호출 경로를 확인했고 Java Map.of의 null 거절을 허용 계약으로 바꾸지 않는다.
- 이전 동의 이메일 로깅: ProviderGateway·AppleTokens·AuthFlowService에는 제공자 응답·ID 토큰·claims 원문 로깅이 없다. 제공자 오류는 일반 UnauthenticatedException으로 변환된다. 이번 수정에 로그를 추가하지 않았다. 미래 디버그 로그 확대는 별도 검토 대상이다.
- 실제 기존 Apple 계정 로그인 후보: 실제 로그인은 아직 미검증이다. 첫 가입의 displayName 미등록은 L2로 이어지고 이미 L2에서 등록한 기존 계정의 실명을 지우면 안 된다. users INSERT/UPDATE는 display_name을 쓰지 않는다. 자동 리뷰의 '항상 null' 기대는 기존 실명 등록 계정에 적용하지 않는다.

최종 기록을 포함한 CI 38047711705가 성공했다. 이후 이 대조/원문 추가는 문서만이며 최신 커밋 CI도 병합 전에 확인한다. 새 CTO 제품 결정·의존성·인증 구조 변경은 없다. 이전 아카이브와 학습 진도는 보존한다.
