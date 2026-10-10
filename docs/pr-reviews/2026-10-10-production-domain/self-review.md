# 구현자 점검 — 운영 호스트 분리

- 작성일: 2026-10-10. 검토 범위 `ceb2ef335b01b913d7ffdbcd5531f6b153bf8a3c` → `b659f4a47dd0c3fca55c4b083bc29216dd75a18b`의 파일 9개.
- CTO 결정: jungsan-api.devkdk.com만 OCI에 추가, 노트북 api.devkdk.com과 웹훅 8080은 유지. CTO가 Tunnel·제공자 콘솔·운영 .env를 준비한다. 추가 CTO 판단 항목은 없다.
- FC 기준: 위 origin/main의 열린 FC-005·009·010·011·014·015·020·021을 기존 출시 조사와 대조했다. 이번 관련 항목은 FC-021이며 문서·DNS 범위를 정정한다. 면제·알림·프론트 연결 후속은 그대로 열림을 유지한다.
- DNS: 상수를 새 호스트로 변경했고 조회/쓰기/사후 검증이 모두 그 상수를 사용한다. 신규 테스트는 승인 주소를 문자 그대로 검증하고 api/webhook 반환 시 쓰기를 거절한다. 기존 생성·재시도·중복·다른 이름·A 레코드·쓰기 실패·ID 검증도 유지했다. 실제 API를 호출하는 검증은 아니다.
- 운영 계약: .env 예시·카카오 Redirect URI·Apple Domain/Return URL·Vercel base·health·Tunnel 추가 절을 필드별로 비교했다. Spring prod는 환경변수이므로 Java/Kotlin 비즈니스 로직·API 필드·마이그레이션 변경은 없다.
- 동시성/재실행: DNS workflow는 main 수동 실행이고 기존 concurrency 직렬화·동일 값 무변경·쓰기 후 재조회가 유지된다. 다른 실행이 동시에 외부 DNS를 편집하는 상황까지 잠그는 구현은 아니다.
- 검증: DNS 가짜 API 9건 통과(실패 0), actionlint workflow 3개 오류 0, git diff --check 통과. 최종 PR CI 링크는 PR 본문과 실제 실행 결과에 추가한다.
- 실제 배포: PR #27 main SHA ceb2ef3 실행 38030304729의 테스트·빌드·전송 성공, 배포 실패·health 미실행. OCI에서 .env 부재와 정산어택 컨테이너 미생성을 확인했고 deploy.sh는 해당 파일을 가장 먼저 요구한다. 키 값은 조회하지 않았다.
- 미검증: 대시보드 추가·개발자 콘솔 저장·Vercel 재배포·운영 health·실제 카카오/Apple 로그인·TestFlight 앱 티켓 교환. 운영 배포 완료나 공식 스토어 출시로 보고하지 않는다.
- 리뷰 결론: 이 diff에서 확인한 병합 차단 결함은 없다. 기존 서비스 변경을 막는 회귀 테스트와 실제 최종 CI를 조건으로 병합한다. 자체 판단이며 n8n/Claude/CTO 코드 리뷰 승인이 아니다.
