# 앱 로그인 진단·로그아웃 자체 리뷰

기준 `89fd5ee8f69f8d4f639389047988bba2f5ec384e`. Codex 자체 점검이며 외부 리뷰·실기기 로그인 성공과 별개다.

- 범위: 로그아웃은 이미 구현돼 있다. 신규 URL 없이 no-store와 WEB/APP·현재 자격 범위 검증을 보완한다. 재호출401 계약을 변경하지 않는다.
- 인증: 기존 LoginUser·APP/WEB 분리·state/브라우저 해시·nonce·만료·잠금·티켓 소비를 유지한다. 콜백 경로·딥링크·CORS·cookie 속성·보안 검증을 완화하지 않는다.
- 로그: 허용 목록 provider/client와 고정 실패 enum만 출력한다. code/state/해시/cookie/nonce/ticket/JWT/userId·예외 본문·URL·PII는 기록하지 않는다. 로그백 캡처로 정확한 출력·throwable 미출력·문자열 주입 배제를 검증했다.
- 원자성: 만료는 기존 AuthPolicy.validUntil을 사용하며 예외 응답과 rollback을 유지한다. success 로그는 서비스 트랜잭션 반환 뒤이며 실제 브라우저·앱 성공이 아니다.
- 중복·동시성: 기존 auth_revocations PK+upsert 보존. 실제 HTTP 검사에 다른 기기 유지·반복401·동시204/401·무효화 행1개를 추가했다. CI 실행 결과는 PR 본문에 별도 기록한다.
- 조회/스키마: 추가 DB 조회·테이블·migration·의존성·운영 기본값 없음. N+1·새 외부 호출 없음.
- Java/Kotlin: 업무 인증 정책은 기존 양쪽 AuthPolicy 구현을 그대로 사용한다. 새 코드는 공유 HTTP 응답·진단 인프라여서 Java Spring Bean/URL을 중복 등록하지 않는다.
- SOLID: 실패 진단 출력은 AuthCallbackLog에 두고 기존 서비스는 인증 검증을 유지한다. 별도 정책 엔진·인터페이스·추정 브라우저 중계 화면은 추가하지 않는다.
- 검증: 로컬 서버184건 성공, 실패/오류/skipped0; compileKotlin·bootJar·Python 문법·git diff --check 통과. 실제 DB·HTTP는 최신 CI 결과로 구분한다.
- Android: 앱95c4985 main 읽기 전용 대조·에뮬레이터 딥링크 등록 확인. 운영 로그에 예외가 없다는 사실로 로그인 성공을 추정하지 않는다. 오류 문구/재현 시각이 없어 root cause는 미확정이다. 앱 저장소를 수정하지 않았다.
- FC: 문서 기준 `560fee34cb704c40c3273238eb2bd38e31edd719`, 열린005/009/010/011/014/015/020/021을 확인하고 이번026 초안을 추가했다.021 실제 앱 연결 후속 외 다른 미결·보류 기능은 유지한다.012 결정 필요는 구현하지 않는다.025 Apple CORS 예외는 보존한다.

CI·PR 병합·배포와 실계정 재현은 각각 PR에 기록한다. FC-026은 로그인 원인과 실제 복귀가 해결되지 않아 열림이다.
