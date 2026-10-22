# FC-026 실제 앱 복귀 수정 자체 리뷰

코드 `38150caa0f725507a2d56a740ef969cd775b7351`, 프론트 문서67d2b7c를 기능 브랜치에 통합했다. 문서 PR46과 구현 PR47은 확인 당시 main 병합 전이었다. 이 기록은 자체 점검이며 외부 승인/실기기 성공과 별개다.

- 변경: app200 HTML·자동1회 시도·수동 링크, Android 고정 app.jeongsan Intent와 그 밖 기존스킴, 티켓300초. web303·원래 복귀 경로·WEB 쿠키 유지.
- 정책: Kotlin/Java 링크·만료를 독립 구현하고 구체 기대 URL/300초·만료 경계·주입값 거절을 비교했다. HTTP 렌더러와 기존 JDBC는 공유 인프라다.
- 권한: state/nonce·브라우저 해시·APP/WEB·PKCE·DB 잠금/티켓삭제·실패원자성은 그대로다. UA는 링크 UI 선택이며 인증 판단/원문 HTML 삽입에 사용하지 않는다.
- 정보 보호: 서버 생성43자 base64url 티켓만 링크에 넣는다. 외부 스크립트·이미지·통신 없음, no-store·no-referrer·nosniff, CSP default-none/base-none/form-none/frame-none과 고정 script해시. HTML에서 추출한 실제script 바이트의 해시로 검증했다. URL query를 history.replaceState로 지운다.
- 상관 쿠키: 기존 Path·None/Lax·Secure/HttpOnly 만료 속성과 Apple 출처 전용 CORS를 실제 WebConfig MVC에서 유지했다. 앱에는 WEB JWT 쿠키/Location을 발급하지 않는다.
- Apple POST: 문서 A안의 직접200을 따른다. 새로고침은 소비된state의 기존401이며 새 티켓을 발급하지 않는다. 별도 완료 URL/저장소/의존성을 추가하지 않았다.
- TTL:017 적용 changelog를 바꾸지 않는다. DDL 설명60초는 과거이며 서비스 정책300초를 AUTH_RELEASE·ERD에 명시한다. 실제 DB 저장 시각을 확인하는 probe1건을 추가했다. CI 결과는 PR에 따로 기록한다.
- 단순함: 새 SDK·App Links/Universal Links·Spring Security·전체 기기 로그아웃·면제 등 미결 기능은 추가하지 않는다. 프론트 앱/웹 코드는 수정하지 않았다.
- 검증: 로컬194건(신규 페이지10건), 실패/오류/skipped0·compileKotlin/bootJar 통과. 최초193건 실행 뒤 새로고침 케이스를 추가해 최종194건을 전체 실행했다. git diff --check 통과. 실제 DB/HTTP는 최신 CI가 기준이다.
- 남은 범위: Chrome 정책과 코드에 근거한 수정이며 실제 Android 카카오톡 왕복을 이 환경에서 재현했다고 하지 않는다. UA가 Android를 숨기면 기존스킴으로 내려간다. 자동 전환 차단 시 수동 클릭이 필요하며5분으로 노출 시간이 늘어나는 대신 PKCE/일회용을 유지한다.
- 승인: 앞선 main 병합 요청이 자동 승인 검토에서 거절됐고 CTO의 명시 승인이 아직 없다. 이번 문서 확인 요청을 병합/배포 승인으로 간주하거나 우회하지 않았다. 코드/CI 완료와 운영 반영을 구분한다.
