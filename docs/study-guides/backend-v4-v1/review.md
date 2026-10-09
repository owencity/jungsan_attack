# PR 리뷰 체크 — 구현자 사전 검토

이 문서는 자동 AI 리뷰 결과를 가장하지 않는다. n8n 리뷰는 실행 시각·commit SHA와 함께 별도 버전으로 보존한다.
주말 리뷰 후 발견 사항의 class/function/diff 줄, 근거, 판단, 수정 PR 링크를 이 체크에 추가한다.

- [ ] GatheringService.settle / markComplete / transfer: FK 자식 삽입 전 unit→Gathering 배타 잠금. 실제 2단위 동시 정산은 통과했으나 다른 경합도 확인한다.
- [ ] GatheringService.respond: 여러 답 중 다른 단위 차수가 있으면 먼저 쓴 행과 revision까지 rollback. HOST 대리 응답은 SELF로 수정 가능, HOST+EXEMPT는 보존한다.
- [ ] GatheringService.details: 목록 테이블별 IN 배치, 본인/실제 송금 대상만 지급 계좌 공개. 공개 join 미리보기에 개인정보 없음.
- [ ] SettlementWorkflow.hash Java/Kotlin: 영역/개수/길이 접두사·ID 정렬, 계산과 무관한 이름/다른 단위는 제외.
- [ ] GatheringService.reopen: WAITING으로 돌아가도 sentAt/confirmedAt 이력은 취소를 막는다. B 이력 때문에 A 취소를 막지 않는지 추가 경합 확인.
- [ ] GatheringService.deleteExpired: 새 unit ID·last_activity·삭제 시각을 재검사하며 자식 FK 순서대로 삭제. 삭제와 가입/수정 경합은 추가 확인.
- [ ] SettlementPresentation.basis: 차수별 절단 합과 실제 송금 차이를 마지막 줄에 붙인다. 마지막이 불참/0원인 특이 입력에서 표시 의미도 제품 관점으로 점검한다.
- [ ] migration 024: v1 CONFIRMED 자료를 자동 전환하지 않고 사전 중단. 레거시 기타 항목/전역 면제 등은 운영 현황과 별도 전환 판단이 필요.
- [ ] 앱 인증/Apple/탈퇴·복수 총무 스푼·실시간 전달은 미완료 목록에 남아 있으며 전체 출시 완료로 표시하지 않음.

검증: Core 54·server 79, 건너뜀 0. 실제 HTTP 25개 확인, fresh migration와 별도 CLI upgrade 4개 확인.
코드 상태와 학습 진도를 분리한다. 리뷰/학습이 남았다고 완료된 구현을 되돌리거나 병합을 자동 실행하지 않는다.
