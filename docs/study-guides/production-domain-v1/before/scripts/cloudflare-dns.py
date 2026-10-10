"""api.devkdk.com 한 레코드만 기존 OCI Tunnel에 맞춘다. 토큰·응답 원문은 로그에 쓰지 않는다."""
import json
import os
import re
import sys
import uuid
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

HOSTNAME = "api.devkdk.com"
ZONE_NAME = "devkdk.com"
API_BASE = "https://api.cloudflare.com/client/v4"


class DnsError(RuntimeError):
    pass


class Cloudflare:
    def __init__(self, token):
        if not token:
            raise DnsError("GitHub Secret CLOUDFLARE_API_TOKEN을 준비하세요.")
        self.token = token

    def request(self, method, path, body=None):
        request = Request(API_BASE + path, method=method,
                          headers={"Authorization": "Bearer " + self.token, "Content-Type": "application/json"},
                          data=json.dumps(body).encode() if body is not None else None)
        try:
            with urlopen(request, timeout=20) as response:
                result = json.load(response)
        except HTTPError as error:
            raise DnsError(f"Cloudflare HTTP {error.code}: 토큰의 devkdk.com DNS 권한·Zone ID를 확인하세요.") from None
        except (URLError, TimeoutError, ValueError):
            raise DnsError("Cloudflare 응답을 확인하지 못했습니다. 원문·토큰은 출력하지 않습니다.") from None
        if result.get("success") is not True:
            raise DnsError("Cloudflare가 요청을 거절했습니다. devkdk.com DNS 권한을 확인하세요.")
        return result["result"]


def sync(client, tunnel_id, zone_id=""):
    # 다른 도메인/서버로 동작 범위를 넓히지 않는다. UUID와 Zone ID는 요청 경로 생성 전에 검증한다.
    try:
        target = str(uuid.UUID(tunnel_id)) + ".cfargotunnel.com"
    except ValueError:
        raise DnsError("CLOUDFLARE_TUNNEL_ID에는 OCI의 기존 Tunnel UUID가 필요합니다.") from None
    if zone_id:
        if not re.fullmatch(r"[0-9a-f]{32}", zone_id):
            raise DnsError("CLOUDFLARE_ZONE_ID 형식이 잘못됐습니다.")
    else:
        zones = client.request("GET", "/zones?" + urlencode({"name": ZONE_NAME, "status": "active"}))
        if len(zones) != 1 or zones[0].get("name") != ZONE_NAME:
            raise DnsError("devkdk.com Zone 하나를 확인해야 합니다. DNS 전용 토큰은 CLOUDFLARE_ZONE_ID 변수를 지정하세요.")
        zone_id = zones[0]["id"]
        if not re.fullmatch(r"[0-9a-f]{32}", zone_id):
            raise DnsError("Cloudflare Zone ID 응답이 잘못됐습니다.")
    # 지정한 Zone ID가 다른 도메인이어도 api.devkdk.com 외 이름은 생성/변경하지 않는다.
    path = f"/zones/{zone_id}/dns_records"
    query = "?" + urlencode({"name": HOSTNAME, "per_page": 100})
    records = client.request("GET", path + query)
    if len(records) > 1 or any(record.get("name") != HOSTNAME for record in records):
        raise DnsError("api.devkdk.com 레코드가 중복되거나 범위를 벗어났습니다. 자동 변경을 중단합니다.")
    desired = {"type": "CNAME", "name": HOSTNAME, "content": target, "proxied": True, "ttl": 1}
    action = "unchanged"
    if not records:
        client.request("POST", path, desired)
        action = "created"
    else:
        record = records[0]
        if record.get("type") != "CNAME":
            raise DnsError("기존 api 레코드가 CNAME이 아닙니다. 기존 A/AAAA 레코드는 임의 교체하지 않습니다.")
        if record.get("content", "").rstrip(".").lower() != target or record.get("proxied") is not True:
            if not re.fullmatch(r"[0-9a-f]{32}", record.get("id", "")):
                raise DnsError("Cloudflare 레코드 ID가 잘못됐습니다.")
            client.request("PATCH", path + "/" + record["id"], desired)
            action = "updated"
    actual = client.request("GET", path + query)
    if len(actual) != 1 or any(actual[0].get(key) != value for key, value in desired.items() if key != "ttl"):
        raise DnsError("DNS 변경 뒤 실제 레코드가 OCI Tunnel 대상과 일치하지 않습니다.")
    return action


if __name__ == "__main__":
    try:
        result = sync(Cloudflare(os.environ.get("CLOUDFLARE_API_TOKEN", "")),
                      os.environ.get("CLOUDFLARE_TUNNEL_ID", ""), os.environ.get("CLOUDFLARE_ZONE_ID", ""))
        print(f"api.devkdk.com DNS {result}; Tunnel 원본 포트와 실제 로그인은 별도 확인")
    except DnsError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
