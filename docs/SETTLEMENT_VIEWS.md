# 개인 정산 조회와 캡처용 표 데이터

> **FC-029 (2026-11-01):** 전원 응답 자동 확정은 폐기했다. OPEN 예상 계산 후 총무가 명단을 검토하고 기존 확정 API를 호출한다. 새 비회원 화면은 응답 제출·수정만 제공하며 아래 개인 금액·송금 API는 기존 클라이언트 호환용이다. [변경 계약](flow-changes/2026-11-01-host-review.md)을 우선한다.


FC-028의 백엔드 2차 계약이다. 기존 회원 API와 비회원 참여 API v18 위에 추가한다.
정산표 화면·캡처 이미지를 서버에 저장하지 않는다. 음료 별도 분배는 미결로 유지한다.

## 비회원 개인 금액

GET `/api/v1/guest/gatherings/{gatheringId}/settlement-units/{settlementUnitId}/settlement`는
비회원 쿠키로 본인 ACTIVE 단위만 조회한다. OPEN은 Core의 예상 부담액·예상 송금·근거를 주며
계좌는 주지 않는다. 계산 불가면200 `available=false,errorCode,total=null,transfers=[]`로 안내한다.
확정 후에는 본인이 송금자인 기존 스냅샷만 조회하고 수취인의 이름·계좌·차수별 근거를 준다.
타인의 송금·응답·계좌는 포함하지 않는다. 응답은 no-store이며 열람 자체로 상태를 바꾸지 않는다.

응답 필드는 gatheringId, settlementUnitId, status, isEstimate, available, errorCode,
participantId, total, settledAt, transfers다. total은 본인의 전체 부담액으로, 입금 확인된 송금도 포함한다.
transfers의 필드는 id(예상은 null), toParticipantId, toDisplayName, amount, status(예상은 PREVIEW),
payout(예상 또는 계좌 미등록/삭제는 null), basis[{roundId,type,amount}]다.

POST `/api/v1/guest/transfers/{transferId}/sent`는 비회원 쿠키·정확한 Origin을 요구하고204다.
서버가 송금의 술자리와 본인 신원을 확인한다. 기존 WAITING/SENT/CONFIRMED·계좌 미등록·
SETTLING 규칙과 알림을 공유하며, 수취인 확인 권한을 주지 않는다. 같은 요청은 멱등이다.

## 총무 정산표

GET `/api/v1/gatherings/{gatheringId}/settlement-units/{settlementUnitId}/settlement/table`은
회원 인증과 해당 단위 총무 권한을 요구한다. 다른 총무나 비회원에게 전체 표를 공개하지 않는다.
REPEATABLE_READ 조회 안에서 입력과 상태를 일관되게 읽고 Core의 표시 변환을 사용한다.

gatheringId, settlementUnitId, title, status, isEstimate, generatedAt, inputRevision, inputHash와
table{lines,rounds,adjustments,transfers,grandTotal}을 반환한다. lines는 참여자 id·이름·최종 부담액·결제액이다.
transfers는 fromParticipantId,toParticipantId,amount이며 최종 송금 안내용이다. 계좌나 입금 상태는 포함하지 않는다.
rounds는 차수 id·순서·결제자 id·결제액·비주류/주류 총액·displayTotal·참여자별 셀이다.
셀은 participantId,type,food,alcohol,amount,source이며, 미응답 source는 UNANSWERED다.
현재 비주류는 안주·음료 합계이므로 음료를 별도 분배한 것처럼 표시하지 않는다.

셀의 food/alcohol은 정확한 원부담을 정수 원으로 절단한 표시값이다. 차수 결제액과 셀 합계
displayTotal은 다를 수 있다. 수취인별 adjustments는 기존 Core의 최종 배정액과 표시 셀의 차이를
양수/음수로 명시한다. 조정은 새 요금이 아니며 표 하단에 별도 행으로 보여준다.
`모든 셀 + 모든 조정 = 최종 개인 합계 = grandTotal`이 성립한다.
UI에서 다시 나누거나 셀마다 올림하지 않는다. 원부담과 최종 부담액을 혼동하지 않는다.

확정 표는 변경 불가능한 입력으로 재구성하되 기존 스냅샷의 송금·grandTotal과 일치하는지 검사한다.
다르면409 SETTLEMENT_VIEW_UNAVAILABLE이며 스냅샷을 바꾸지 않는다. 개인 확정 송금 조회는
표 재계산과 무관하게 스냅샷을 기준으로 한다. OPEN의 미응답은 기존 DRANK 가정으로 계산하되
UNANSWERED 표식을 유지한다. 계산 불가 표는 기존 VALIDATION_FAILED/NO_ROUNDS 오류를 따른다.

## 웹 연결과 남은 범위

비회원401은 회원 로그인으로 강제 이동시키지 않고 브라우저 자격 문제로 안내한다. 단위 제외는
기존 본인 조회의 included=false로 표시한다. 공유 링크 미리보기에는 전체 명단·계좌를 추가하지 않는다.
표 캡처에는 이름·차수·결제자·계산 상태·기준 시각·합계를 표시하고 화면 밖 사람이 잘리지 않게 한다.
비회원 계좌 등록·결제자·추가 총무·메시지·신고/차단·스푼·기기 이동 복구는 후속이다.
