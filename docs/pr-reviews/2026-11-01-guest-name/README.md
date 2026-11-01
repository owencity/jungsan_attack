# FC-032 이름 정정 · 고정 코드 검토

- 기준: f26f4f6ed2c2f83a5023169e844fbd41c66d87ec → 8137cb9bd993dcfc6ed141ae457dc8123ff93533 (PR54).
- 정책/카탈로그: origin/main 8d27496270e6c8adcb78b4580d68c150f9c3cce6.
- 자체 점검이며 외부 리뷰 완료가 아니다. 학습 상태 UNRECORDED.
- 로컬 서버 230개/bootJar 성공, CI 41289287779 전체 성공.
- 실제 MySQL/HTTP 비회원 66개(기존+이름 정정), 스푼 23개 회귀 통과.
- 웹 PR2 기준: bef58cefb65ac6dca8cc828ffadb6a1b8b5733f7 → c0f055ead2915b08d397fbddf7cea80b6512708c. web-fixed.patch는 PNG를 제외한 고정 diff다. Vitest 289개·빌드 통과.
- 앱 PR5 기준: c2f05e4eebac7acb0cc12e10fc5c7b07a145749f → 9c1aba34300689bda76e1b17f06c3e7874dfdf3e. app-fixed.patch에 고정했다. Android 232개·컴파일 통과. 배포/CI 최신 상태는 ../../operations/fc032-release-2026-11-01.md 참조.
- iOS 테스트 후속: 9c1aba3 → d8e730f2bcf34192fdd306899b3e17b4344277a1, ios-test-followup.patch. 첫 키보드 안내를 닫는 UI 자동화 실패에 대응한다. 이름 입력·저장 검증을 건너뛰지 않는다. 후속 CI 결과가 나오기 전에는 해결됐다고 단정하지 않는다.
- iOS 이미지 검사 후속: d8e730f → b1d917dea682a69d8cc80c1be6a0221ad1088e16, ios-image-test-followup.patch. 실패 접근성 기록의 실제 PNG/저장 메뉴를 근거로 루트 식별자 대신 파일명과 누를 수 있는 메뉴를 검사한다. iPad의 기존 검사 성공과 새 검사 전체 성공은 구분한다.

## 검토 결과

1. 수정 대상은 브라우저 해시 또는 총무의 활성 단위 명단으로 결정한다. 이름·본문 participantId로 비회원 권한을 선택하지 않는다. 회원 계정·탈퇴 좌석은 guest 표식과 user_id 검사로 거절한다.
2. 이름과 응답은 동일 서비스 트랜잭션이다. 잘못된 roundId의 롤백, 같은 값 재시도, 명단/스푼 신원 유지가 실제 DB 검사에 포함된다.
3. 공유 이름이 확정 자료에 반영되는 것을 막기 위해 소속 이력 전체 상태를 검사한다. 모든 단위→술자리 잠금 순서를 지켜 기존 확정·탈퇴와 순서를 맞췄다. 잠금 중 새 단위가 생기면 추가 잠금 대신 입력 변경으로 거절한다.
4. expectedDisplayName 비교는 오래된 화면의 덮어쓰기를 막는다. 이름이 원래 값으로 되돌아오는 ABA까지 검출하는 버전 번호는 아니다. 오타 정정의 최소 구현이며 금전 처리는 기존 revision/hash로 별도 보호한다.
5. 이름 정정의 쿼리는 명단 크기별 반복 조회가 아닌 일괄 조회/갱신이다. 스키마·계산 엔진·프로덕션 설정·의존성 변경 없음.
6. 웹/앱 모두 편집 화면을 연 시점의 이름을 expectedDisplayName으로 전송한다. 저장 도중 중복 동작을 막고 실패 시 입력을 남긴다. 성공 후 서버 상태를 재조회하며 화면에서 금액을 별도로 계산하지 않는다.

## 학습 후보

| 분류 | 근거 | 코드 위치·볼 관점 |
|---|---|---|
| Effective Java | EJ-49 · 8장 메서드 · 아이템49 매개변수가 유효한지 검사하라 | guest/javaimpl/GuestPolicy.java:11 correctedName — 표시 이름 검증, 기존 값 비교, 멱등 성공 순서 |
| Effective Java | EJ-76 · 10장 예외 · 아이템76 가능한 한 실패 원자적으로 만들라 | gathering/GatheringService.kt:226 guestRespond — 이름 변경 뒤 응답 검증 실패가 전체 롤백되는 경계. Spring/DB 검증은 아래 기술 항목과 구분 |
| 기술별 · UNMATCHED | MySQL 행 잠금·트랜잭션 경합 | gathering/GatheringService.kt:239 lockNameScope — 기존 단위→술자리 순서, 추가 단위 재조회, 확정 경합. scripts/verify-guest-participation.py의 실제 동시 실행 확인 |
| 기술별 · UNMATCHED | 쿠키 기반 비회원 권한·수정 대상 인가 | guest/GuestController.kt의 respond, gathering/GatheringService.kt:249 renameGuest — 본인 권한과 총무 권한의 서로 다른 입력 경계 |

기기 변경/쿠키 삭제 복구, 실명 인증, 개인 수정 링크는 범위 밖이다. 실제 iPad·운영 전체 흐름 확인과 CI 결과를 혼동하지 않는다.
