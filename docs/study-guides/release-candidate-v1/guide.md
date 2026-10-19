# OCR 제외 출시 후보 학습 방향표

[PR #39](https://github.com/owencity/jungsan_attack/pull/39), `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420` → `8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095`. 정책·목차는 origin/main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`다. 기존 PR의 추천을 현재 diff와 다시 대조해 파일·기호·줄을 고정했다. 통합 자체를 새 Java/Kotlin 구현으로 꾸미지 않는다.

## Java 기본기

- JST-08-01-02 · 8장 예외 처리(exception handling) 1.2 예외 클래스의 계층구조 p.445 — [server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt:38](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt#L38): 경로 부재의 구체 예외 두 타입과 기존 Exception 처리의 선택 관계를 확인한다.
- JST-14-02-03 · 14장 람다와 스트림 2.3 스트림의 중간연산 p.958 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java:10](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java#L10): distinct와 filter가 받는 사람의 중복·행동자 제외를 결정하는 새 파이프라인을 읽는다.

## Effective Java

- 직접 관련된 별도 핵심 학습 주제 없음.

## 함수형 Java

- FJ-06-03 · 스트림 파이프라인 구축하기 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java:11](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java#L11): DB 저장과 분리된 스트림의 중간 연산·최종 toList가 수신자 목록을 만드는 연결을 확인한다.

## Kotlin 기본

- KB-07 · 7강, 코틀린에서 예외를 다루는 방법 — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:38](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L38): 테스트 실패 시에도 use가 MVC 컨텍스트를 닫는 자원 수명주기를 확인한다.
- KB-15 · 15강, 코틀린에서 배열과 컬렉션을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:231](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L231): 차수별 자동 응답을 참여자 Set으로 모으는 변경이 사용자별 알림 중복을 제거한다.
- KB-17 · 17강, 코틀린에서 람다를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:220](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L220): groupBy 람다와 참여자별 조회가 송금자 요약과 수취인만 있는 참여자 안내를 분리한다.

## Kotlin 고급

