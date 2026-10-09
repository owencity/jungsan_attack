# PR #10 — 검토 메모와 테스트 후보

기준 head: `1f295946b8d2fabc9eac0bfda82eb0545dde6e4a`. [학습 가이드](pr-10-user-real-name.md)와 [원본 JSON](pr-10-user-real-name.json)을 별도로 둔다.

검토 지점은 결함 확정이나 병합 판단이 아니다. 코드를 수정하거나 테스트를 실행하지 않았다. 기존 개발 검증과 이번 정적 검토를 구분한다.

## 검토 지점

### low · test · UserApiSpec.kt

[관련 파일](https://github.com/owencity/jungsan_attack/blob/1f295946b8d2fabc9eac0bfda82eb0545dde6e4a/server/src/test/kotlin/app/jeongsan/server/user/UserApiSpec.kt)

동시 등록·JPA save의 실명 보존은 실제 DB 회귀 검사로 남기면 좋다. 기존 수동 MySQL 검증을 실패를 재현할 수 있는 통합 검증으로 보존하는 방법을 권한다.

- 근거: UserApiSpec은 UserRepository를 mock으로 대체하고 결과 User를 stub한다. 이전 개발 검증에서는 실제 MySQL 동시 HTTP 요청의 200/409를 확인했지만 그 절차는 테스트 소스에 없다.
- 가능 영향: 조건부 쿼리·flush/clear·컬럼 매핑이 바뀌어도 HTTP 단위 테스트의 stub 응답만으로는 DB 실행 회귀를 발견하지 못할 수 있다. 현재 동시성 결함이 확인됐다는 뜻은 아니다.
- 확인할 점: 동일 이름·다른 이름 동시 등록과 기존 닉네임 갱신의 실명 보존을 MySQL 통합 검사로 반복 가능하게 남길 수 있는가?

### low · security · AuthenticationGuardSpec.kt

[관련 파일](https://github.com/owencity/jungsan_attack/blob/1f295946b8d2fabc9eac0bfda82eb0545dde6e4a/server/src/test/kotlin/app/jeongsan/server/common/AuthenticationGuardSpec.kt)

인증 가드의 검사 범위를 Spring이 실제 등록한 핸들러와 대조할 필요가 있다. RequestMappingHandlerMapping의 등록 목록을 기준으로 검사하는 방법을 권한다.

- 근거: scanner로 찾은 컨트롤러의 Class.forName(...).declaredMethods를 검사한다. declaredMethods는 상속된 메서드를 포함하지 않으며 현재 PR의 핸들러는 직접 선언돼 있다.
- 가능 영향: 나중에 상위 타입에서 상속한 엔드포인트가 생기면 현재 가드가 해당 핸들러를 누락할 수 있다. 현재 공개 누출이 확인된 것은 아니다.
- 확인할 점: 상속·인터페이스 매핑이 생겨도 실제 등록된 모든 API 핸들러를 검사하도록 가드와 검증 fixture를 확장할 것인가?

## 추가 테스트 후보

- **서로 다른 실명 두 요청을 같은 미등록 사용자에게 동시에 전달하는 실제 MySQL 통합 검사** — 기대: 성공 한 건·409 한 건이며 저장 이름은 성공 요청의 값. 이유: 수동 검증 결과를 재현할 수 있는 회귀 검사로 보존한다.
- **같은 실명의 동시 최초 등록과 공백 포함 재시도** — 기대: 두 요청 모두 200이고 저장된 정규화 실명은 하나. 이유: 다른 값 경합과 같은 값 멱등은 독립 계약이다.
- **이미 실명이 있는 사용자에게 예전 실명이 NULL인 detached 엔티티의 닉네임 갱신 save를 실행** — 기대: 닉네임은 갱신되고 기존 실명은 유지. 이유: 일반 JPA 갱신 제외가 카카오 로그인과 겹쳐도 실명 보존을 유지하는지 확인한다.
- **utf8mb4 실제 DB에 보조 평면 문자 10개를 등록하고 재조회** — 기대: HTTP 200·동일 문자열 저장·needsName=false. 이유: MockMvc 코드포인트 검증과 DB 문자 저장 제약을 동시에 검증한다.
- **인증 애너테이션 없는 메서드를 상위 컨트롤러에서 상속해 실제 핸들러로 등록** — 기대: 전체 인증 가드가 공개 whitelist 외 핸들러 누락을 발견. 이유: declaredMethods 기반 검사와 Spring의 실제 핸들러 등록 범위 차이를 확인한다.
- **서로 다른 두 사용자 소유의 실제 술자리를 만들고 한 사용자의 목록 API 조회** — 기대: 로그인 사용자 소유 술자리만 날짜 내림차순으로 반환. 이유: 현재 목록 테스트는 빈 목록 mock 호출을 확인하며 실제 조회 조건과 데이터 제외는 별도 검사다.

