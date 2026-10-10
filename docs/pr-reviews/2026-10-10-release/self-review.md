# 구현자 재검토

코드 기준 `a8a56266b037a9e87eef3cc0697c8c7f989bbbd4`. n8n 댓글을 결함 확정으로 취급하지 않고 해당 코드·계약·검증과 대조했다.
기존 백엔드 계산/인증 코드는 이전 검증 head `1977a7d02a6764998b1183afbd64cf41b6b4b0eb` 이후 변경하지 않았다.
신규 DNS 동작은 가짜 API 7건과 actionlint 3개 workflow에서 통과했다. 최종 문서 head의 CI를 다시 확인한 뒤 병합한다.

| 실제 리뷰·영역 | 재검토 근거·결과 | 상태 |
|---|---|---|
| #7 Core 잔액 조정·누락 응답 | CALC_RULES_V2 T7의 대체 조정자 규칙, CONFIRM 명시 응답 검사. 수동 경로는 AUTO 예비 생성, 자동 경로는 SELF/HOST 완비 후 Core 호출 | 구현·회귀 확인 |
| #7 NO_ATTENDEE 계약 | Java/Kotlin 오류 enum과 API.md 오류 계약. 면제 API는 사용자 보류이며 Core EXEMPT 계산은 기존 규칙 보존 | 보류 범위 구분 |
| #10 인증 가드·Group | feature 패키지 @RestController 스캔·합성 RequestMapping 검사. 최종 코드에는 GroupController가 없으며 AuthenticationGuardSpec/HTTP 경계 검사 통과 | 이전 전제 해소 |
| #10 실명 동시 등록·문자 길이 | codePointCount 2..10, conditional UPDATE, 같은 이름 재시도 성공/다른 이름 409 계약. affected-row를 무시한다는 것만으로 같은 값 재시도를 결함으로 판단하지 않음 | 단위 검증, 실제 실명 등록 경합은 후속 |
| #10 내가 참여한 목록 | GatheringService.list의 본인 좌석 JOIN과 단위 요약. #10 당시 총무 한정 구현은 최종 #18/#27 목록이 대체 | 후속 구현 포함 |
| #16 SAVE 총액 상한 | Java/Kotlin 모두 전체 합계 검사는 CONFIRM에 유지. 개별 금액과 주류는 SAVE 검사. 기존 Core 동작/명세 불일치가 남아 있어 후속 수정 대상으로 보존 | 후속, 이번 PR 새 정책 결정 아님 |
| #16 BigInteger 포화 | effectiveRounds는 Long 변환 전 포화, Validator가 원본 drink 합계를 BigInteger로 검증해 오류 반환. 조용한 금액 확정으로 이어지지 않음 | 계산 실패 경로 확인 |
| #18 계좌 키 | prod는 필수 환경변수, compose는 PAYOUT 키 누락/빈값 거절, cipher는 32바이트 검사. all-zero 로컬 키를 운영에 넣지 않도록 DEPLOY 키 생성 안내 | 설정 경계 확인, 실제 키는 CTO 준비 |
| #18 잠금 순서 | 기존 단위 수정은 unit→Gathering, createUnit은 Gathering을 먼저 잡지만 기존 단위를 FOR UPDATE하지 않고 새 단위만 조립. READ_COMMITTED 삭제 배치는 ID/활동 재검사. 실제 다중 단위·탈퇴 생성 경합은 기존 시험, 전체 부하 시험을 주장하지 않음 | 확인된 경합과 후속 구분 |
| #18 payout/sent | 방 잠금 뒤 사용자 계좌 변경. 계좌 없는 시점 sent 요청의 PAYOUT_MISSING은 재시도 가능한 정상 거절이며 미확인 송금을 발송 완료로 만들지 않음 | 정책 경계 확인 |
| #18 v1 migration | 기존 CONFIRMED는 HALT. 운영에 기존 v1 DB가 있으면 별도 확인, 이번 OCI에는 정산 컨테이너/배포 경로가 없어 새 전용 DB 예정 | 운영 사전 조건 |
| #18 basis 마지막 차수 | 마지막 줄에 절단 차이를 반영하는 현재 표시 로직은 ABSENT 마지막 차수에도 잔액 표시 가능. 최종 송금액은 바꾸지 않지만 표시 근거 개선은 후속 | 후속 |
| #18 배치 JSON 조건 | notification_outbox JSON 조건은 인덱스 조회로 바뀌지 않음. 100건 한도·단위 트랜잭션은 기존 설계, 대량 자료에서 성능 보장하지 않음 | 운영 부하 후속 |
| #20 이름 부분 일치 삭제 | 탈퇴자 이름을 포함한 시스템 소식 제거는 동명이인/부분 일치의 다른 소식도 지울 수 있음. 금액·송금·좌석은 보존하지만 소식 FK 개선은 남음 | 알려진 후속 |
| #20 Apple revoke I/O | 계정 탈퇴와 외부 HTTP는 다른 트랜잭션. 재시도 worker 자체는 row 잠금 중 최대 5초 외부 I/O를 수행하므로 worker 처리량 개선은 남음 | 알려진 후속 |
| #20 Apple 취소 | code 없는 취소는 GlobalExceptionHandler.handleRequestParameter로 400 MALFORMED_REQUEST. 현재 HTTP 시험에서 400 확인; 원래 웹/앱으로 취소 복귀 UX는 후속 | 500 방지 확인, UX 후속 |
| #20 JWKS·인증 DB 읽기 | 매 Apple 로그인 JWKS 조회와 매 인증 2회 DB 읽기는 남음. 로그아웃 즉시 무효화·탈퇴 차단을 유지하며 TTL 캐시를 임의 도입하지 않음 | 성능 후속 |
| #20 티켓·탈퇴 경합 | 실제 HTTP 17/DB 15: 티켓 4건 중 1건 교환, 만료·rollback, 탈퇴/단위 생성 4회 경합·JWT 차단 | 실제 DB 검증 |
| #21 Action v5 유효성 | GitHub run 37966497377에서 checkout/setup-java/setup-gradle v5가 실제 성공 | 당시 추정 해소 |
| #21 배포 중 취소·DB 시험 | deploy cancel-in-progress=false, 배포 flock, SHA 이미지·복원 검사. 실제 MySQL/HTTP 검증은 CI와 배포 빌드에 포함 | 수정·검증 |
| #27 자동 정산 | 단위 row 잠금·UNIQUE 스냅샷·Core 검증 후 제외. 동일 응답은 revision/알림 불변. 자동/수동 같은 persistSettlement. Java/Kotlin 정책 및 알림 독립 구현 | 실제 MySQL 20·Core54/Server116 |
| #27 인원 밖 결제자 | REMOVE_PAYER로 보류·응답 보존·총무 배너는 CTO 2026-10-10 승인 | 신규 미결 결정 없음 |
| 신규 DNS 범위·멱등성 | scripts/cloudflare-dns.py sync: 이름/UUID/Zone/레코드 수·종류 제한, POST/PATCH 뒤 재조회. scripts/verify-cloudflare-dns.py 7건 | 가짜 API 검증, 실운영 후속 |

최초 공개 출시 전 실제 Kakao/Apple 로그인·앱 티켓 교환·logout·탈퇴를 확인해야 한다.
CTO가 운영 설정을 입력하고 DNS 토큰을 준비한다. DNS 변경은 remote-managed Tunnel 원본 포트를 바꾸지 않으며 18080 연결 확인이 별도로 필요하다.
기존 JWT 14일/30일 미결은 이 PR에서 바꾸지 않는다. 가이드 생성이나 자체 점검을 사용자 학습/리뷰 승인으로 체크하지 않는다.
