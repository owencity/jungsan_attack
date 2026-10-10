# 운영 DB 최초 인증 — PR 29 후속 고정 가이드

[PR #29](https://github.com/owencity/jungsan_attack/pull/29). 후속 diff `08b13a4a9c292499a4e781380af95f6c47aa7df9` → `ba508a6903a125b07383ea21927727fb31ced64f`.
[Apple 설정·Java/Kotlin 원본 가이드](../apple-provider-v1/guide.md)는 그대로 보존한다.
이번 [diff](changes.patch)는 운영 YAML·CI·문서 4개 파일이다. 언어 구현을 바꾼 것으로 표시하지 않는다.
목차·정책은 당시 origin/main `c11c9b9af1dfd76c7daf90b9a4e6f92f23c00ce4`의 실제 파일을 함께 보존했다.

## Java 기본기

- 이번 후속 diff는 해당 없음. 원본 언어 구현의 위치는 v1을 본다.

## Effective Java

- 이번 후속 diff는 해당 없음. 원본 언어 구현의 위치는 v1을 본다.

## 함수형 Java

- 이번 후속 diff는 해당 없음. 원본 언어 구현의 위치는 v1을 본다.

## Kotlin 기본

- 이번 후속 diff는 해당 없음. 원본 언어 구현의 위치는 v1을 본다.

## Kotlin 고급

- 이번 후속 diff는 해당 없음. 원본 언어 구현의 위치는 v1을 본다.

## 기술별 학습 포인트

- DB 최초 인증 — [server/src/main/resources/application-prod.yml:13](https://github.com/owencity/jungsan_attack/blob/ba508a6903a125b07383ea21927727fb31ced64f/server/src/main/resources/application-prod.yml#L13): 로컬 URL과 달리 운영 URL에 없던 공개키 교환 옵션을 변경 줄에서 확인한다. MySQL 인증 캐시를 비우기 전 성공이 최초 연결 성공을 보장하는지 실패 CI와 FLUSH PRIVILEGES 검증을 비교한다.

- 실패 근거 보존 — [.github/workflows/ci.yml:62](https://github.com/owencity/jungsan_attack/blob/ba508a6903a125b07383ea21927727fb31ced64f/.github/workflows/ci.yml#L62): CI 가짜 설정으로 기동한 prod 로그만 artifact에 추가되는지 diff를 읽는다. 운영 로그·실제 키를 가져오는 경로가 생기지 않았는지도 대조한다.

실패 근거: [CI 38034112570](https://github.com/owencity/jungsan_attack/actions/runs/38034112570), 실제 prod 최초 인증에서 `Public Key Retrieval is not allowed`. 수정 후 최신 CI와 운영 배포는 별도 확인한다.
