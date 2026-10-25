# OCI 점검 문서 자체 검토

- 실제 SSH 점검: 80/443 리스너 없음, nginx/caddy/apache2 unit/실행 파일 없음, app loopback 18080, 호스트 방화벽 공인 80/443 허용 없음.
- 외부 TCP 80/443 3회씩 timeout. TCP 22의 108~150ms는 HTTPS 성능으로 기록하지 않았다.
- 실제 메타데이터로 OCI 싱가포르 확인. OCI Security List/NSG는 미확인으로 기록했다.
- 앱 처리 시간은 Micrometer 차이이며 과거 예외 로그의 시간으로 쓰지 않았다.
- Caddy 후보는 설치·validate·인증서 발급·직접 경로 회귀를 실행한 것으로 기록하지 않았다.
- 기존 webhook/dev API를 유지하고 해당 정산 API의 DNS만 전환하는 안을 적었다. 운영 IP/비밀 설정 값은 문서에 새로 공개하지 않았다.
- 설명 노트의 책/강의 번호와 페이지는 해당 origin/main 카탈로그에서 확인했다. 학습 체크는 미완료로 유지했다.
- 문서만 추가되어 Java/Kotlin 구현·회귀 시험을 새로 실행하지 않았다. 실제 코드 변경이 없다는 것과 서버 기능 검증 완료를 구분했다.
- git diff --check: 통과.
- 이번 PR의 실제 AI 리뷰는 GitHub 원문으로 별도 보존하며, 자체 검토를 사람 승인으로 기록하지 않는다.
