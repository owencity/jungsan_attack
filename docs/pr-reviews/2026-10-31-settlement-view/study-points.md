# 정산표와 비회원 송금 학습 후보

상태: **UNRECORDED**. 학습 완료를 뜻하지 않는다.
diff: `cfd1917dddeb5b883800ac1e48b46ceb88e4f6b0` → `f882a13cd6a1d92384b136b95671f5e7aca83e8b`.
목차 근거: 저장소 STUDY_POLICY.md·STUDY_CATALOG.md. 아래 항목은 책 본문을 읽었다는 기록이 아니다.

- Effective Java · EJ-50 · 적시에 방어적 복사본을 만들라 — `core/src/main/java/app/jeongsan/core/javaimpl/SettlementTable.java`의 record 생성자: List.copyOf와 Map 복사가 외부 변경으로부터 표시 결과를 보호하는 범위를 살펴본다.
- 기술별 학습 포인트 · UNMATCHED · MySQL 일관된 읽기 — `GatheringService.settlementTable`의 REPEATABLE_READ: 여러 SELECT가 한 정산 입력을 읽는 조건과 갱신 잠금과의 차이를 검토한다.
- 기술별 학습 포인트 · UNMATCHED · 멱등성과 동시성 — `GatheringService.guestSent/transferAs`, `verify-guest-participation.py`: 인증된 송금자 확인과 단위 잠금·상태 전이가 중복 알림을 막는 경계를 검토한다.
- 기술별 학습 포인트 · UNMATCHED · 금액 표시와 보존 법칙 — `SettlementTable.present`, `SettlementTableSpec`: 표시 셀 절단·원 단위 조정·확정 스냅샷이 각각 어떤 값의 진실인지 살펴본다.
