#!/usr/bin/env bash
# Spring·DB·Tunnel은 그대로 두고 Ubuntu 호스트에 정산어택 HTTPS 진입점만 설치한다.
set -euo pipefail

[[ "${EUID}" == 0 ]] || { echo "sudo 권한이 필요하다." >&2; exit 1; }
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
config_source="${repository_root}/infra/caddy/Caddyfile"
[[ -f "$config_source" && -d /home/ubuntu/jeongsan ]] || exit 1
exec 9>/home/ubuntu/jeongsan/.deploy.lock
flock -n 9 || { echo "다른 정산어택 배포가 진행 중이다." >&2; exit 1; }

source /etc/os-release
[[ "${ID}" == ubuntu && "${VERSION_ID}" == 24.04 ]] || { echo "Ubuntu 24.04용 절차다." >&2; exit 1; }
for service_name in nginx apache2; do
    if systemctl is-active --quiet "$service_name"; then
        echo "기존 ${service_name}가 실행 중이다. 포트 공유 방식을 먼저 검토한다." >&2
        exit 1
    fi
done
if [[ -f /etc/caddy/Caddyfile ]] && ! grep -q '^# jeongsan managed ingress$' /etc/caddy/Caddyfile; then
    echo "기존 Caddy 설정은 자동 교체하지 않는다." >&2
    exit 1
fi
if ss -H -ltnp '( sport = :80 or sport = :443 )' | grep -v '"caddy"' | grep -q .; then
    echo "80 또는 443을 다른 서비스가 사용한다." >&2
    exit 1
fi
curl --fail --silent --show-error --max-time 10 http://127.0.0.1:18080/actuator/health | grep -q '"UP"'

backup_directory="/var/backups/jeongsan-caddy/$(date -u +%Y%m%dT%H%M%SZ)-$$"
install -d -m 700 /var/backups/jeongsan-caddy "$backup_directory"
if [[ -f /etc/caddy/Caddyfile ]]; then
    cp --preserve=mode /etc/caddy/Caddyfile "${backup_directory}/Caddyfile"
fi

if ! command -v caddy >/dev/null; then
    # 공식 서명 저장소만 사용하며 다른 프로젝트 서비스를 자동 재시작하지 않는다.
    export DEBIAN_FRONTEND=noninteractive NEEDRESTART_MODE=l
    apt-get update
    apt-get install --yes debian-keyring debian-archive-keyring apt-transport-https curl gnupg
    curl --fail --silent --show-error --location --max-time 60 \
        https://dl.cloudsmith.io/public/caddy/stable/gpg.key -o "${backup_directory}/caddy-signing.asc"
    gpg --batch --yes --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg "${backup_directory}/caddy-signing.asc"
    curl --fail --silent --show-error --location --max-time 60 \
        https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt -o /etc/apt/sources.list.d/caddy-stable.list
    chmod 644 /usr/share/keyrings/caddy-stable-archive-keyring.gpg /etc/apt/sources.list.d/caddy-stable.list
    apt-get update
    apt-get install --yes caddy
    # 패키지의 기본 환영 페이지 대신 검증한 프로젝트 설정을 적용한다.
    systemctl stop caddy
fi

caddy validate --config "$config_source" --adapter caddyfile
install -d -m 755 /etc/caddy
install -m 644 "$config_source" /etc/caddy/Caddyfile
if systemctl is-active --quiet caddy; then
    if ! systemctl reload caddy; then
        cp --preserve=mode "${backup_directory}/Caddyfile" /etc/caddy/Caddyfile
        systemctl reload caddy
        echo "새 설정 적용 실패. 이전 Caddy 설정을 복원했다." >&2
        exit 1
    fi
else
    systemctl enable --now caddy
fi
systemctl is-active --quiet caddy
python3 "${repository_root}/scripts/ingress/allow-caddy-ports.py" "$backup_directory"
caddy version
echo "Caddy 실행·내부 방화벽 준비 완료. DNS·OCI 인바운드·공인 인증서 확인은 별도다."
echo "백업: ${backup_directory}"
