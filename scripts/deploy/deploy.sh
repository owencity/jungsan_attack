#!/usr/bin/env bash
# 서버에서 실행하는 배포 스크립트.
#
#   CI 에서:  ssh ... "cd ~/jeongsan && bash scripts/deploy/deploy.sh"
#   손으로:   ssh ubuntu@서버 && cd ~/jeongsan && bash scripts/deploy/deploy.sh
#
# **둘의 동작이 같아야 한다.** 배포가 깨졌을 때 CI 로그만 보고 추측하는 대신
# 서버에서 같은 명령을 그대로 돌려볼 수 있어야 원인이 빨리 잡힌다.
#
# 인자로 이미지 tar 경로를 받는다. 없으면 이미 로드된 이미지로 재시작만 한다
# (설정만 바꾸고 다시 띄우는 경우).

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

IMAGE_TAR="${1:-}"

cd "$DEPLOY_ROOT"

# .env 가 없으면 여기서 멈춘다. 빈 값으로 뜨면 카카오 로그인이 조용히 깨져
# "왜 안 되지"를 한참 헤매게 된다 — 차라리 배포를 실패시킨다.
require_file "${DEPLOY_ROOT}/.env" "docs/DEPLOY.md 의 '서버에 .env 만들기' 참조."
require_file "$COMPOSE_FILE"

if [ -n "$IMAGE_TAR" ]; then
  require_file "$IMAGE_TAR"
  log "이미지 로드: $IMAGE_TAR"
  docker load < "$IMAGE_TAR"
  rm -f "$IMAGE_TAR"
else
  warn "이미지 tar 인자가 없다 — 이미 로드된 이미지로 재시작만 한다"
fi

log "컨테이너 기동"
# --remove-orphans: compose 에서 서비스를 뺐을 때 남은 컨테이너를 정리한다.
compose up -d --remove-orphans

log "태그 없는 이전 이미지 정리"
# 배포할 때마다 :latest 가 옮겨가면서 이전 이미지가 dangling 으로 남는다.
# 안 지우면 디스크가 금방 찬다(이미지 하나가 ~600MB).
docker image prune -f

ok "배포 완료"
compose ps
