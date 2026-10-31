# 비회원 참여와 본인 응답 자체 점검

## 범위와 고정 소스

- PR: https://github.com/owencity/jungsan_attack/pull/50
- base: `09a33f023a8713d1b328ae609123de56f56d72ca` (PR49, 제품 방향).
- 실행 코드 commit: `9da531495af9876aa47ddd67f81e20c35a54da4e`.
- 자체 점검이며 외부 시니어 리뷰·CTO 병합 승인·운영 적용의 근거로 사용하지 않는다.

## 권한과 정보 노출

GuestTokens는 기존 서명 설정을 이용하되 purpose와 비숫자 subject를 요구한다. 회원 파서는
비회원 subject를 숫자로 해석할 수 없고 비회원 파서는 회원 purpose를 받지 않는다.
GuestBrowserResolver는 httpOnly 쿠키만 읽는다. 컨트롤러에는 수정 대상 participantId를 받는 API가 없다.
guestPerson은 브라우저 해시·술자리·ACTIVE·user_id NULL과 별도 표식을 함께 확인한다.
따라서 탈퇴한 회원 좌석이 user_id NULL이 됐다는 이유로 비회원 권한을 얻지 않는다.

session을 포함한 모든 guest 쓰기는 정확한 frontend-origin을 검사한다. 기존 WebConfig의
허용 출처와 회원 API를 넓히지 않는다. 공개 bootstrap 하나만 인증 가드 허용 목록에 추가하고
나머지 guest 핸들러는 GuestBrowser를 요구한다. 토큰은 JSON·URL로 반환하거나 로깅하지 않는다.
본인 조회는 타인 명단·계좌·응답·대화·송금을 제공하지 않는다.

## 중복과 원자성

guestJoin은 단위→술자리 잠금 후 브라우저 신원을 조회한다. 다른 단위의 동시 요청도 술자리 잠금을
공유하므로 신원 조회와 INSERT 사이에 중복이 끼지 않는다. DB PK와 참여자 복합 FK가 추가 방어선이다.
이미 포함된 단위의 POST는 현재 상태만 반환한다. 응답 수정과 재참여의 의미를 분리해 오래된 재시도가
나중 응답을 덮지 않는다. 자동 정산 직후에도 동일 참여의 재시도는 성공한다.

클래스 수준 @Transactional(READ_COMMITTED)이 참여자·권한·명단·응답·알림을 묶는다.
내부 guestMe 호출은 새 readOnly 트랜잭션을 시작하지 않고 외부 쓰기 트랜잭션 안에서 실행된다.
응답 두 개 중 첫 답변 저장 후 둘째가 실패하는 경우까지 전용 MySQL probe로 검증한다.
테스트는 실제 DB 검증이므로 Mock 성공만으로 잠금·롤백을 입증하지 않는다.

## 기존 동작과 구현 경계

회원 응답과 비회원 응답은 respondAs에서 동일한 단위·차수 검증, SELF/HOST, EXEMPT 보호,
revision, 알림, 자동 정산을 이용한다. 기존 독립 Java SettlementWorkflow 정책·계산은 바꾸지 않았다.
새 이름·Origin·재참여 이름 정책은 Java와 Kotlin에 독립 구현하고 각각 기대값을 검증한다.
HTTP/JWT/JDBC와 Spring 트랜잭션은 공유 인프라다.

총무 명단의 활성 비회원은 표식 존재를 검사해 허용한다. 계좌 권한이 없는 비회원 수취인은
saveRound에서 거절한다. 알림 정책은 userId 0을 제외하므로 비회원 가상 회원을 만들지 않는다.
본인 조회는 참여자 수만큼 쿼리하지 않고 고정된 배치 조회를 사용한다. Core 금액 계산은 그대로다.

신규031만 추가하고 적용된 changelog를 변경하지 않았다. 참여자 FK CASCADE라 기존 술자리 삭제를
막지 않는다. 기존 버전으로 롤백하면 guest API가 사라지는 점과 신원 복구 미지원은 운영 전 고려할 제한이다.
신규 prod 설정·기본값·라이브러리는 없다.

## 검증 기록

로컬 컴파일 성공45초, :core:test :server:test :server:bootJar 성공1분34초.
XML 결과는 Core54·Server219, 실패/오류/건너뜀0이다. Python 구문과 git diff --check도 통과했다.
실제 MySQL·Boot3→4·기존 알림/UGC/인증·배포 복원·Caddy 결과는 최종 CI 링크와 함께 PR에 기록한다.
첫 CI41199395327은 기존 업그레이드·자동 정산·알림·UGC·인증 검증 뒤 guest probe에서 멈췄다.
MVC CORS의 정상적인 text/plain 403을 probe가 JSON으로 읽은 원인이어서 Content-Type에 따라 파서를
구분했다. 검증을 제거하지 않았고 서로 다른 단위 동시 참여·다른 브라우저 동시 참여·중간 답변 실패
롤백·동일 응답 재시도 검증도 추가했다. 서버 실행 코드 자체는9da5314 그대로다.
운영·모바일웹·실기기·정산표·캡처는 별도이며 이 자체 점검으로 완료 처리하지 않는다.

## 남은 범위

쿠키 초기화 동시 실행은 웹에서 직렬화해야 한다. 쿠키 삭제·만료·기기 이동의 본인 복구와 회원 연결,
비회원 결제자·계좌·보냈어요·메시지·신고/차단·스푼, 웹과 자동 정산표·캡처는 후속이다.
FC-028은 열림을 유지한다. 이 PR만 운영 배포해도 기존 참여 웹이 자동으로 비회원 흐름으로 바뀌지 않는다.
