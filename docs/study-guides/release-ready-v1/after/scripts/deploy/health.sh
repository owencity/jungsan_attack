#!/usr/bin/env bash
# 다른 앱의 8080 응답을 정산어택 성공으로 판정하지 않는다.
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
TRIES="${TRIES:-30}"
INTERVAL="${INTERVAL:-5}"
URL="http://127.0.0.1:18080/actuator/health"

for attempt in $(seq 1 "$TRIES"); do
  container_id="$(compose ps -q app)"
  if [ -n "$container_id" ]; then
    image="$(docker inspect --format '{{.Config.Image}}' "$container_id")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$container_id")"
    binding="$(compose port app 8080 2>/dev/null || true)"
    if [ "$image" = "${APP_IMAGE:-}" ] && [ "$health" = healthy ] && [ "$binding" = '127.0.0.1:18080' ] &&
      curl -fsS --max-time 3 "$URL" 2>/dev/null | grep -q '"status":"UP"'; then
      ok "정산어택 이미지·컨테이너·18080 HTTP 확인 ($attempt 회차)"
      exit 0
    fi
  fi
  sleep "$INTERVAL"
done
compose ps || true
# 제공자·사용자 값이 포함될 수 있는 운영 로그는 CI에 자동 출력하지 않는다.
die "정산어택이 $TRIES 회 시도 동안 정상 응답하지 않았다. 서버에서 앱 로그를 확인한다."
