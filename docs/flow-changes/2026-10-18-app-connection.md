# 2026-10-18 — 웹·앱 소스와 실제 서버 연결 계약 대조

## FC-023 다음 차의 '나 혼자 시작하기'가 headcount 1을 보낸다

- **상태:** 프론트만 (서버 계약 유지·웹/앱 수정 제안, 실제 빌드 재현·수정 확인 후속)
- **바뀐 흐름:** API 모드에서 다음 차 명단의 다른 사람을 모두 해제한 뒤 [나 혼자 시작하기]를 누르면 목표 인원이 1로 전송된다. 목데이터 모드는 최소 2로 보정한다.
- **관찰 근거:** 앱 로컬 소스를 읽기 전용으로 대조했다. `RoomScreen.kt`의 `NextRoundOffer`는 빈 선택을 허용하고, `V3Gateway.createUnit`은 본인을 합친 `ids.size`를 그대로 보낸다. `V3Store.createUnit`은 `maxOf(HEADCOUNT_MIN, picked.size)`다. 실제 앱 빌드/화면을 실행한 결과로 보지 않는다.
- **웹 추가 대조:** `owencity/profile` 원격 main의 `RoomPage.tsx`도 빈 선택의 [나 혼자 시작하기]를 허용한다. `gateway.ts:191`은 `ids.length`, 목 스토어 `store.ts:215`는 `Math.max(HEADCOUNT_MIN, picked.size)`다. 웹에도 같은 요청 차이가 있다. 조회한 파일의 blob SHA·발췌는 `docs/pr-reviews/2026-10-18-release/web-source-evidence.json`에 보존했다. 현재 배포된 Vercel 빌드와 동일하다고 단정하지 않는다.
- **기존 계약:** FC-020·AUTO_SETTLEMENT의 자동 정산 목표 인원은 2~50명이다. 현재 참여 명단이 본인 1명인 것은 허용한다. 나머지 한 명이 들어와 응답할 때까지 기다리는 것과 목표 인원을 1로 보내는 것은 다르다.
- **백엔드 영향:** API·DB·계산 규칙 변경 없음. `UnitCreateRequest.headcount`의 `@Min(2)`를 완화하지 않는다.
- **서버 검증:** `GatheringApiSpec`에 1명 목표 400/서비스 미호출, 본인 단독 명단+목표 2명 201/요청 전달을 추가했다. 실제 MySQL HTTP에서도 1의 요청 거절·삽입 없음과 2의 생성 성공을 대조한다. 실행 결과는 PR의 검증 기록을 따른다.
- **프론트 권장 수정:** 앱 `V3Gateway.createUnit`과 웹 `gateway.createUnit`의 목표 인원 초기값을 목데이터와 같은 최소 2 기준으로 맞춘다. 본인만 선택하는 버튼은 유지할 수 있다. API 모드의 요청 본문 검사 테스트도 추가한다.
- **후속:** 프론트 담당자가 웹/앱 API 경로를 수정한 뒤 웹·Android·iOS 실제 빌드에서 다시 확인한다. 이 저장소에서 프론트 소스를 수정하거나 담당자에게 메시지를 보내지 않았다.
- **반영할 곳:** 앱 `V3Gateway.kt`·웹 `gateway.ts`·API 모드 테스트, 백엔드 출시 연결 기록. 서버 계약 문서의 버전 개정은 필요 없다.
