# FC-034 카카오톡 로그인 운영 기록 · 2026-11-05 KST

## 서버 배포 완료

- 소스 `8e23b7fa2eb1ff29beaa14cc0d9250fb6069c856`, [PR56](https://github.com/owencity/jungsan_attack/pull/56).
  [CI42023282001](https://github.com/owencity/jungsan_attack/actions/runs/42023282001) 전체 성공:
  서버238건(실패/오류/건너뜀0), core·실제 MySQL/HTTP·인증 DB·실패 복원·Caddy 검증 포함.
- 기존 운영 `8137cb9bd993dcfc6ed141ae457dc8123ff93533`가 새 소스의 조상이며,
  실행 코드 차이는 FC-034 인증 경로와 설정이다. 새 changelog/core 변경은 없다.
- 전송 전/후·ARM 이미지·실행 컨테이너의 jar SHA256:
  `6b85da60162cead14e621bf324fa3c235d14b8d25cd0a0b92a7f5c917e45fd0c` 일치.
- 이미지 `jeongsan-server:8e23b7fa2eb1ff29beaa14cc0d9250fb6069c856`,
  image ID `sha256:74c3c8bd41aea1a99d82984302e0b2ec33320bd8ac0d7f76976e02d8ca293561`.
- 운영 `.env`에 공개 숫자 `KAKAO_APP_ID=1547878`을 추가하고 새 compose를 적용했다.
  카카오 REST 키의 해시를 대조해 이 앱이 기존 운영 로그인과 같은 개발자 앱임을 확인했다.
  Native app key/Client Secret/토큰/개인정보 값은 이 문서에 보존하지 않는다.
- 22:03 KST 배포 전 OCI 내부 `backups/pre-native-kakao-20261105T130319Z/`에 DB·기존 설정을
  권한600으로 백업했다. 기존 이미지8137cb9는 복구용으로 유지했다. 잠금·헬스·실패 복원을
  유지하는 기존 deploy.sh로 적용했고 실제 컨테이너/18080 건강 검사와 새 release 이력을 확인했다.
- 첫 실행은 자동 승인 검토가 두 번째 이미지 인자 지원을 의문으로 차단했다. 실제 운영
  deploy.sh 해시 `eb676eff771ad6536ce9f51b66e5fb25607efb06a14db6d44293f3840d76df2e`,
  둘째 인자→export APP_IMAGE→건강 성공 뒤 이력 기록 로직과 CI의 같은 호출6건을 확인한 뒤
  동일 명령의 재검토가 승인돼 배포했다. 차단 시점에는 운영 변경이 시작되지 않았다.

| 운영 HTTPS 확인 | 결과 |
|---|---|
| health | 200 / UP, 실행 컨테이너 healthy |
| auth/me 무인증 | 401 UNAUTHENTICATED |
| native 카카오 잘못된 토큰 | 401 UNAUTHENTICATED, 쿠키 미발급 |
| native 빈 필드 | 400 VALIDATION_FAILED, 쿠키 미발급 |
| native 잘못된 JSON | 400 MALFORMED_REQUEST, no-store·쿠키 미발급 |
| Apple Origin form POST 콜백의 잘못된 state | 401, CORS403 아님 |
| Apple Origin으로 다른 API POST | 403, 허용 범위 유지 |
| Apple/Kakao 앱 인가 시작 | 각각302, 예상 HTTPS 제공자/콜백·Secure/HttpOnly·no-store 검사 통과 |
| 검증용 가짜 제공자 토큰의 운영 로그 포함 여부 | false |

no-store 성공200은 MockMvc에서 검증했다. 운영의401/필드400에는 Cache-Control이 없고,
잘못된 JSON400에는 no-store가 있음을 구분한다. 정상 제공자 토큰의200은 실제 기기 후속이다.
이 점검은 실제 계정의 로그인/탈퇴·정산 데이터를 변경하지 않았으며 인가 시작용 임시 state2개만 생성했다.

## 카카오·GitHub 설정 완료

- 기존 카카오 앱 **정산어택 / 1547878**의 기존 Native key에 iOS 번들 `app.jeongsan`을 등록했다.
  CTO가 저장 직전 두 설정을 명시 승인했으며 저장 뒤 재진입해 값과 수정 시각을 확인했다.
- 같은 키를 `owencity/jungsan_app`의 GitHub Actions Variables **KAKAO_NATIVE_APP_KEY**로 저장했다.
  기존 JS_API_BASE_URL을 유지하고 공개 Native key만 사용한다.
- 새 카카오 개발자 앱 생성·사용자 연결 이전은 하지 않는다. 운영 서버는 같은 앱 ID를 검사한다.

## 앱 검증과 TestFlight

- 앱 소스 [PR9](https://github.com/owencity/jungsan_app/pull/9), 제품 변경843dc5d,
  Close 이름 검사 보완1e40bb7, 중복 사전 설치 제거e3fc5bc.
- 로컬 Android/공통237건(실패/오류/건너뜀0). 일반 CI42023356215 및
  [42028457518](https://github.com/owencity/jungsan_app/actions/runs/42028457518) 전체 성공:
  Android·iOS 기기 타깃·Swift·기존 iPhone/iPad 데모/이름/이미지 검사 포함.
- Release 인증 CI42023356204는 iPhone2건 및 iPad 카카오 안내/취소/Apple 대안이 성공했다.
  iPad 직접 Apple 검사1건은 Cancel 이름만 찾다 실패했고 실제 영상 마지막에는 정상 Apple
  입력 화면이 있었다. Close/닫기를 검사에 추가하고 Apple 대안도 닫기·앱 복귀를 필수 검사했다.
- 1e40bb7 재검증42028457453은 양쪽 XCTest 실행 전에 준비 명령 시간 제한으로 중단됐다.
  부팅 종료 로그는 있으나 당시 진단에는 명령이 없어 정확한 중단 명령은 확정하지 않는다.
  e3fc5bc는 xctestrun이 수행하는 설치와 중복된 simctl install을 제거하고 실패 명령을 남긴다.
  테스트 단언/대기 제한은 유지한다. [e3 재검증42033427282](https://github.com/owencity/jungsan_app/actions/runs/42033427282) 진행 중.
- [TestFlight18 실행42033810265](https://github.com/owencity/jungsan_app/actions/runs/42033810265)을
  소스 `e3fc5bcc2665d14da0e86cbed95dd2f5981419a5`, export 서명, 운영 API 주소로 실행했다.
  실제 Native key 설정 검사 통과 후 archive 진행 중이다. 업로드 성공·Apple 처리 완료로 보고하지 않는다.

PR은 스터디용으로 열어두고 main 병합은 하지 않았다. TestFlight18은 실기기 검증용 후속이며
카카오톡 설치 기기의 실제 동의/취소/앱 복귀·기존 계정 유지, 미설치 안내의 명시적 App Store 열기,
Apple 로그인 완료와 App Store 재심사 결과는 별도로 확인한다.
