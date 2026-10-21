# FC-025 자체 리뷰

기준 코드 `0f3482b23f14902a1cc318e9d398a0dc08608ba0`. Codex 자체 리뷰이며 외부 리뷰·사용자 학습 완료와 별개다.

- 범위: 정확한 Apple 콜백 경로에 HTTPS appleid.apple.com·POST·credentials만 허용한다. 기존 /api/** 출처·메서드는 그대로다.
- 보안: state·상관 쿠키·nonce·일회용 challenge 검증을 우회하지 않는다. 기존 잘못된 state는 401이며 정상 WEB/APP 완료는 303이다.
- 쿠키: 기존 운영 None·Secure·HttpOnly·auth Path·300초 속성을 확인했다. 실제 브라우저 계정 왕복은 별도다.
- 재현: 수정 전 운영 요청은 2026-10-21 12:15 KST 403 Invalid CORS request였다.
- 검증: 로컬 서버177건(신규 MVC7건) 성공, 실패·오류·skipped0, compileKotlin·bootJar 성공. 새 Python 문법·git diff --check 통과.
- 회귀: 다른 API·카카오 콜백·유사 도메인·잘못된 출처·GET preflight 거절, 기존 프론트 CORS 401 보존을 테스트했다.
- DB/동시성: DB·업무 트랜잭션·새 목록 조회 변경 없음. 독립 Java 정책 복제 대신 공유 MVC 구성만 변경했다. 의존성·prod 기본값 추가 없음.
- 운영 검증 도구는 고정된 로컬/운영 호스트만 사용하고 리다이렉트를 따르지 않는다. 쿠키·state·인가 URL은 출력하지 않는다. 운영 로그인 시작으로 생성된 challenge는 기존5분 TTL로 만료된다.
- CI 전체 성공·병합·배포·운영 결과는 PR 본문에서 각각 기록한다. 실계정 Safari·TestFlight 최종 로그인은 미실행이다.
- SOLID: CORS 예외는 기존 MVC 구성의 책임 안에 둔다. 별도 정책 계층·상속·인터페이스는 추가하지 않는다.

## FC 범위 확인

문서 기준 `493d1a74e908ba3daa31e1f4b55cce0c250303b2`. 열린 FC-005·009·010·011·014·015·020·021·025를 확인했다. 이번 반영은025 및021의 누락 콜백 검증 기록이다. 나머지는 기존 내부 알림·정산·계좌·프론트 연결 및 보류 결정 범위로 이번 긴급 인증 수정에서 변경하지 않는다. FC-012 결정 필요도 구현하지 않는다. 문서 브랜치 docs/fc-025-apple-callback-cors의 대상 FC는 main에 반영된 문서와 대조했다.
