# 2026-11-11 총 정산인원·차수별 참석 인원 서버 배포 기록

FC-035와 FC-037의 서버 변경을 운영에 배포했다. 총무가 지정한 전체 인원과 차수별 예상 참석 인원을 각각 저장하며, 실제 명단과 응답으로 계산하는 기존 core는 변경하지 않았다. 서버 완료와 웹·앱 배포 완료를 구분한다.

## 실행 소스와 문서 기록

- 운영 실행 소스: `9880b58b59dc209e5ecab0e805931232cfaaf70c`
- 이전 운영 소스: `02e8557b78bfc17a428dadd9626800f5d7a9adee`
- PR: [백엔드 #59](https://github.com/owencity/jungsan_attack/pull/59), 브랜치 `fix/total-headcount-backend`. 학습용 PR은 병합하지 않았다.
- 검증한 CI: [42854345618](https://github.com/owencity/jungsan_attack/actions/runs/42854345618), 같은 실행 소스로 전체 성공
- 운영 이미지: `jeongsan-server:9880b58b59dc209e5ecab0e805931232cfaaf70c`, `arm64`
- jar SHA-256: `6a9bf25051791103735fbdd2fd4b752047da9a0ea4ab3914e91e1633d2e0803a`
- 운영 재확인 시각: 2026-11-11 01:40:35 KST

이 기록과 FC 상태를 갱신한 후속 문서 커밋은 배포 소스가 아니다. 해당 커밋을 만들거나 푸시해도 위 실행 이미지와 jar는 교체하지 않는다. PR의 최신 문서 커밋과 운영 실행 SHA를 혼동하지 않는다.

## 완료한 검사

| 구분 | 결과 |
|---|---|
| Core | 59개 통과 |
| Server | 254개 통과, jar 빌드 성공 |
| 실제 HTTP·DB | 213개 통과: 연결 2·총무 확인/UTC/알림 24·알림 재시도 18·UGC 29·인증 HTTP 26·비회원 91·스푼 23 |
| FC-037 신규 시나리오 | 비회원 검사 91개 중 19개. 총 7명·차수 6/6/5, 권한·재시도·명단 변경·확정 후 불변 포함 |
| 업그레이드 보존 | 17개 통과. 기존 JSON·계좌·송금·revision/hash·금액 유지 |
| 인증 DB | 29개 통과 |
| 배포 복원 | 6개 통과 |
| 운영 설정 회귀 | Apple 미설정 프로필 HTTP 6개, DNS·Caddy·방화벽 검사 통과. 이 프로필 검사는 운영 서버의 Apple 설정을 제거한 것이 아니다. |

실제 MySQL·HTTP 검사에서 여섯 명이 먼저 응답한 뒤에도 정산은 OPEN이고 스냅샷은 없었다. 마지막 일곱 번째 응답까지 받은 후에도 총무가 직접 확정해야 한다. 참석 인원 6/6/5와 차수 금액 6,000/6,000/5,000원으로 개인 부담 합계 17,000원, 총무 수취 합계 14,000원을 확인했다. 총인원만 바꾸면 금액은 그대로이며 이전 revision으로 확정할 수 없다.

## 운영 DB와 공개 요청 확인

배포 절차에서 DB를 백업하고, 기존 Liquibase 이력과 확정 정산·송금·근거를 배포 전후 비교했다.

- 기존 변경 이력과 체크섬을 보존했다.
- `033-round-expected-headcount`, `034-unit-total-headcount`만 순서대로 추가했다.
- 새 두 컬럼은 NULL을 허용한다. 기존 값을 추론해 채우는 일괄 갱신은 없다.
- 기존 정산·송금·근거 25개 중 누락 0개, 변경 0개다. 이는 비교 대상 자료의 보존 결과이며 모든 운영 방의 계산을 다시 검증했다는 뜻은 아니다.
- 실행 컨테이너는 `healthy`, jar 해시는 위 산출물과 일치한다.

| 운영 요청 | 확인 결과 |
|---|---|
| health | 200, UP |
| 익명 `/auth/me` | 401 |
| 익명 총인원 PUT, 존재하지 않는 방·단위 0 사용 | 401. 인증에서 차단됐으며 실제 방을 수정하지 않음 |
| Apple Origin의 잘못된 콜백 | 401. CORS 403이 아닌 인증 검증으로 진입 |
| Apple Origin의 다른 API | 403 |
| Apple·카카오 로그인 시작 | 각각 302, 제공자 주소·Secure/HttpOnly 쿠키·캐시 금지 확인 |
| Apple 로그인 요청 | `scope=name`, `form_post`, `SameSite=None` 확인 |

실제 운영 계정으로 총인원 저장이나 정산 확정을 수행하지 않았다. 성공 204, 계산 금액, 동시 요청과 확정 후 409는 격리된 CI DB에서 확인했다. 개인 정보·설정 비밀값을 기록에 포함하지 않았다.

기계가 읽을 수 있는 로컬 결과는 `build/releases/total-headcount/backend-deployment.json`과 `build/total-headcount-release/`의 `production-smoke.json`, `production-verification.json`, `database-verification.json`에 저장했다. 이 산출물은 Git 추적 대상이 아니다.

## 웹·앱 배포와 남은 확인

2026-11-11 12:20 KST까지 확인한 결과다. 웹 본인 확인 대기와 iOS 검증 중이었던 이전 기록을 아래 상태로 갱신했다. 백엔드는 재배포하지 않았으며 실행 소스 `9880b58`을 유지한다.

- 웹: 소스 `1dd8974140488504797b4e429610b8737f3f0967`. 사용자의 이메일 본인 확인 후 12:14 KST에 [Vercel 배포 HiyCtSBQj2shis6kyHKeTENSG1Br](https://vercel.com/kdks-projects-8235f941/jungsan/HiyCtSBQj2shis6kyHKeTENSG1Br)가 `jungsan.devkdk.com`의 Production·Current·Ready임을 확인했다.
- 앱 공통 검증: 소스 `6c59e618f6ad11289f818fac7b7434d0faacf327`의 [CI 42855434587, 두 번째 실행](https://github.com/owencity/jungsan_app/actions/runs/42855434587/attempts/2)이 성공했다. 첫 실행은 iOS 시스템 키보드 안내의 `Continue` 처리 시간 초과로 실패했다. 소스를 바꾸지 않고 재실행해 성공했으므로 첫 실패를 코드 수정으로 해결했다고 기록하지 않는다.
- iOS: 같은 앱 소스로 [TestFlight 20 업로드 실행 42951101378](https://github.com/owencity/jungsan_app/actions/runs/42951101378)이 성공했다. 12:20 KST 로그의 `Upload succeeded`와 `EXPORT SUCCEEDED`를 확인했다. Apple 처리 완료·TestFlight 목록 노출·기기 업데이트는 아직 확인하지 않았다. App Review 제출·승인 완료를 뜻하지 않는다.
- Android: 버전 코드 6 AAB 준비와 CI 성공. Google Play 업로드·심사·사용자 업데이트는 아직 확인하지 않았다.

구버전 앱은 새 총인원 필드를 사용하지 않을 수 있다. 웹 운영 반영과 iOS 업로드가 끝났어도 모든 사용자가 새 앱으로 업데이트했다고 판단하지 않는다.
