#!/usr/bin/env bash
# CI가 생성한 전용 MySQL과 로컬 HTTP 서버만 사용한다. 운영·개발 중 컨테이너를 교체하지 않는다.
set -euo pipefail
[ "${GITHUB_ACTIONS:-}" = true ] || { echo 'GitHub Actions 전용 실행이다.' >&2; exit 1; }
container=jeongsan-v4-verification
if docker inspect "$container" >/dev/null 2>&1; then
  echo '동일 이름의 기존 DB가 있어 실행하지 않는다.' >&2
  exit 1
fi
server_pid=''
cleanup() {
  [ -z "$server_pid" ] || kill "$server_pid" 2>/dev/null || true
  if [ "$(docker inspect --format '{{index .Config.Labels "jeongsan.ci"}}' "$container" 2>/dev/null)" = "$GITHUB_RUN_ID" ]; then
    docker rm -f "$container" >/dev/null
  fi
}
trap cleanup EXIT
docker run -d --name "$container" --label "jeongsan.ci=$GITHUB_RUN_ID" -p 127.0.0.1:13306:3306 \
  -e MYSQL_ROOT_PASSWORD=local-v4-test -e MYSQL_DATABASE=jeongsan_auth_test mysql:8.4 >/dev/null
ready=false
for attempt in $(seq 1 40); do
  if docker exec "$container" mysql -uroot -plocal-v4-test -e 'SELECT 1' jeongsan_auth_test >/dev/null 2>&1; then ready=true; break; fi
  sleep 2
done
[ "$ready" = true ] || { echo 'CI MySQL 준비 실패' >&2; exit 1; }
mkdir -p server/build
java -Duser.timezone=Asia/Seoul -jar server/build/libs/server.jar --server.port=18081 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:13306/jeongsan_auth_test?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
  --spring.datasource.password=local-v4-test > server/build/release-http.log 2>&1 &
server_pid=$!
ready=false
for attempt in $(seq 1 40); do
  if curl -fsS http://127.0.0.1:18081/actuator/health | grep -q '"status":"UP"'; then ready=true; break; fi
  kill -0 "$server_pid" 2>/dev/null || break
  sleep 2
done
[ "$ready" = true ] || { echo 'CI HTTP 서버 준비 실패' >&2; exit 1; }
python3 scripts/verify-auto-settlement.py
python3 scripts/verify-auth-release.py
./gradlew :server:authDatabaseProbe --no-daemon
python3 scripts/verify-deploy.py
