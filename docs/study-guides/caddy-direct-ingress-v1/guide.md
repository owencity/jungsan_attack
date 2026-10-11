# Caddy 직접 HTTPS PR 스터디 가이드

- 정책·목차는 실제 origin/main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420` 원문을 보존했다.
- sourceBase: `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`, sourceHead: `04a4750bd60bf36696c42466a409ab2cfc6c8a8f`. [고정 diff](changes.patch)의 `+` 줄부터 보고 [after](after) 원문과 대조한다.
- Caddy·설치·방화벽은 인프라 변경이다. Java/Kotlin 업무 구현·책 목차를 억지로 연결하지 않는다.
- 아래 기술별 주제는 정책 §5에 따라 실제 diff에서 골랐다. 책 번호·강의 제목·페이지를 새로 만들지 않았다.
- 리뷰 원문·자체 검토는 [리뷰 기록](../../pr-reviews/2026-10-12-caddy-direct-ingress)이며 학습 완료와 구분한다.

## Java 기본기

- 해당 없음

## Effective Java

- 해당 없음

## 함수형 Java

- 해당 없음

## Kotlin 기본

- 해당 없음

## Kotlin 고급

- 해당 없음

## 기술별 학습 포인트

### Caddy·ACME

- 같은 도메인의 자동 HTTPS와 DNS only 직접 연결 — [`infra/caddy/Caddyfile:20`의 `jungsan-api.devkdk.com {`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/infra/caddy/Caddyfile#L20): 호스트 선언이 자동 인증서 발급을 시작하는 지점이다. DNS가 Tunnel을 가리키면 왜 Caddy의 80/443 검증이 실패하는지 보고, A 레코드와 proxy 상태를 각각 확인한다. [공식 자료](https://caddyserver.com/docs/automatic-https)

### Caddy·라우팅

- handle의 경로 보존과 공개 엔드포인트 제한 — [`infra/caddy/Caddyfile:22`의 `@application path`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/infra/caddy/Caddyfile#L22): /api/*와 정확한 health만 넘긴다. handle_path로 바꾸면 콜백 경로가 어떻게 달라지는지, metrics가 앱까지 도달하지 않는지 실제 Caddy 테스트의 denied 요청 수 검증과 함께 본다. [공식 자료](https://caddyserver.com/docs/caddyfile/directives/handle)

### Caddy·로그

- 접근 로그와 오류 로그의 차이 및 중첩 필드 제거 — [`infra/caddy/Caddyfile:9`의 `request>uri delete`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/infra/caddy/Caddyfile#L9): access log를 켜지 않아도 upstream 실패의 오류 로그에 쿼리·헤더가 들어갈 수 있다. verify-caddy-ingress.py에서 실제 502를 만들고 URI·헤더·DUMMY_TOKEN이 제거됐는지 검사하는 부분을 대조한다. [공식 자료](https://caddyserver.com/docs/caddyfile/directives/log)

### iptables

- 첫 일치 규칙과 REJECT 앞 삽입·멱등 실행 — [`scripts/ingress/allow-caddy-ports.py:17`의 `def plan_rules(`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/scripts/ingress/allow-caddy-ports.py#L17): 80/443 허용을 최종 REJECT 뒤에 붙이면 왜 도달하지 않는지 본다. 중복·SSH 누락·예상 밖 레이아웃을 먼저 거절하고 재실행 시 빠진 규칙만 추가하는 테스트를 diff에서 확인한다. [공식 자료](https://manpages.ubuntu.com/manpages/noble/man8/iptables.8.html)

### 배포·동시성

- 앱 배포와 Caddy 설치가 공유하는 flock — [`scripts/ingress/install-caddy.sh:10`의 `flock -n 9`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/scripts/ingress/install-caddy.sh#L10): 기존 deploy.sh와 같은 잠금 파일을 쓰는 이유를 확인한다. 잠금 중 앱·DB를 재시작하지 않고, 기존 Caddy 설정·다른 웹서버를 발견하면 자동 덮어쓰기 전에 멈추는 조건을 읽는다. [공식 자료](https://manpages.ubuntu.com/manpages/noble/man1/flock.1.html)

### 운영·복구

- live 방화벽과 재부팅용 저장 파일의 차이 — [`scripts/ingress/allow-caddy-ports.py:81`의 `os.replace(temporary_path, saved_path)`](https://github.com/owencity/jungsan_attack/blob/04a4750bd60bf36696c42466a409ab2cfc6c8a8f/scripts/ingress/allow-caddy-ports.py#L81): Docker·Tailscale의 동적 체인을 통째로 저장·restore하지 않는 이유를 본다. saved rules는 두 줄만 변경하고 실패 시 이번 실행이 추가한 live 규칙만 제거하는 범위를 점검한다. [공식 자료](https://manpages.ubuntu.com/manpages/noble/man8/iptables-restore.8.html)

## diff를 보고 직접 체크할 것

- [ ] route를 handle_path로 바꿨을 때 Apple callback의 upstream 경로를 설명한다.
- [ ] 최종 REJECT 뒤의 ACCEPT가 실행되지 않는 이유를 설명한다.
- [ ] 왜 살아 있는 Docker/Tailscale 체인을 통째로 restore하지 않았는지 설명한다.
- [ ] error log가 실제 발생하지 않으면 redaction 테스트가 무엇을 증명할 수 없는지 설명한다.
- [ ] 새 연결 2.00초와 재사용 연결 0.12초 중 하나만 선택해 성능을 결론 내리면 왜 부정확한지 설명한다.

학습 상태는 모두 미완료다. 구현·CI·운영 전환이 성공해도 공부 완료로 표시하지 않는다.
