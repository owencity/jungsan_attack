# FC-027 자체 점검

코드 `a31bb2007bd795c04395ff6e5c318c9979047923`. 이 기록은 자체 리뷰이며 외부 리뷰 승인·운영·실기기 성공과 별개다.

- 요구사항: App Review Guideline4의 외부 브라우저 전환은 앱이 바꿔야 한다. 서버는 appReturn=session을 선택한 state에만 고정 스킴303을 제공한다. 앱·웹 저장소는 수정하지 않았다.
- 보안: client·복귀모드·티켓을 허용 목록으로 검사한다. 임의 URL 입력을 받지 않으며 콜백query·UA가 모드를 바꾸지 못한다. state/provider/browser/nonce/PKCE·티켓 만료/일회용은 기존 검증을 유지한다.
- 호환: 기존 app은200 HTML, web은303·WEB 쿠키다. 운영 롤백을 위해 auth_challenges의8컬럼 전체 INSERT를 깨는 컬럼 추가 대신 FK CASCADE 표식만 추가한다. 기존 state 삭제도 표식을 제거한다.
- 데이터: 신규020/changeSet030만 추가하고 기존 checksum·prod 설정·의존성·core 계산은 변경하지 않았다. ERD/API v16·FC027·DEVLOG를 같은 PR에서 갱신한다.
- 트랜잭션: 시작state와 표식INSERT, 완료state 잠금·표식 조회·소비·티켓 생성은 기존 READ_COMMITTED 서비스 트랜잭션 안이다. 외부 제공자 실패는 기존대로 전체 rollback하며 표식도 남아 재시도할 수 있다. 조회는 로그인당 고정1건 추가로 목록 N+1이 아니다.
- 검증: 서버202건·실패/오류/skipped0, compileKotlin/bootJar 성공. 새8건은 양쪽 언어 정책·고정URL·양쪽 제공자303·쿠키·query변조/기존page를 검증한다. 실제 DB probe에 선택 저장·CASCADE·재사용·PKCE·구버전INSERT를 추가했고 실제HTTP는 잘못된모드400을 검사한다. 해당 실제DB·업그레이드 결과는 CI에서 별도 확인한다.
- 데모: 소셜 계정 비밀번호 공유나 새로운 운영 인증 우회를 추가하지 않는다. 명시적 체험 모드를 제안하고 전 기능·링크·탈퇴·심사 Notes의 앱 담당 후속을 문서화했다. 기존 mock store가 전 기능을 충족한다는 확인은 하지 않았다.
- 남은 범위: 최신CI, 리뷰·병합·운영 배포, iOS 앱 적용·기기 E2E, 실제 데모 접근 방법과 App Store Connect 재제출. 진행 중 탈퇴409의 심사 수용 여부는 미확정이다.
