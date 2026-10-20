"""CI 전용 MySQL에서 Boot 3의 저장 데이터·토큰을 Boot 4가 이어 쓰는지 확인한다."""
import argparse
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import subprocess
import sys
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import uuid
import zipfile

BASE = "http://127.0.0.1:18081/api/v1"
DATABASE = "jeongsan_auth_test"
CONTAINER = "jeongsan-v4-verification"
ROOT = Path(__file__).resolve().parent.parent
STATE = ROOT / "server/build/boot-upgrade-state.json"
RESULT = ROOT / "server/build/boot-upgrade-result.json"
BASELINE_REF = "3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420"
checks = 0


def guard():
    run = os.environ.get("GITHUB_RUN_ID", "")
    if os.environ.get("GITHUB_ACTIONS") != "true" or not run.isdigit():
        raise SystemExit("GitHub Actions의 전용 업그레이드 검증만 허용한다.")
    result = subprocess.run(["docker", "inspect", CONTAINER], capture_output=True, text=True, check=True)
    owner = json.loads(result.stdout)[0]["Config"]["Labels"].get("jeongsan.ci")
    if owner != run:
        raise SystemExit("현재 CI가 만든 DB가 아니므로 실행하지 않는다.")


def sql(query):
    result = subprocess.run(["docker", "exec", CONTAINER, "mysql", "--default-character-set=utf8mb4",
                             "-uroot", "-plocal-v4-test", "-N", "-s", DATABASE, "-e", query],
                            capture_output=True, text=True, encoding="utf-8", check=True)
    return result.stdout.strip()


def check(value, label):
    global checks
    if not value:
        raise AssertionError(label)
    checks += 1
    print(f"PASS {checks}: {label}", flush=True)


def encode(value):
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode()


def token(user, client):
    body = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({
        "sub": str(user), "client": client, "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600,
    }).encode())
    signature = hmac.new(b"local-only-dev-secret-please-change-32b", body.encode(), hashlib.sha256).digest()
    return body + "." + encode(signature)


def call(method, path, signed=None, client="WEB", body=None, status=200):
    headers = {"Content-Type": "application/json"}
    if signed:
        if client == "WEB":
            headers["Cookie"] = "jeongsan_token=" + signed
        else:
            headers["Authorization"] = "Bearer " + signed
    request = Request(BASE + path, method=method, headers=headers,
                      data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=20) as response:
            actual, raw = response.status, response.read()
    except HTTPError as error:
        actual, raw = error.code, error.read()
    if actual != status:
        raise AssertionError(f"{method} {path}: expected {status}, got {actual}")
    return json.loads(raw) if raw else None


def boot_version(path):
    with zipfile.ZipFile(path) as jar:
        manifest = jar.read("META-INF/MANIFEST.MF").decode()
    return next(line.partition(": ")[2] for line in manifest.splitlines() if line.startswith("Spring-Boot-Version: "))


def changelog():
    return sql("SELECT ID,AUTHOR,FILENAME,MD5SUM,DATEEXECUTED,EXECTYPE FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED").splitlines()


def create_user(name):
    identity = str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,created_at) "
        f"VALUES('UPGRADE','{identity}','업그레이드검증','{name}',UTC_TIMESTAMP())")
    return int(sql(f"SELECT id FROM users WHERE provider='UPGRADE' AND provider_id='{identity}'"))


def issue_app_token(user):
    # 제공자 승인은 흉내 내지 않는다. 티켓 검증 이후 JWT 발급은 구버전 서버가 직접 한다.
    verifier = "v" * 43
    challenge = encode(hashlib.sha256(verifier.encode()).digest())
    ticket = encode(uuid.uuid4().bytes + uuid.uuid4().bytes)
    ticket_hash = hashlib.sha256(ticket.encode()).hexdigest()
    sql(f"INSERT INTO auth_tickets VALUES('{ticket_hash}',{user},'{challenge}',UTC_TIMESTAMP()+INTERVAL 50 SECOND)")
    return call("POST", "/auth/app/exchange", body={"ticket": ticket, "codeVerifier": verifier})["token"]


