"""CI 전용 MySQL/18081에서 내부 알림 대상·문구·동시 재시도를 검증한다. 운영 주소를 받지 않는다."""
import base64
import hashlib
import hmac
import json
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.request import Request, urlopen

sys.stdout.reconfigure(encoding="utf-8")
BASE = "http://127.0.0.1:18081/api/v1"
DATABASE = "jeongsan_auth_test"
checks = 0

def sql(query):
    result = subprocess.run(["docker", "exec", "jeongsan-v4-verification", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-plocal-v4-test", "-N", "-s", DATABASE, "-e", query], capture_output=True, text=True, encoding="utf-8")
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()

assert sql("SELECT DATABASE()") == DATABASE

def check(value, label):
    global checks
    assert value, label
    checks += 1
    print(f"PASS {checks}: {label}", flush=True)

def call(method, path, user=None, body=None, expected=200):
    def encode(value): return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    headers = {"Content-Type": "application/json"}
    if user is not None:
        token = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({"sub": str(user), "client": "WEB", "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600}).encode())
        headers["Cookie"] = "jeongsan_token=" + token + "." + encode(hmac.new(b"local-only-dev-secret-please-change-32b", token.encode(), hashlib.sha256).digest())
    request = Request(BASE + path, headers=headers, method=method, data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=20) as response: status, raw = response.status, response.read()
    except HTTPError as error: status, raw = error.code, error.read()
    value = json.loads(raw) if raw else None
    assert status == expected, (method, path, status, value)
    return value

users = []
for name in ("알림총무", "알림결제자", "알림송금자"):
    key = str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('NOTIF_TEST','{key}','알림검증','{name}','FREE',UTC_TIMESTAMP())")
    users.append(int(sql(f"SELECT id FROM users WHERE provider_id='{key}'")))
host, payer, sender = users
room = call("POST", "/gatherings", host, expected=201)
gathering_id = room["id"]
unit_id = room["settlementUnits"][0]["id"]
host_id = room["me"]["participantId"]
root = f"/gatherings/{gathering_id}/settlement-units/{unit_id}"
share = room["shareToken"]
def join(user):
    return call("POST", "/join/" + share, user, {"settlementUnitId": unit_id})["participantId"]
payer_id, sender_id = join(payer), join(sender)
other_unit = call("POST", f"/gatherings/{gathering_id}/settlement-units", host,
    {"requestId": str(uuid.uuid4()), "participantIds": [host_id, sender_id]}, 201)
other_root = f"/gatherings/{gathering_id}/settlement-units/{other_unit['id']}"
for _ in range(2):
    call("POST", root + "/rounds", host, {"total": 30000, "payerParticipantId": payer_id}, 201)
preview = call("GET", root + "/settlement/preview", host)
settled = call("POST", root + "/settlement", host, {"inputRevision": preview["inputRevision"], "inputHash": preview["inputHash"]})
def notifications(user, kind=None, settlement_unit_id=None):
    if settlement_unit_id is None:
        settlement_unit_id = unit_id
    rows = call("GET", "/me/notifications", user)
    return [row for row in rows if row["settlementUnitId"] == settlement_unit_id and (kind is None or row["type"] == kind)]
check(not notifications(host, "SETTLED") and len(notifications(host, "SETTLED_HOST")) == 1,
    "행동한 총무에게 참여자 금액 알림 대신 서버 계산 완료 1건")
check(len(notifications(payer, "SETTLED")) == 1 and notifications(payer, "SETTLED")[0]["body"] == "담당 차수의 정산금액을 확인해주세요",
    "송금할 금액이 없는 수취인도 정산 공개 알림 수신")
check(len(notifications(sender, "SETTLED")) == 1 and notifications(sender, "SETTLED")[0]["body"] == "알림결제자님께 20,000원 보내주세요",
    "송금자는 수취인 실명·천 단위 금액으로 정산 공개 알림 수신")
check(len(notifications(payer, "AUTO_RESPONDED")) == 1 and len(notifications(sender, "AUTO_RESPONDED")) == 1,
    "두 차수 자동 응답이어도 사용자별 알림 1건")
check(len(notifications(payer, "PAYOUT_MISSING")) == 1 and not notifications(sender, "PAYOUT_MISSING"),
    "계좌 없는 실제 수취인만 등록 요청 1건, 정산은 SETTLING 유지")
check(next(unit for unit in settled["settlementUnits"] if unit["id"] == unit_id)["status"] == "SETTLING",
    "계좌 미등록 때문에 정산을 중단하지 않음")
account = {"bank": "카카오뱅크", "accountNo": "1234-567890", "holder": "다른예금주"}
with ThreadPoolExecutor(2) as pool:
    list(pool.map(lambda _: call("PUT", "/users/me/payout", payer, account), range(2)))
check(len(notifications(sender, "PAYOUT_REGISTERED")) == 1 and len(notifications(host, "PAYOUT_REGISTERED")) == 1,
    "계좌 최초 등록 동시 재시도는 미확인 송금자별 1건")
check(notifications(sender, "PAYOUT_REGISTERED")[0]["body"] == "알림결제자님이 계좌를 등록했어요. 이제 보낼 수 있어요",
    "계좌 등록 알림은 예금주·계좌번호 대신 사용자 실명")
call("PUT", "/users/me/payout", payer, {**account, "accountNo": "9999-567890"})
check(len(notifications(sender, "PAYOUT_REGISTERED")) == 1 and not notifications(payer, "PAYOUT_REGISTERED"),
    "계좌 변경은 최초 등록 알림을 재생성하지 않고 행동자 제외")
transfer = next(row for row in settled["transfers"] if row["settlementUnitId"] == unit_id and row["fromParticipantId"] == sender_id)
transfer_path = f"/transfers/{transfer['id']}"
with ThreadPoolExecutor(2) as pool:
    list(pool.map(lambda _: call("POST", transfer_path + "/sent", sender, expected=204), range(2)))
check(len(notifications(payer, "SENT")) == 1 and notifications(payer, "SENT")[0]["body"] == "알림송금자님이 보냈대요. 입금을 확인해주세요",
    "동시 보냈어요는 수취인에게 이름 포함 알림 1건")
call("POST", transfer_path + "/not-received", payer, expected=204)
call("POST", transfer_path + "/not-received", payer, expected=204)
check(len(notifications(sender, "NOT_RECEIVED")) == 1 and notifications(sender, "NOT_RECEIVED")[0]["body"] == "알림결제자님이 아직 입금을 확인 못 했대요",
    "아직 안 들어왔어요 재시도는 송금자에게 이름 포함 알림 1건")
call("POST", transfer_path + "/confirm", payer, expected=204)
call("POST", transfer_path + "/confirm", payer, expected=204)
check(len(notifications(sender, "CONFIRMED")) == 1 and notifications(sender, "CONFIRMED")[0]["body"] == "알림결제자님이 입금을 확인했어요",
    "확인 재시도는 송금자에게 이름 포함 알림 1건")
remaining = next(row for row in settled["transfers"] if row["settlementUnitId"] == unit_id and row["fromParticipantId"] == host_id)
call("POST", f"/transfers/{remaining['id']}/confirm", payer, expected=204)
check(notifications(sender, "COMPLETED")[0]["body"] == "담당 차수의 정산이 완료됐어요" and not notifications(payer, "COMPLETED"),
    "다른 단위가 OPEN이면 7일 삭제를 예고하지 않고 마지막 확인자 제외")
call("POST", other_root + "/rounds", host, {"total": 10000, "payerParticipantId": host_id}, 201)
preview = call("GET", other_root + "/settlement/preview", host)
other_settled = call("POST", other_root + "/settlement", host, {"inputRevision": preview["inputRevision"], "inputHash": preview["inputHash"]})
other_transfer = next(row for row in other_settled["transfers"] if row["settlementUnitId"] == other_unit["id"])
call("POST", f"/transfers/{other_transfer['id']}/confirm", host, expected=204)
check(notifications(sender, "COMPLETED", other_unit["id"])[0]["body"] == "정산 완료! 7일 뒤 사라져요",
    "모든 단위가 끝나면 7일 삭제 예고")
call("POST", "/me/notifications/read-all", sender, expected=204)
check(call("GET", "/auth/me", sender)["unreadNotificationCount"] == 0, "알림 읽음과 미읽은 수 일치")

# 회원 신원을 공유한 다른 단위의 접근까지 제거하지 않는다.
room = call("POST", "/gatherings", host, expected=201)
gathering_id, share = room["id"], room["shareToken"]
unit_id = room["settlementUnits"][0]["id"]
root = f"/gatherings/{gathering_id}/settlement-units/{unit_id}"
payer_id = join(payer)
other_unit = call("POST", f"/gatherings/{gathering_id}/settlement-units", payer,
    {"requestId": str(uuid.uuid4()), "participantIds": [payer_id, room["me"]["participantId"]]}, 201)
call("DELETE", root + f"/participants/{payer_id}", host, expected=204)
check(len(notifications(payer, "REMOVED")) == 1 and notifications(payer, "REMOVED")[0]["body"] == room["title"] + "의 담당 정산에서 빠졌어요",
    "내보내짐 알림은 제외된 단위 범위를 명시")
remaining_room = call("GET", f"/gatherings/{gathering_id}", payer)
check(payer_id in next(unit for unit in remaining_room["settlementUnits"] if unit["id"] == other_unit["id"])["participantIds"],
    "내보내져도 다른 단위의 참여·접근 유지")
check(not notifications(host, "REMOVED"), "내보낸 총무 본인에게는 알림 없음")
print(f"NOTIFICATION/RETRY: {checks} checks passed")
