# PR #21 CI·배포 리뷰와 스터디

코드 기준: `c4fa119854137596c5da7f88922ed487900e5637` → `acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5`. [PR #21](https://github.com/owencity/jungsan_attack/pull/21).

origin/main의 STUDY_POLICY·STUDY_CATALOG를 읽었다. 이번 diff는 YAML·Git 파일 모드 변경이며 Java 기본기·Effective Java·함수형 Java·Kotlin 기본·Kotlin 고급에는 직접 대응 항목이 없다. 언어 파일을 추가하거나 목차를 억지로 연결하지 않았다.

## 기술별 학습 포인트

- **GitHub Actions — PR 이벤트와 main push 이벤트** — [.github/workflows/ci.yml:5](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/ci.yml#L5) → `on.pull_request / on.push`: 대상 브랜치를 제한하지 않은 pull_request가 쌓은 PR까지 검증하고 push main이 병합 결과를 다시 검증하는지 diff에서 확인한다.
- **GitHub Actions — 실행별 concurrency 그룹** — [.github/workflows/ci.yml:14](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/ci.yml#L14) → `concurrency`: event_name/ref로 만든 그룹이 다른 PR 검증을 취소하지 않으면서 같은 PR의 이전 실행만 취소하는지 확인한다.
- **Git·Linux — 100644와 100755 실행 비트** — [.github/workflows/ci.yml:37](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/ci.yml#L37) → `verify.steps 실행 권한 확인`: gradlew 내용 diff가 없어도 changes.patch의 mode change가 실행 가능 여부를 바꾸는지 확인하고 test -x 실패와 exit 126을 연결한다.
- **Gradle — test와 bootJar의 태스크 경계** — [.github/workflows/ci.yml:43](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/ci.yml#L43) → `verify.steps Server 테스트와 jar 빌드`: bootJar만 실행하면 서버 테스트를 보장하지 못하므로 명시적인 :server:test를 실행하는지 확인한다.
- **GitHub Actions — always와 테스트 결과 artifact** — [.github/workflows/ci.yml:46](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/ci.yml#L46) → `테스트 결과 보존 if`: 앞 단계 테스트가 실패해도 XML/HTML 업로드가 실행되는지, 결과가 없으면 어떻게 처리하는지 확인한다.
- **GitHub Actions — 수동 실행 ref와 운영 배포 조건** — [.github/workflows/deploy.yml:31](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/deploy.yml#L31) → `jobs.deploy.if`: workflow_dispatch에서 기능 브랜치를 선택해도 main ref 조건으로 운영 배포를 건너뛰는지 확인한다.
- **CI/CD — 검증한 커밋과 배포할 커밋** — [.github/workflows/deploy.yml:51](https://github.com/owencity/jungsan_attack/blob/acd2a4f56e5712f0530a71c1617d91bf0c6fd8b5/.github/workflows/deploy.yml#L51) → `테스트 step`: PR 검증 이후 main의 실제 배포 커밋에서도 Core·Server 테스트가 실패하면 빌드·전송을 진행하지 않는지 확인한다.
