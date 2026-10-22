# 로컬 HTTP·운영 HTTPS 쿠키 검증 보완

원본 v1 코드 89fd5ee8f69f8d4f639389047988bba2f5ec384e, 보완 코드 4d8f1049f577539fcf7ba69cada87ebebd1f5484.

기술별 학습 포인트: HTTP Secure 쿠키 — `verify-auth-release.py`의 웹 로그아웃 검사: local의 cookie-secure=false와 prod의 true를 분리해 검증한다. 실제 운영 Secure 검증은 별도 운영 스모크로 확인한다. 서버·인증 정책 변경은 없고 Java/Kotlin 언어 추가 학습은 해당 없다.

초기 CI39758535100은 로컬 HTTP에도 Secure를 요구한 검사 조건 때문에 실패했다. Core54/Server184와 그 이전 실제 회귀는 성공했으나 전체 성공으로 기록하지 않는다. 수정 후 최신 CI 결과는 PR에 보존한다.
