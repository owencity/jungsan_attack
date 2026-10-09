# 2026-10-06 — 같은 술자리에서 추가 차수의 새 총무

## FC-015 같은 술자리·참여자 재사용·총무별 독립 정산

- **상태:** 열림 (CTO 방향 확정 · 계약 반영, 서버·웹·앱 구현 후속)
- **기존 제안 정정:** `docs/fc-015-next-round`의 별개 Gathering 생성·새 링크 재가입 제안은
  CTO의 최신 요청과 다르다. 같은 Gathering 안에서 새 총무의 SettlementUnit을 만든다.
- **바뀐 흐름:**
  - A의 1·2·3차 뒤 B가 4차를 맡으면 **같은 링크·같은 참여자 신원**으로 B의 담당 단위를 만든다.
  - [다음 차는 내가 계산했어요]는 기존 `createGathering` 호출 대신 **단위 생성 + 담당 차수 입력**으로 바꾼다.
  - 기존 명단에서 이번 계산 대상자를 선택한다. 기존 응답을 복사하거나 전원 자동 포함하지 않는다.
  - 새로 온 사람은 같은 링크로 참여하고 선택한 OPEN 단위에만 포함한다.
  - A의 정산·송금 중에도 B의 새 차수 응답·계산이 가능하다. 참여자는 자신의 차수 응답을 보면 된다.
  - 카드 문제의 대신 결제를 위해 `payerParticipantId`는 유지하고 기본으로 총무를 선택한다.
- **백엔드 영향:** 총무별 관리 권한·명단·revision/hash·정산·송금 상태를 분리한다.
  공유 participant ID를 재사용한다. 수식은 기존 Core v2, 호출 입력 범위는 단위별이다.
- **프론트 영향:** 상세의 `settlementUnits`, round/transfer의 `settlementUnitId`, 단위별 host와 상태를 사용한다.
  술자리 요약 OPEN만으로 이미 정산된 다른 단위의 수정 버튼을 열지 않는다. 여러 화면의 전역 host/status 판정도 변경한다.
- **반영한 곳:** REQUIREMENTS v4, CALC_RULES_V2 호출 범위, DOMAIN_DB_DESIGN_V2 연결,
  API v5, ADR-020, [SETTLEMENT_UNITS.md](../SETTLEMENT_UNITS.md).
- **구현 전 확인:** 전체 완료 후 추가 단위 생성, 복수 총무 스푼은 SETTLEMENT_UNITS §7.
  계약 반영만으로 FC-015 전체를 `반영됨`으로 바꾸지 않는다.
