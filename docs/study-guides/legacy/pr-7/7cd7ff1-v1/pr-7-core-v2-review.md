# PR #7 — 검토 메모와 테스트 후보

기준 head: `7cd7ff1c8e8442733d11590df17eea119fc63a27`. [학습 가이드](pr-7-core-v2.md)와 [원본 JSON](pr-7-core-v2.json)을 별도로 둔다.

검토 지점은 결함 확정이나 병합 판단이 아니다. 코드를 수정하거나 테스트를 실행하지 않았다. 기존 개발 검증과 이번 정적 검토를 구분한다.

## 검토 지점

### medium · requirement · Validation.kt

[관련 파일](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Validation.kt)

전체 합계 상한을 검사하는 시점과 계산 문서가 일치하는지 확인이 필요하다. 저장에도 상한을 적용할지, 확정 전용이라고 문서를 명시할지 결정하는 방법을 권한다.

- 근거: CALC_RULES_V2.md §5는 저장·확정 공통에 '항목 및 전체 금액은 1조 원 이하'를 둔다. 구현의 전체 합계 fold와 상한 검사는 validateOnConfirm에만 있다.
- 가능 영향: 각 차수는 상한 이하여도 합계가 상한을 넘는 입력이 SAVE에서는 통과하고 CONFIRM에서 실패할 수 있다. 계산 결과가 잘못된다는 지적은 아니다.
- 확인할 점: 각 6천억 원인 두 차수의 SAVE 통과를 허용하는 계약인가? 허용 여부에 맞춰 검증 시점 또는 문서와 phase 테스트를 일치시킬 수 있는가?

### low · kotlin · Model.kt

[관련 파일](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Model.kt)

effectiveRounds의 '입력 모델은 불변' 주석은 읽기 전용 컬렉션과 깊은 불변성을 구분해 표현하는 편이 정확하다. 호출자 소유권 계약을 명시하거나 필요한 경계에서 방어적 복사를 검토할 수 있다.

- 근거: SettlementInput은 val rounds: List<Round>를 받고 drinkItems가 비면 effectiveRounds에서 rounds를 그대로 반환한다. 호출자가 MutableList를 전달할 수 있다.
- 가능 영향: 외부의 동일 컬렉션 참조가 변경되면 val과 data class만으로 계산 중 입력 불변성을 보장하지 못한다. 현재 호출자가 변경한다는 증거는 없다.
- 확인할 점: 계산 동안 입력 컬렉션을 변경하지 않는 것을 호출자 계약으로 둘 것인가, 스냅샷 경계에서 복사할 것인가?

### low · operation · Validation.kt

[관련 파일](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Validation.kt)

서버 연결 전 ErrorCode와 API 오류 표를 동기화하는 후속 작업이 필요하다. 엔진만 보존한 PR 범위는 존중하되 API 연결 PR에서 enum 대조를 권한다.

- 근거: MISSING_ATTENDANCE·NEGATIVE_ADJUSTED_AMOUNT 등이 추가됐고 기존 코드가 제거됐다. PR #7은 docs/API.md를 변경하지 않으며 DEVLOG는 서버·API 후속 작업을 명시한다.
- 가능 영향: 구형 계약으로 프론트 예외 분기를 구현하면 새 문자열을 처리하지 못할 수 있다. 현재 HTTP 정산 연결이 있다는 뜻은 아니다.
- 확인할 점: 정산 API PR에서 새 enum 목록과 오류 위치 필드를 같은 계약 버전으로 맞추고 검증할 것인가?

## 추가 테스트 후보

- **결제자마다 합계는 상한 이하지만 전체가 1조 원을 넘는 입력을 SAVE와 CONFIRM에 각각 전달** — 기대: CONFIRM은 AMOUNT_TOO_LARGE. SAVE의 기대값은 문서와 계약 결정에 맞춰 고정. 이유: 검증 시점과 문서의 공통 상한을 대조하는 회귀 근거가 필요하다.
- **참여자·차수·응답 맵의 삽입 순서를 바꾼 같은 의미의 입력** — 기대: 금액·조정자·송금 목록 순서는 동일하고 근거 차수는 seq 순서. 이유: 같은 객체를 두 번 호출하는 기존 결정론 검사와 입력 순서 독립성 검사는 다르다.
- **수취인이 담당 차수에서 모두 ABSENT이고 다른 두 부담자가 서로 다른 원부담을 갖는 입력** — 기대: 최대 원부담자가 잔액 조정자이며 자기 송금 없이 수취인 원금 회수. 이유: 현재 면제 결제자·동률 테스트 외에 ABSENT와 비동률 fallback을 직접 검증한다.
- **여러 술 항목은 각각 상한 이하지만 같은 차수의 술값 합계가 상한을 넘는 입력** — 기대: AMOUNT_TOO_LARGE를 포함한 Failure를 반환하고 산술 예외로 종료하지 않음. 이유: 단일 항목 곱셈 경계와 여러 항목 누적 경계를 구분한다.

