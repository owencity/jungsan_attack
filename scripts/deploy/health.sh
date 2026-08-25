#!/usr/bin/env bash
# 앱이 실제로 응답할 때까지 기다린다. 실패하면 로그를 남기고 0 이 아닌 값으로 끝난다.
#
#   bash scripts/deploy/health.sh          기본 30회 × 5초 = 최대 150초
#   TRIES=60 bash scripts/deploy/health.sh
#
# "컨테이너가 떴다"와 "서비스가 응답한다"는 다르다. compose up 이 성공해도
# Liquibase 마이그레이션 중이거나 DB 연결에 실패해 죽는 중일 수 있다 —
# 그래서 헬스 엔드포인트가 UP 을 줄 때까지 확인한다.

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

TRIES="${TRIES:-30}"
INTERVAL="${INTERVAL:-5}"
URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"

log "헬스체크: $URL (최대 $((TRIES * INTERVAL))초)"

for i in $(seq 1 "$TRIES"); do
  if curl -fsS --max-time 3 "$URL" 2>/dev/null | grep -q '"status":"UP"'; then
    ok "헬스체크 통과 (${i}회차, $((i * INTERVAL))초)"
    exit 0
  fi
  sleep "$INTERVAL"
done

echo ""
warn "헬스체크 실패 — 컨테이너 상태:"
compose ps || true
echo ""
warn "app 최근 로그 60줄:"
compose logs --tail 60 app || true
die "앱이 ${TRIES}회 시도 동안 UP 을 반환하지 않았다"
