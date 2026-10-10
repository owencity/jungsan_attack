# 운영 DNS 추가 스터디 — PR #27

기준 diff `0a341c1d2b95ebe5d536cac0ebc2952b9088149b` → `a8a56266b037a9e87eef3cc0697c8c7f989bbbd4`. 원격 main 목차·정책 기준 `f094e884580159ffb82844bd3d759cf028c96458`.
Java/Kotlin 계산·인증 학습 23개는 [기존 고정 가이드](../release-ready-v1/GUIDE.md)에 그대로 남긴다.
이번 추가는 Python/Actions 운영 설정이다. 없는 Java/Kotlin 변경에 책 목차를 억지로 연결하지 않는다.

## Java 기본기
이번 추가 diff에 해당 없음.
## Effective Java
이번 추가 diff에 해당 없음.
## 함수형 Java
이번 추가 diff에 해당 없음.
## Kotlin 기본
이번 추가 diff에 해당 없음.
## Kotlin 고급
이번 추가 diff에 해당 없음.
## 기술별 학습 포인트

- [sync · scripts/cloudflare-dns.py:42](https://github.com/owencity/jungsan_attack/blob/a8a56266b037a9e87eef3cc0697c8c7f989bbbd4/scripts/cloudflare-dns.py#L42): DNS 호스트·Zone·Tunnel ID 범위를 제한해 다른 서비스가 바뀌지 않게 하는 관점. 실제 changes.patch에서 그 분기의 실패/재시도도 함께 읽는다.
- [sync · scripts/cloudflare-dns.py:66](https://github.com/owencity/jungsan_attack/blob/a8a56266b037a9e87eef3cc0697c8c7f989bbbd4/scripts/cloudflare-dns.py#L66): 없으면 생성·같으면 무변경·다르면 PATCH하는 재실행 관점. 실제 changes.patch에서 그 분기의 실패/재시도도 함께 읽는다.
- [sync · scripts/cloudflare-dns.py:78](https://github.com/owencity/jungsan_attack/blob/a8a56266b037a9e87eef3cc0697c8c7f989bbbd4/scripts/cloudflare-dns.py#L78): API 쓰기 성공과 실제 적용을 재조회로 구분하는 관점. 실제 changes.patch에서 그 분기의 실패/재시도도 함께 읽는다.

검증: 가짜 Cloudflare API 7건 통과. 실제 토큰·DNS·Tunnel 연결 성공은 별도이며 학습 완료로 표시하지 않는다.
