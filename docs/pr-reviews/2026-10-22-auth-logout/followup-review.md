# 실제 HTTP 검증 후속

보완 코드 4d8f1049f577539fcf7ba69cada87ebebd1f5484. 서버 소스/184건 로컬 테스트 결과는 기존89fd5ee와 같다. Python 테스트에서 로컬 비Secure 쿠키를 명시 검증하고 운영용 Secure 확인은 운영 스모크로 분리했다. 테스트를 삭제하거나 건너뛰지 않았다. 초기 실패와 최신 전체 CI는 PR에 각각 기록한다.

자동 AI 질문 검토: RestProviderGateway.identity는 ApiException을 보존하고 나머지 Exception은 UnauthenticatedException으로 변환한다. AuthMaintenance가1분 주기로 만료 challenge/ticket/revocation을 정리한다. 기존 controller/service state검증은 유지하며 MVC 필수 파라미터 누락400을 새 단계 로그로 전부 추적한다고 주장하지 않는다. 잘못된 Bearer 우회 금지는 기존 AuthPolicy/리졸버 테스트 범위다. 실계정 로그인 원인 미확정은 유지한다.
