#!/usr/bin/env bash
# 배포 스크립트 공통 함수. 각 스크립트가 맨 위에서 source 한다.
#
# 왜 따로 두나 — 스택이 늘면(nginx·loki·prometheus·grafana·redis·kafka...)
# 스크립트도 같이 늘어난다. 로깅·에러처리를 매번 복붙하면 그때부터 손댈 수 없게 된다.

set -euo pipefail

# CI 로그에서도 사람 눈으로도 읽히게. 터미널이 아니면(파이프·CI) 색을 끈다.
if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_INFO=$'\033[36m'; C_OK=$'\033[32m'
  C_WARN=$'\033[33m'; C_ERR=$'\033[31m'
else
  C_RESET=""; C_INFO=""; C_OK=""; C_WARN=""; C_ERR=""
fi

log()  { echo "${C_INFO}▶${C_RESET} $*"; }
ok()   { echo "${C_OK}✅${C_RESET} $*"; }
warn() { echo "${C_WARN}⚠️${C_RESET}  $*"; }

# 실패는 조용히 넘어가지 않는다. 어디서 죽었는지 남기고 즉시 멈춘다.
die()  { echo "${C_ERR}❌${C_RESET} $*" >&2; exit 1; }

# 배포 루트. 스크립트가 어디서 실행되든 같은 곳을 가리키게 한다 —
# CI 가 부르든 사람이 SSH 로 들어와 부르든 동작이 같아야 한다.
DEPLOY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${DEPLOY_ROOT}/docker-compose.prod.yml"

# compose 명령을 한 곳에서 만든다. 파일이 늘어나면(-f monitoring.yml 등) 여기만 고친다.
compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

require_file() {
  [ -f "$1" ] || die "$1 이(가) 없다. ${2:-}"
}
