# 구현자 점검

- 기준: `c11c9b9af1dfd76c7daf90b9a4e6f92f23c00ce4` → `d3c7e47ccb879d2b65aefb220a3900f976d3e186`, 파일 15개.
- CTO 결정: Codex가 OCI .env와 카카오·독립 내부 키 생성, Apple 로그인 키는 CTO가 서버에 입력. Apple 미설정만 503 분리. 배포용 p8은 서버에 전송하지 않았다. 추가 CTO 판단 항목은 없다.
- Java/Kotlin: providerAvailable을 독립 구현했고 이름·설정 개수·빈 문자열·공백·NBSP·잘못된 제공자·완전한 설정을 비교했다. Java 문자열 공백 판정을 Kotlin과 맞췄다. Spring/HTTP 어댑터는 기존 공용 구조이며 중복 Bean·URL을 추가하지 않았다.
- 보안/동시성: 제공자 설정은 기동 시 고정되며 Apple 설정 미완료는 인가·토큰 교환·해제의 외부 호출 전에 503이다. 인가 실패가 DB challenge를 생성하지 않는 테스트도 통과했다. 기존 state/nonce/서명/티켓/쿠키/Bearer/로그아웃/탈퇴 가드를 변경하지 않았다.
- 가용성: 운영 카카오 필수 설정은 계속 검증한다. 모든 Apple 값을 채운 잘못된 개인키는 기동 시 실패한다. .env의 키 이름은 필수이며 Apple 값만 빈 상태를 허용한다. 키를 채운 뒤에는 재배포가 필요하다.
- 검증: Server 전체 124건, 신규 8건, 실패·오류·건너뛰기 0. bootJar·actionlint·Bash 문법 검사 통과. 실제 OCI compose config --quiet에서 새 설정/빈 Apple 값을 검증했다. 실제 prod/MySQL/HTTP는 최신 PR CI에서 검증한다.
- 범위: API v9·AUTH_RELEASE·DEPLOY·FC-021·최신 AGENTS/DEVLOG 정합성을 대조했다. 새 의존성·스키마·Core 계산·공개 핸들러 변경이 없다. FC-021은 실제 제공자·웹/앱 연결 시험까지 열림이다.
- 운영: .env 권한 600, 카카오 설정·4개 내부 키·새 콜백 준비. Apple 값은 발급 전 빈 상태이며 실제 로그인 성공으로 표시하지 않는다. Tunnel 추가 완료는 CTO 확인이며 기존 노트북 API·웹훅 경로는 보존한다.
- 확인된 병합 차단 결함 없음. 최종 CI 전체 성공·실제 리뷰 기록 보존 후 병합한다. 실제 운영 배포·카카오 콜백·Apple 로그인은 별도 확인한다.