def room(host, guest):
    created = call("POST", "/gatherings", host, body={}, status=201)
    unit = created["settlementUnits"][0]["id"]
    root = f"/gatherings/{created['id']}/settlement-units/{unit}"
    call("POST", root + "/rounds", host,
         body={"total": 10000, "payerParticipantId": created["me"]["participantId"]}, status=201)
    call("POST", "/join/" + created["shareToken"], guest, body={"settlementUnitId": unit})
    return created["id"], root


def seed():
    baseline = boot_version(os.environ["BOOT3_BASELINE_JAR"])
    target = boot_version(ROOT / "server/build/libs/server.jar")
    check(baseline == "3.5.16" and target.startswith("4."), "확정 Boot 3.5.16 산출물과 Boot 4 산출물을 구분")
    host_id, guest_id = create_user("이전총무"), create_user("이전참여자")
    host, guest = token(host_id, "WEB"), token(guest_id, "WEB")
    app = issue_app_token(host_id)
    check(call("GET", "/auth/me", app, "APP")["id"] == host_id,
          "구버전 HTTP 티켓 교환이 실제 APP 토큰을 발급하고 검증")
    call("PUT", "/users/me/payout", host,
         body={"bank": "국민", "accountNo": "00123456-7890", "holder": "이전총무"})
    settled_id, settled_root = room(host, guest)
    preview = call("GET", settled_root + "/settlement/preview", host)
    call("POST", settled_root + "/settlement", host,
         body={key: preview[key] for key in ("inputRevision", "inputHash")})
    settled = call("GET", f"/gatherings/{settled_id}", host)
    check(len(settled["transfers"]) == 1 and settled["transfers"][0]["amount"] == 5000,
          "구버전 HTTP로 정산 명세·미입금 송금을 저장")
    open_id, open_root = room(host, guest)
    open_preview = call("GET", open_root + "/settlement/preview", host)
    history = changelog()
    check(bool(history) and all("\t9:" in item for item in history), "구버전 Liquibase 적용 이력·체크섬을 저장")
    state = dict(baseline_ref=BASELINE_REF, baseline_version=baseline, target_version=target,
                 host_id=host_id, host=host, guest=guest, app=app,
                 settled_id=settled_id, settled=settled, open_id=open_id, open_root=open_root,
                 open_preview=open_preview, me=call("GET", "/auth/me", host), changelog=history,
                 payout_hex=sql(f"SELECT HEX(payout_encrypted) FROM users WHERE id={host_id}"))
    STATE.write_text(json.dumps(state, ensure_ascii=False), encoding="utf-8")
    print("BOOT-UPGRADE: Boot 3 seed ready", flush=True)