- KA-20 · 20강, 코틀린의 어노테이션 — [server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt:27](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt#L27): 생성자 프로퍼티의 파라미터 적용 대상을 고정한 변경이 Kotlin 버전별 Spring 설정 주입 위치와 연결된다.

## 기술별 학습 포인트

- Spring MVC — [server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt:39](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt#L39): 구체 예외 핸들러의 선택과 기존 catch-all 500 처리의 관계가 HTTP 상태를 결정한다.
- Spring MVC — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:104](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L104): 등록된 리소스 핸들러 안의 경로 부재와 매핑 자체 부재가 다른 예외를 생성한다.
- MockMvc — [server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt:40](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/GlobalExceptionHandlerSpec.kt#L40): 실제 DispatcherServlet 요청과 엄격 JSON 비교가 404 상태뿐 아니라 오류 본문의 정보 노출 회귀를 검증한다.
- JDBC 배치 조회 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:242](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L242): IN 조회가 결제자 계좌 상태를 한 번에 모으므로 알림 생성에 N+1을 추가하지 않는다.
- MySQL 트랜잭션·멱등 — [scripts/verify-notifications.py:87](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-notifications.py#L87): 실제 최초 계좌 등록의 동시 두 요청이 사용자별 알림 하나로 남는지 SQL 잠금과 HTTP 결과를 함께 검사한다.
- 단위 상태·삭제 계약 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:245](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L245): 단위 완료와 술자리 전체 완료를 구분한 상태로 삭제 예고를 정하므로 알림 내용과 실제 삭제 시점을 대조한다.
- Gradle 의존성 관리 · 컴파일러·BOM·도구 모음의 버전 경계 — [gradle/libs.versions.toml:2](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/gradle/libs.versions.toml#L2): Kotlin 플러그인·Boot BOM 버전을 함께 맞추면서 Java 도구 모음과 프레임워크 기준을 구분한다.
- Spring Boot 4 · 기능별 starter와 자동 설정 발견 — [server/build.gradle.kts:23](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/build.gradle.kts#L23): Liquibase 라이브러리만 있는 것과 Boot가 기동 시 실행하는 자동 설정 모듈의 차이가 DB 준비를 결정한다.
- Jackson·JJWT · JSON 메이저 버전 공존과 런타임 의존성 — [server/build.gradle.kts:32](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/build.gradle.kts#L32): 애플리케이션 Jackson 3와 JJWT 내부 Jackson 2가 다른 패키지로 공존하므로 전역 제외의 영향을 확인한다.
- Jackson 3 · 매퍼 빌더와 JSON 노드 문자열 접근 — [server/src/main/java/app/jeongsan/server/user/javaimpl/AppleTokens.java:23](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/main/java/app/jeongsan/server/user/javaimpl/AppleTokens.java#L23): 구성 완료 뒤 생성한 매퍼와 문자열 전용 헤더 접근이 Apple JWT의 기존 거절 경로를 유지하는지 읽는다.
- Spring Boot·JSON · 자동 설정 JSON 계약 회귀 검증 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:25](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L25): 운영 자동 설정 매퍼로 ISO 날짜·null·큰 정수를 검증해 수동 테스트 매퍼와의 차이를 확인한다.
- 데이터 호환성 · 저장 JSON의 후방 호환성과 독립 기대값 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:48](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L48): 이전 저장 문자열을 고정해야 새 직렬화 결과끼리 비교하며 계좌번호 선행 0 손실을 놓치지 않는다.
- Spring RestClient · HTTP 클라이언트 변환기와 외부 응답 바인딩 — [server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt:61](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/common/JsonCompatibilitySpec.kt#L61): RestClient 기본 변환기의 별칭·추가 필드·큰 ID 처리를 실제 제공자 로그인과 구분해 검증한다.
- CI 기준 소스 · 이전 실행 산출물의 재현 가능한 기준 — [.github/workflows/ci.yml:49](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/.github/workflows/ci.yml#L49): 확정된 이전 소스와 새 소스를 따로 빌드해야 같은 구현을 구버전처럼 시험하는 오류를 피한다.
- 서버 수명주기 · 같은 DB를 유지한 프로세스 교체 — [scripts/verify-release.sh:43](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-release.sh#L43): Boot 3의 저장 뒤 서버만 교체하므로 새 DB 기동과 실제 업그레이드 검증을 구분한다.
- Liquibase · 적용 이력·체크섬·실행 시각 불변 — [scripts/verify-boot-upgrade.py:155](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-boot-upgrade.py#L155): 업그레이드 시 기존 changeSet을 다시 쓰거나 재실행하지 않았는지 DB 이력으로 대조한다.
- 저장 데이터 · 암호문 보존과 읽기 호환성 — [scripts/verify-boot-upgrade.py:156](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-boot-upgrade.py#L156): 같은 계좌를 새로 저장하는 것과 구버전 암호문·JSON을 새 런타임에서 읽는 검증은 다르다.
- JWT·HTTP · 티켓 fixture와 실제 토큰 발급의 경계 — [scripts/verify-boot-upgrade.py:100](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-boot-upgrade.py#L100): 제공자 승인은 시험하지 않고 구버전 서버의 실제 티켓 교환·토큰 발급·새 서버의 검증을 이어 읽는다.
- 정산 입력 계약 · 버전 전환 전 미리보기의 revision/hash — [scripts/verify-boot-upgrade.py:171](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-boot-upgrade.py#L171): 이전 입력 해시를 그대로 보내 정산하므로 런타임 변경이 준비된 요청의 계약을 깨는지 확인한다.
- HTTP·DB 검증 · 검증 실패의 저장 부수효과 — [scripts/verify-boot-upgrade.py:194](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/verify-boot-upgrade.py#L194): 앱의 목표 인원 1 요청이 400이어도 단위를 남기지 않는지 실제 DB 개수와 함께 검증한다.
- Spring MVC 검증 · 요청 검증과 서비스 호출 경계 — [server/src/test/kotlin/app/jeongsan/server/gathering/GatheringApiSpec.kt:85](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/server/src/test/kotlin/app/jeongsan/server/gathering/GatheringApiSpec.kt#L85): 명단 크기와 자동 정산 목표 인원을 구분하고 Bean Validation 거절이 서비스 실행 전에 일어나는지 읽는다.
- Caddy·ACME · 같은 도메인의 자동 HTTPS와 DNS only 직접 연결 — [infra/caddy/Caddyfile:20](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/infra/caddy/Caddyfile#L20): 호스트 선언이 자동 인증서 발급을 시작하는 지점이다. DNS가 Tunnel을 가리키면 왜 Caddy의 80/443 검증이 실패하는지 보고, A 레코드와 proxy 상태를 각각 확인한다.
- Caddy·라우팅 · handle의 경로 보존과 공개 엔드포인트 제한 — [infra/caddy/Caddyfile:22](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/infra/caddy/Caddyfile#L22): /api/*와 정확한 health만 넘긴다. handle_path로 바꾸면 콜백 경로가 어떻게 달라지는지, metrics가 앱까지 도달하지 않는지 실제 Caddy 테스트의 denied 요청 수 검증과 함께 본다.
- Caddy·로그 · 접근 로그와 오류 로그의 차이 및 중첩 필드 제거 — [infra/caddy/Caddyfile:9](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/infra/caddy/Caddyfile#L9): access log를 켜지 않아도 upstream 실패의 오류 로그에 쿼리·헤더가 들어갈 수 있다. verify-caddy-ingress.py에서 실제 502를 만들고 URI·헤더·DUMMY_TOKEN이 제거됐는지 검사하는 부분을 대조한다.
- iptables · 첫 일치 규칙과 REJECT 앞 삽입·멱등 실행 — [scripts/ingress/allow-caddy-ports.py:17](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/ingress/allow-caddy-ports.py#L17): 80/443 허용을 최종 REJECT 뒤에 붙이면 왜 도달하지 않는지 본다. 중복·SSH 누락·예상 밖 레이아웃을 먼저 거절하고 재실행 시 빠진 규칙만 추가하는 테스트를 diff에서 확인한다.
- 배포·동시성 · 앱 배포와 Caddy 설치가 공유하는 flock — [scripts/ingress/install-caddy.sh:10](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/ingress/install-caddy.sh#L10): 기존 deploy.sh와 같은 잠금 파일을 쓰는 이유를 확인한다. 잠금 중 앱·DB를 재시작하지 않고, 기존 Caddy 설정·다른 웹서버를 발견하면 자동 덮어쓰기 전에 멈추는 조건을 읽는다.
- 운영·복구 · live 방화벽과 재부팅용 저장 파일의 차이 — [scripts/ingress/allow-caddy-ports.py:81](https://github.com/owencity/jungsan_attack/blob/8aa2b46adf1a0a2a7da68cbfc1b2295f8bd88095/scripts/ingress/allow-caddy-ports.py#L81): Docker·Tailscale의 동적 체인을 통째로 저장·restore하지 않는 이유를 본다. saved rules는 두 줄만 변경하고 실패 시 이번 실행이 추가한 live 규칙만 제거하는 범위를 점검한다.

