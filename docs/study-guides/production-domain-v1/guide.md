# 정산어택 운영 호스트 분리 — 고정 스터디 가이드

기준 diff `ceb2ef335b01b913d7ffdbcd5531f6b153bf8a3c` → `b659f4a47dd0c3fca55c4b083bc29216dd75a18b`. 목차·정책은 작업 당시 최신 `origin/main`인 `ceb2ef335b01b913d7ffdbcd5531f6b153bf8a3c`의 실제 파일을 읽고 복사했다.
[고정 diff](changes.patch)에서 각 변경을 먼저 체크하고, [before](before/)와 [after](after/)의 동일 파일을 비교한다.
이번 변경은 Python·운영 설정이며 Java/Kotlin 구현을 새로 추가하지 않았다. 기존 언어 가이드와 리뷰 기록은 덮어쓰지 않는다.

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

### DNS

- 기존 서비스의 호스트와 신규 API 호스트 격리 — [scripts/cloudflare-dns.py:11 · HOSTNAME → sync](https://github.com/owencity/jungsan_attack/blob/b659f4a47dd0c3fca55c4b083bc29216dd75a18b/scripts/cloudflare-dns.py#L11): 변경된 HOSTNAME이 GET 필터와 POST/PATCH 이름을 함께 제한하는지, 기존 api/webhook 레코드가 돌아오면 쓰기 전에 중단하는지 diff와 두 신규 테스트에서 확인한다.

### OAuth · 프론트 운영 설정

- 콜백 등록·환경변수·웹 API base의 주소 일치 — [docs/DEPLOY.md:66 · 운영 .env 키 표 / (d) 카카오 / (e) Apple / (g) Vercel](https://github.com/owencity/jungsan_attack/blob/b659f4a47dd0c3fca55c4b083bc29216dd75a18b/docs/DEPLOY.md#L66): KAKAO/APPLE_REDIRECT_URI, Apple Return URL·Domain, Vercel API base가 같은 새 호스트로 바뀌었는지 해당 diff를 대조한다.

### Cloudflare Tunnel

- DNS 레코드와 Tunnel의 서비스 경로를 따로 적용 — [docs/DEPLOY.md:183 · Cloudflare DNS 실행과 Tunnel 연결](https://github.com/owencity/jungsan_attack/blob/b659f4a47dd0c3fca55c4b083bc29216dd75a18b/docs/DEPLOY.md#L183): DNS workflow와 Public Hostname 추가가 각각 무엇을 바꾸는지, 새 18080 경로를 추가하면서 기존 웹훅 8080 경로가 보존되는지 변경된 절에서 확인한다.

검증: 가짜 DNS API 9건 성공, actionlint workflow 3개 오류 0. 실제 DNS·Tunnel 저장·운영 로그인 성공은 미확인이다.
