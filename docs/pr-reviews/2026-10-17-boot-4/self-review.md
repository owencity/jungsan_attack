# Boot 4 자체 점검

대상 [PR #36](https://github.com/owencity/jungsan_attack/pull/36), `e258f1c338e8ba8b4d2c99fe7b3aa17464055287` → `ad0b154effea3aa72e004d367f9f15150d66e122`. 외부 리뷰가 아닌 구현자의 자체 점검이다.

- 변경 범위: Boot/Kotlin·기능별 starter·Jackson API·애너테이션 적용 위치와 호환성 테스트. 도메인·API 계약·스키마·운영 설정 변경 없음.
- 의존성: QueryDSL/Cloud 실사용을 검색해 0건 확인했다. JPA는 UserRepository에서 쓰므로 유지한다. JJWT의 Jackson 2는 런타임 구현으로 남긴다.
- 자동 설정: Boot 4에서 Liquibase 코어만으로 자동 실행이 보장되지 않으므로 starter로 교체했다. 실제 CI MySQL의 스키마 적용·기동 및 prod 재기동이 통과했다.
- 권한: 새 핸들러·공개 URL 없음. 인증 가드·WEB/APP 토큰·잘못된 서명/만료·Apple 헤더 양쪽 구현 테스트 통과.
- 금액·동시성: 계산/잠금/트랜잭션 코드 불변. core 54·server 144건 통과, 실패/오류/건너뛰기 0. 실제 MySQL 정산/UTC 20건·알림 경합 18건·HTTP 인증 17건·DB 인증 probe 15건도 통과했다.
- 저장 호환: 기존 계좌 JSON 문자열을 고정해 앞자리 0·한글·필드 값을 대조한다. 암호화 방식 변경은 없다.
- API 직렬화: Boot 자동 설정 JsonMapper의 날짜·null·큰 정수와 RestClient의 제공자 DTO 바인딩을 검증했다. 오류 400/401/404/500 기존 스펙도 통과했다.
- 실패 이력: 최초 Jackson 전환 테스트 컴파일에서 변환기가 JsonMapper를 요구하고 valueToTree의 타입 추론이 실패했다. 빌더/제네릭을 명시해 수정했고 테스트를 삭제·비활성화하지 않았다.
- 한계: 로컬 Docker 미기동. 실제 제공자 로그인·사용자 기기·운영 적용은 검증 완료로 표시하지 않는다. 운영 배포는 이 PR에서 실행하지 않는다.
- CI 범위: 격리한 새 MySQL에 changelog를 적용하고 같은 DB로 prod 프로필을 재기동했다. 운영 DB의 기존 Liquibase 체크섬과 데이터에 대한 업그레이드 리허설은 아니다.
- 근거: [Backend CI run 39](https://github.com/owencity/jungsan_attack/actions/runs/39042279879) 전체 성공. `ci-success-01.json`은 단계 상태 원문, `ci-log-excerpt-01.txt`는 GitHub 로그의 성공 줄을 추출한 것이며 전체 로그 사본은 아니다. 실제 CI에서 prod 제공자 가용성·배포 복원 6건·DNS도 통과했다.
- 학습: 확정 diff·before/after·정책/카탈로그 SHA·파일 해시를 별도 archive에 보존한다. 단순 import 변경을 언어 학습으로 부풀리지 않았다.
