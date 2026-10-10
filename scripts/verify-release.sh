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

# 운영 프로필에서 Apple만 비어 있어도 DB·카카오·기존 인증 가드는 정상이어야 한다.
kill "$server_pid"
wait "$server_pid" 2>/dev/null || true
server_pid=''
docker exec "$container" mysql -uroot -plocal-v4-test -e 'FLUSH PRIVILEGES' >/dev/null 2>&1
env SPRING_PROFILES_ACTIVE=prod DB_HOST=127.0.0.1 DB_PORT=13306 DB_NAME=jeongsan_auth_test DB_USER=root \
  DB_PASSWORD=local-v4-test JWT_SECRET=01234567890123456789012345678901 \
  PAYOUT_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= \
  AUTH_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= \
  KAKAO_CLIENT_ID=ci-kakao-client KAKAO_CLIENT_SECRET=ci-kakao-secret \
  KAKAO_REDIRECT_URI=https://api.test/api/v1/auth/kakao/callback \
  APPLE_CLIENT_ID='' APPLE_TEAM_ID='' APPLE_KEY_ID='' APPLE_PRIVATE_KEY_BASE64='' \
  APPLE_REDIRECT_URI=https://api.test/api/v1/auth/apple/callback \
  FRONTEND_ORIGIN=https://web.test LOGIN_SUCCESS_URL=https://web.test/jungsan \
  java -jar server/build/libs/server.jar --server.port=18081 > server/build/release-prod-http.log 2>&1 &
server_pid=$!
ready=false
for attempt in $(seq 1 40); do
  if curl -fsS http://127.0.0.1:18081/actuator/health | grep -q '"status":"UP"'; then ready=true; break; fi
  kill -0 "$server_pid" 2>/dev/null || break
  sleep 2
done
[ "$ready" = true ] || { echo 'Apple 미설정 운영 프로필 기동 실패' >&2; tail -n 45 server/build/release-prod-http.log; exit 1; }
python3 scripts/verify-provider-availability.py
