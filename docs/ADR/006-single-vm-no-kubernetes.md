# ADR-006 · 단일 VM에 docker compose로 배포한다

**상태** 확정 · **관련** `SPEC.md` §10

## 맥락

배포 대상은 **OCI Always Free 인스턴스 한 대(2 OCPU / 12GB, Ampere A1 = ARM64)** 다.
여기에 앱·PostgreSQL·Loki·Prometheus·Grafana를 모두 올린다.
ArgoCD, GitLab CI, Jenkins를 검토했다.

## 결정

```
GitHub Actions (ARM 러너)
  → Gradle 빌드·테스트 (Testcontainers)
  → Docker 이미지 빌드 (ARM64)
  → GHCR push
  → SSH: docker compose pull && docker compose up -d
```

**K8s·ArgoCD·Jenkins·GitLab을 쓰지 않는다.**

## 근거

### ArgoCD는 애초에 쓸 수 없다

**Kubernetes 전용 GitOps 도구다.** docker compose 환경에는 동기화할 대상이 없다.
K8s를 깔면? **k3s 컨트롤 플레인만으로 1~2GB, ArgoCD가 ~500MB를 먹는다.**
이미 빡빡한 12GB에서 서비스가 쓸 자원이 사라진다.
게다가 **노드가 하나라 스케줄링·자가치유·롤링 업데이트라는 K8s의 본질이 전부 무의미하다.**

### Jenkins는 자기 발등을 찍는다

**Jenkins는 서버가 필요하고, 그 서버는 결국 이 인스턴스다.**
Jenkins(JVM ~500MB~1GB) + 빌드 시 Gradle 데몬(~1~2GB)이 **운영 중인 앱과 같은 메모리를 놓고 경쟁**한다.
빌드할 때마다 서비스가 느려지고, 최악의 경우 빌드가 앱을 OOM으로 죽인다.

### GitLab은 옮길 이유가 없다

GitLab CI 자체는 훌륭하지만 GitHub을 이미 쓰고 있고, ARM 러너와 GHCR이 그대로 붙는다.
GitLab의 강점인 셀프호스팅 통합은 **또 서버를 요구한다.**

### ARM 빌드는 선택이 아니다

**Always Free의 2 OCPU / 12GB는 Ampere A1, 즉 ARM64다.** x86 Always Free는
1/8 OCPU짜리라 이 앱을 못 올린다. **GitHub Actions 기본 러너는 x86이라
그냥 빌드하면 ARM 서버에서 뜨지 않는 이미지가 나온다.**
ARM 러너(퍼블릭 리포 무료)를 쓴다. `buildx` + QEMU는 3~5배 느려 차선이다.

### 모니터링이 서비스를 죽이지 않게

**컨테이너별 메모리 상한과 스왑 2~4GB를 처음부터 지정한다.**

```yaml
deploy: { resources: { limits: { memory: 1g } } }
```
```
JVM: -XX:MaxRAMPercentage=70
Prometheus: 보존 7~15일, 스크랩 주기 30초~1분
```

상한이 없으면 Prometheus가 부풀어 앱을 OOM으로 죽인다. **감시자가 감시 대상을 죽이는 셈이다.**

## 대가

- **무중단 배포가 안 된다.** `up -d` 시 짧은 다운타임이 있다
- **원클릭 롤백이 없다.** 이전 이미지 태그로 다시 배포해야 한다
- 서버가 통째로 죽으면 **모니터링도 같이 죽어 알림이 안 온다**

## 재검토 조건

- **노드가 2대 이상**이 될 때 → K8s + ArgoCD를 검토한다
- 무중단 배포·롤백이 필요해질 때 → **Kamal**을 먼저 본다.
  K8s 없이 헬스체크 기반 무중단 전환과 롤백을 주는, 소수 VM 전용 도구다
- 모니터링이 앱 자원을 실제로 잠식할 때 → **Grafana Cloud 무료 티어**로 외부 이관.
  메트릭 ~10k 시리즈 / 로그 ~50GB월 수준이고, **서버가 죽어도 알림이 살아 있다는 게 더 큰 이점**이다
