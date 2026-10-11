# Caddy 구현 자체 검토

- Java/Kotlin 업무 기능·스키마·API DTO를 변경하지 않았다. 설치와 proxy는 공유 인프라 범위다.
- 실제 Caddy v2.11.7 validate, 별도 임시 포트의 실제 proxy 경로·POST/401·CORS/쿠키·502 오류 로그 제거 테스트 통과.
- 순수 방화벽 계획 검증 8건 통과. SSH·InstanceServices·Docker·Tailscale 기존 규칙을 보존하며 80/443만 최종 REJECT 앞에 추가한다.
- 실제 설치 재실행 후 live/saved rules에 포트별 한 규칙씩 확인. 전체 방화벽 restore·Docker 재시작을 하지 않았다.
- 앱 배포와 .deploy.lock 공유. 기존 Caddy 설정과 다른 웹서버가 있으면 자동 교체하지 않는다.
- 실제 공인 인증서 검증·peer OCI IP·CF-Ray 없음·health 200/UP·사용자 조회 401·CORS/OPTIONS·로그인 시작 302·metrics 404 확인.
- `.env` 600 유지. 비밀키·token·OAuth code/state·cookie 값은 검증 JSON/로그/PR에 넣지 않았다.
- 새 연결 0.75~2.00초가 남는 사실을 공개했다. 재사용 결과 0.12~0.47초를 초기 접속 성능으로 포장하지 않았다.
- 실제 카카오/Apple 계정의 최종 로그인·앱 심사·개인 학습 완료는 검증하지 않았다.
- 열린 FC-005/009/010/011/014/015/020/021 본문은 같은 origin/main 기준으로 확인했다. FC-021의 접속 경로만 이번 작업이며 실계정 E2E 후속을 닫지 않았다. 면제 보류·제품 결정·알림 후속은 이번 인프라 작업과 무관하다.
- :server:compileKotlin: BUILD SUCCESSFUL in 18s, 6 tasks up-to-date. core 업무 변경 없어 로컬 계산 테스트 재실행은 생략했으며 PR CI는 전체 회귀를 실행한다.
- git diff --check 통과. 자체 검토와 GitHub AI 원문, 사람 승인은 별개다.
