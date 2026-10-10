#!/usr/bin/env bash
# 배포는 성공한 SHA를 기록하며 실패하면 이전 앱 이미지를 복원한다. DB는 되돌리지 않는다.
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
cd "$DEPLOY_ROOT"
require_file "${DEPLOY_ROOT}/.env" "필수 운영 인증 설정을 먼저 준비한다."
require_file "$COMPOSE_FILE"
exec 9> "${DEPLOY_ROOT}/.deploy.lock"
flock -n 9 || die "다른 정산어택 배포가 진행 중이다."

previous_image="${APP_IMAGE:-}"
image_tar="${1:-}"
target_image="${2:-${APP_IMAGE:-}}"
[[ "$target_image" =~ ^jeongsan-server:[a-f0-9]{40}$ ]] || die "main 커밋 SHA 이미지 태그가 필요하다."
export APP_IMAGE="$target_image"
compose config --quiet

if [ -n "$image_tar" ]; then
  require_file "$image_tar"
  resolved_tar="$(realpath "$image_tar")"
  [[ "$resolved_tar" == "$DEPLOY_ROOT/"* ]] || die "이미지 tar는 정산어택 배포 디렉터리에 있어야 한다."
  docker load < "$resolved_tar"
fi
docker image inspect "$APP_IMAGE" >/dev/null

# 기존 서비스의 컨테이너·이미지·볼륨을 정리하지 않는다. 실패 복원용 이전 이미지도 보존한다.
if compose up -d && bash "${DEPLOY_ROOT}/scripts/deploy/health.sh"; then
  printf 'APP_IMAGE=%s\n' "$APP_IMAGE" > "${DEPLOY_ROOT}/.release.env.tmp"
  mv "${DEPLOY_ROOT}/.release.env.tmp" "${DEPLOY_ROOT}/.release.env"
  [ -z "$image_tar" ] || rm -f -- "$resolved_tar"
  ok "운영 이미지 확인 완료: $APP_IMAGE"
else
  if [[ "$previous_image" =~ ^jeongsan-server:[a-f0-9]{40}$ ]] && [ "$previous_image" != "$target_image" ]; then
    export APP_IMAGE="$previous_image"
    warn "이전 앱 이미지 복원: $previous_image (DB는 유지)"
    compose up -d app && bash "${DEPLOY_ROOT}/scripts/deploy/health.sh" || die "이전 앱 복원도 실패했다. 수동 확인이 필요하다."
  fi
  die "새 이미지 배포 실패. 성공 이력은 갱신하지 않았다."
fi