def verify():
    state = json.loads(STATE.read_text(encoding="utf-8"))
    check(state["baseline_ref"] == BASELINE_REF and state["target_version"] == boot_version(ROOT / "server/build/libs/server.jar"),
          "다른 소스·산출물의 검증 상태를 재사용하지 않음")
    current_history = changelog()
    previous_history = state["changelog"]
    check(current_history[:len(previous_history)] == previous_history,
          "Boot 4 기동 뒤 기존 changeSet·체크섬·실행 시각 불변")
    added_history = [row.split("\t") for row in current_history[len(previous_history):]]
    check(len(added_history) == 1 and added_history[0][0:3] ==
          ["029-ugc-safety", "jeongsan", "db/changelog/019-ugc-safety.yaml"]
          and added_history[0][3].startswith("9:") and added_history[0][5] == "EXECUTED",
          "신고·차단 신규 changeSet 029만 정확히 한 번 추가 적용")
    check(sql(f"SELECT HEX(payout_encrypted) FROM users WHERE id={state['host_id']}") == state["payout_hex"],
          "구버전 계좌 암호문을 변경·재암호화하지 않음")
    me = call("GET", "/auth/me", state["host"])
    check(me == state["me"], "구버전 WEB 쿠키로 같은 계정·계좌·알림 수를 조회")
    check(me["payout"] == {"bank": "국민", "accountNo": "001234567890", "holder": "이전총무"},
          "저장 계좌의 선행 0·한글 예금주 유지")
    check(call("GET", "/auth/me", state["app"], "APP") == me, "구버전 APP Bearer를 새 서버가 검증")
    call("GET", "/auth/me", state["host"], "APP", status=401)
    call("GET", "/auth/me", state["app"], "WEB", status=401)
    check(True, "업그레이드 뒤에도 APP/WEB 토큰 교차 사용은 401")
    detail = call("GET", f"/gatherings/{state['settled_id']}", state["host"])
    check(detail == state["settled"], "기존 정산 단위·송금 근거·명단·날짜·타임라인 JSON 보존")
    preview = call("GET", state["open_root"] + "/settlement/preview", state["host"])
    check(preview == state["open_preview"], "정산 전 입력의 revision·hash·금액이 양쪽 버전에서 같음")
    updated = call("POST", state["open_root"] + "/settlement", state["host"],
                   body={key: state["open_preview"][key] for key in ("inputRevision", "inputHash")})
    check(updated["status"] == "SETTLING" and sum(t["amount"] for t in updated["transfers"]) == 5000,
          "구버전에서 받은 미리보기로 새 서버에서 수동 정산 가능")
    transfer = detail["transfers"][0]["id"]
    call("POST", f"/transfers/{transfer}/sent", state["guest"], status=204)
    call("POST", f"/transfers/{transfer}/confirm", state["host"], status=204)
    completed = call("GET", f"/gatherings/{state['settled_id']}", state["host"])
    check(completed["status"] == "COMPLETED" and completed["transfers"][0]["status"] == "CONFIRMED",
          "업그레이드 전 송금을 새 서버에서 보냄·입금 확인·완료")
    call("POST", "/auth/logout", state["app"], "APP", status=204)
    call("GET", "/auth/me", state["app"], "APP", status=401)
    call("GET", "/auth/me", state["host"])
    check(True, "구버전 APP 토큰 로그아웃은 재사용을 막고 WEB 로그인은 유지")
    upgrade_checks = checks
    print(f"BOOT-UPGRADE: {upgrade_checks} checks passed", flush=True)

    # 앱의 '나 혼자 시작하기'가 목 모드와 API 모드에서 다른 인원을 보내던 계약 차이다.
    guest_id = call("GET", "/auth/me", state["guest"])["id"]
    guest_pid = next(p["id"] for p in detail["participants"] if p["userId"] == guest_id)
    path = f"/gatherings/{state['settled_id']}/settlement-units"
    payload = {"requestId": str(uuid.uuid4()), "participantIds": [guest_pid], "headcount": 1}
    count_before = sql(f"SELECT COUNT(*) FROM settlement_units WHERE gathering_id={state['settled_id']}")
    rejected = call("POST", path, state["guest"], body=payload, status=400)
    check(rejected["code"] == "VALIDATION_FAILED" and
          sql(f"SELECT COUNT(*) FROM settlement_units WHERE gathering_id={state['settled_id']}") == count_before,
          "앱의 본인 단독 headcount=1 요청은 400이며 단위를 저장하지 않음")
    payload["headcount"] = 2
    created = call("POST", path, state["guest"], body=payload, status=201)
    refreshed = call("GET", f"/gatherings/{state['settled_id']}", state["guest"])
    unit = next(u for u in refreshed["settlementUnits"] if u["id"] == created["id"])
    check(unit["participantIds"] == [guest_pid] and unit["headcount"] == 2 and unit["status"] == "OPEN",
          "본인만 명단에 두고 목표 인원 2로 보내면 201로 새 단위를 만듦")
    app_checks = checks - upgrade_checks
    print(f"APP-CONNECTION: {app_checks} checks passed", flush=True)
    result = dict(baseline_ref=BASELINE_REF, baseline_version=state["baseline_version"],
                  target_version=state["target_version"], upgrade_checks=upgrade_checks,
                  app_connection_checks=app_checks, checks=checks, status="passed",
                  scope="isolated CI MySQL; historical Boot 3 HTTP data; not production database backup")
    RESULT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("seed", "verify"))
    phase = parser.parse_args().phase
    guard()
    if sql("SELECT DATABASE()") != DATABASE:
        raise SystemExit("전용 검증 DB가 아니다.")
    seed() if phase == "seed" else verify()
