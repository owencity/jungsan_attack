# 인원 기준 자동 정산 — FC-020 구현 계약

총무가 인원을 입력하고 그 인원 안의 모든 사람이 모든 차수에 응답하면 서버가 같은 트랜잭션에서 확정한다.
인원 미입력은 자동 정산을 끈다. 수동 미리보기·확정은 현재 ACTIVE 명단 전원을 대상으로 유지한다.

- `POST /api/v1/gatherings`의 선택 본문 `{headcount?: 2..50}`. 본문을 생략한 기존 호출도 지원한다.
- `POST /api/v1/gatherings/{gid}/settlement-units`에 선택 `headcount`를 추가한다. 멱등 요청의 비교 대상에도 포함한다.
- `PUT /api/v1/gatherings/{gid}/settlement-units/{id}/headcount {headcount: 2..50}`는 해당 OPEN 단위 총무만 가능하며 204를 반환한다.
- 상세·목록·단위 생성 응답에 `headcount`, `autoSettlementError`를 포함한다. 후자는 정상일 때 null이다.
- 인원 순서는 총무가 첫째, 나머지는 단위 `joined_at` 오름차순, 동률은 participant id다. 재포함은 새 가입 시각으로 센다.
- 응답은 해당 단위의 실제 차수와 SELF/HOST 응답 행으로 판정한다. 빈칸·AUTO는 응답 완료가 아니다.
- 가입, 본인/대리 응답, 인원 수정, 차수 수정/삭제 뒤 판정한다. 차수 추가·정산 되돌리기 직후에는 판정하지 않는다.
- 자동 확정에서만 인원 밖 명단을 REMOVED로 바꾸고 해당 단위 응답을 삭제한다. 공유 participant·다른 단위는 유지한다.
- 단위 `FOR UPDATE` → 술자리 `FOR UPDATE` 순서로 잠근다. 검증, 제외, 송금 스냅샷, 알림은 한 트랜잭션이다.
- 수동·자동 확정은 같은 Core 및 스냅샷 저장 함수를 호출한다. 자동에는 클라이언트 미리보기 해시 비교가 없다.

## 알림과 계산 실패

`HEADCOUNT_EXCEEDED`는 인원을 넘는 구간에 처음 진입할 때 총무에게 한 번 보낸다. 다시 인원 이내가 되면 다음 초과를 알릴 수 있다.
`MEMBER_EXCLUDED`는 제외된 사람에게, `SETTLED_HOST`는 총무에게, `SETTLED`는 실제 송금자에게 수취인·금액 요약을 보낸다.
자동 확정 타임라인은 "모두 응답해서 자동으로 계산했어요"다. 푸시는 후속이며 내부 알림만 저장한다.

**설계 가정:** 인원 밖 사람 중 결제자가 있으면 기존 `REMOVE_PAYER` 금지와 충돌하므로 자동 확정을 보류한다.
`autoSettlementError=REMOVE_PAYER`로 총무에게 보이고 인원을 늘리거나 결제자를 수정하거나 수동 정산한다.
Core 검증 실패도 해당 오류 코드를 저장하고 OPEN을 유지한다. 응답 저장을 취소하지 않으며 명단·스냅샷을 변경하지 않는다.
이 가정은 돈 받을 사람을 조용히 제거하지 않기 위한 것이며 PR에서 CTO 검토 대상으로 남긴다.

새 물리 스키마는 `018-auto-settlement.yaml`, changeSet `028`이다. 기존 migration은 수정하지 않는다.
