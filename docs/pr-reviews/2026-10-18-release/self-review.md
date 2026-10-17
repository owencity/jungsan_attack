# 출시 준비·기존 데이터 전환 자체 점검

대상 [PR #37](https://github.com/owencity/jungsan_attack/pull/37), `b8b91bb348b828a6ec0ff98a3749fc8d66c5a15b` → `e9a3d50b0d11f87507ef80ceb772f04850f2442e`. 구현자의 자체 점검이며 별도 외부 리뷰를 실행한 기록이 아니다.

- 문제: 새 DB 검증만으로는 이전 체크섬·암호화 계좌·정산 명세·토큰의 연속성을 알 수 없었다. 출시 연결 대조 중 웹/앱의 본인 단독 생성 요청 불일치도 찾았다.
- 범위: CI/검증·회귀 테스트·FC 기록. 사업 규칙·Java/Kotlin 계산·API·DB·prod 설정·라이브러리 변경 없음. 테스트 인프라를 두 언어로 복제하지 않는다.
- 격리: GITHUB_ACTIONS·숫자 run ID·컨테이너 소유 라벨·고정 로컬 포트/DB를 검사한다. 운영·개발 컨테이너를 재사용하지 않는다. 기존 cleanup도 CI 소유 라벨을 확인한다.
- 이전 기준: 원격 main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`의 Boot 3.5.16을 별도로 빌드한다. 계좌·정산·미리보기는 실제 구버전 HTTP로 저장한다. 제공자 승인은 fixture 범위 밖이다.
- 인증: WEB은 공개 로컬 키로 만든 테스트 쿠키를 양쪽 서버에서 사용했다. APP은 fixture 티켓을 구버전 HTTP로 교환해 실제 발급받았다. 교차 사용·로그아웃 재사용 거절을 확인한다.
- 데이터: 기존 Liquibase ID/파일/체크섬/실행 시각과 계좌 암호문, 정산/날짜/타임라인 JSON을 비교한다. 기존 미리보기로 정산하고 기존 송금을 완료한다.
- 앱 계약: 명단 1명은 허용하지만 자동 정산 목표 인원은 2 이상이다. 1 거절·삽입 없음 및 2 허용을 검증한다. 프론트의 UI/API 모드·목 모드 차이는 FC-023에 수정 방법과 함께 기록한다.
- 실행: 로컬 core 54·server 146건, 실패/오류/건너뛰기 0, bootJar 성공(31초). :server:compileKotlin 성공(12초). Python AST·CI 밖 seed/verify 거절·Bash 문법 통과.
- 도구 한계: --tests로 선별 실행한 61건은 통과했으나 제외된 spec에 No TestIdentifier listener 경고가 발생했다. 이를 전체 통과 근거로 사용하지 않았다. 전체 146건 결과와 원격 CI를 따로 확인한다. 테스트/리스너를 비활성화하거나 프레임워크 버전을 임의로 바꾸지 않았다.
- 첫 CI: source 227a067의 리허설 12건과 전체 기존 회귀가 통과했다. 추가 앱 경계 검사는 최종 source의 별도 CI 결과를 따른다. 원문 단계 상태와 로그 성공 줄 발췌를 함께 보존한다.
- 최종 코드 CI: [run 43](https://github.com/owencity/jungsan_attack/actions/runs/39055094203), source e9a3d50에서 Core 54·서버 146·Boot 3 기준 빌드·기존 DB 전환 12·앱 요청 2·기존 실제 DB 회귀·prod 재기동·배포 복원·DNS 전부 성공했다. JSON은 단계 상태, txt는 성공 줄 발췌이며 전체 로그 사본은 아니다.
- 증거: public-probe.json은 10/18 운영의 비인증 요청이다. app-source-evidence.json은 로컬 파일 해시/발췌이며 dirty 상태·빌드 동일성은 미확인이다. web-source-evidence.json은 원격 기본 브랜치의 blob SHA/발췌이며 배포 빌드가 아니다.
- 미완료: 실제 계정 OAuth 승인·웹/iOS/Android 최종 빌드 실행, 운영 DB 사본의 모든 데이터 상태, PR 병합·운영 배포는 완료로 처리하지 않는다. 없는 경로의 운영 500은 PR #33 미적용 상태로 남아 있다.
