# ADR-015 · 확정된 송금 명세를 불변 스냅샷으로 저장한다

**상태** 확정 · **대체** `ADR-005`의 `paidAmount` 방식 · **관련** `REQUIREMENTS.md`,
`CALC_RULES_V2.md`, `DOMAIN_DB_DESIGN_V2.md`, `ADR-004`

## 맥락

Core v1은 술자리 전체의 대표결제자를 중심으로 계산했고 참여자마다 하나의
`paymentStatus`와 `paidAmount`를 저장했다. 계산 결과 자체는 저장하지 않고 현재 입력으로
항상 다시 계산했다(`ADR-005`). 확정 후 입력 수정과 재정산도 허용했기 때문이다.

Core v2에서는 전제가 바뀌었다.

- 차수마다 결제자이자 수취인인 총무가 한 명 있다.
- 한 참여자가 여러 총무에게 각각 송금할 수 있다.
- 서로 다른 수취인의 채권은 상계하지 않는다.
- 금액 확정 후 기존 계산 입력과 송금액은 바뀌지 않는다.
- 입금 확인과 이의제기는 특정 `송금자 → 수취인 → 금액`을 대상으로 한다.

참여자 한 행에 입금 상태를 두면 어느 수취인에게 보낸 돈인지 표현할 수 없다. 반대로
계산 결과를 전혀 저장하지 않으면, 입금 확인과 분쟁이 당시 확정된 어떤 금액을 대상으로
하는지 영속적인 식별자가 없다.

## 결정

미리보기 계산값은 저장하지 않는다. **금액 확정 시 Core v2가 만든 송금 명세만 불변
스냅샷으로 저장한다.**

```text
Settlement
  gatheringId, inputRevision, inputHash, grandTotal, confirmedAt

SettlementTransfer
  settlementId, senderParticipantId, recipientParticipantId, amount, status
```

확정 트랜잭션은 현재 입력을 Core로 다시 계산한 뒤 `Settlement`와 모든
`SettlementTransfer`를 함께 삽입한다. 같은 트랜잭션에서 술자리 상태를 금액 확정으로
바꾸고 알림 outbox도 기록한다.

금액 확정 후 다음 값은 수정하지 않는다.

- `Settlement.inputRevision`, `inputHash`, `grandTotal`
- `SettlementTransfer.senderParticipantId`, `recipientParticipantId`, `amount`

변하는 것은 송금의 워크플로 상태와 상태 변경 이력뿐이다. 금액 오류가 뒤늦게 발견돼도
기존 스냅샷을 고치지 않는다. MVP에서는 오프라인 조율하고, 후속 버전에서 별도의 조정
정산을 추가한다.

`participants.payment_status`, `paid_amount`, `sent_at`, `received_at`은 더 이상 권위 있는
데이터가 아니다. v2 구현은 transfer 단위 상태를 사용하며 기존 컬럼은 migration에서
제거 대상으로 다룬다.

## 근거

### 여러 수취인을 정확히 표현한다

```text
A → B 10,000원
A → C 7,000원
```

A에게 하나의 `paymentStatus`만 두면 B에게만 보냈는지, 둘 다 보냈는지 알 수 없다.
송금 건이 상태의 주체가 되어야 각각 요청·확인·이의제기를 진행할 수 있다.

### 확정 당시의 계약을 보존한다

`inputHash`는 확정 순간의 경합을 막지만 그 자체로 송금 관계를 설명하지 않는다.
확정된 송금 명세가 있으면 알림, 입금 확인, 분쟁과 감사 로그가 모두 같은 ID를 참조한다.

### Core의 순수성은 유지된다

DB가 계산하지 않는다. Core가 결과를 만들고 서버가 확정된 결과 일부를 저장한다.
미리보기와 확정 직전 검증은 여전히 현재 입력으로 Core를 실행한다. 따라서 계산 규칙의
유일한 구현은 계속 Core다.

### 과도한 상세 스냅샷을 피한다

차수별 유리수 breakdown 전체를 복제하지 않는다. 금액 근거 화면은 변경 불가능한 입력을
Core로 다시 계산해 만든다. 영속화하는 것은 입금 워크플로에 꼭 필요한 최종 transfer뿐이다.

## 대가

- 계산 입력과 송금 스냅샷 사이의 정합성을 확정 트랜잭션과 테스트로 보장해야 한다.
- Core 계산 규칙을 훗날 변경하면 과거 근거 화면은 당시 계산 버전이 필요하다. 첫 버전은
  `calculation_version = 2`를 저장해 향후 버전 라우팅 지점을 남긴다.
- 확정 후 수정이 쉬운 구조보다 사용자 실수를 사전에 막는 UX가 더 중요해진다.
- `ADR-005`와 기존 participant 입금 컬럼을 migration 및 API에서 정리해야 한다.

## 재검토 조건

- 금액 확정 후 조정 정산을 제품 범위에 넣을 때
- 계산 규칙 v3가 생겨 과거 Core v2 근거를 재현해야 할 때
- 부분 입금이나 한 송금의 여러 차례 입금을 지원할 때

이 경우 기존 transfer를 수정하지 않고 `adjustment settlement` 또는 별도 payment ledger를
추가하는 방향을 먼저 검토한다.

