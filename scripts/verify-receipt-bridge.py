"""CI 전용 실제 MySQL·Spring·모의 OCR 경계를 검증한다. 실제 Python 모델 검증은 아니다."""
import base64
import hashlib
import hmac
import http.client
import json
import os
import subprocess
import sys
import time
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from concurrent.futures import ThreadPoolExecutor

assert os.environ.get("GITHUB_ACTIONS") == "true", "CI 전용이다."
sys.stdout.reconfigure(encoding="utf-8")
BASE = "http://127.0.0.1:18081/api/v1"
checks = 0
disabled = sys.argv[1:] == ["disabled"]
assert sys.argv[1:] in ([], ["disabled"])

def sql(query):
    result = subprocess.run(["docker", "exec", "jeongsan-v4-verification", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-plocal-v4-test", "-N", "-s", "jeongsan_auth_test", "-e", query], capture_output=True, text=True, encoding="utf-8")
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()

assert sql("SELECT DATABASE()") == "jeongsan_auth_test"

def check(value, label):
    global checks
    assert value, label
    checks += 1
    print(f"PASS OCR {checks}: {label}", flush=True)

def token(user, client):
    def encode(value): return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    value = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({"sub": str(user), "client": client,
        "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600}).encode())
    secret = b"01234567890123456789012345678901" if disabled else b"local-only-dev-secret-please-change-32b"
    return value + "." + encode(hmac.new(secret, value.encode(), hashlib.sha256).digest())

def call(method, path, user=None, body=None, expected=200, media="application/json", client="WEB"):
    headers = {"Content-Type": media}
    if user is not None:
        issued = token(user, client)
        headers["Authorization" if client == "APP" else "Cookie"] = "Bearer " + issued if client == "APP" else "jeongsan_token=" + issued
    payload = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
    try:
        with urlopen(Request(BASE + path, method=method, headers=headers, data=payload), timeout=20) as response:
            status, raw, returned = response.status, response.read(), response.headers
    except HTTPError as error:
        status, raw, returned = error.code, error.read(), error.headers
    value = json.loads(raw) if raw else None
    assert status == expected, (method, path, status, value)
    return value, returned

def upstream_calls():
    with urlopen("http://127.0.0.1:18082/calls", timeout=3) as response:
        return json.load(response)["calls"]

if disabled:
    fields = sql("""SELECT settlement_unit.gathering_id,settlement_unit.id,participant.user_id
        FROM settlement_units settlement_unit JOIN participants participant ON participant.id=settlement_unit.host_participant_id
        JOIN users actor ON actor.id=participant.user_id WHERE actor.provider='RECEIPT' AND settlement_unit.status='OPEN'
        ORDER BY settlement_unit.id DESC LIMIT 1""").split("\t")
    gathering_id, settlement_unit_id, user_id = map(int, fields)
    before_calls = upstream_calls()
    rejected, _ = call("POST", f"/gatherings/{gathering_id}/settlement-units/{settlement_unit_id}/receipts/extract",
        user_id, b"\xff\xd8\xff\x00", 503, "image/jpeg")
    check(rejected["code"] == "OCR_UNAVAILABLE" and upstream_calls() == before_calls, "prod에서 OCR 설정 없이 기동하고 OCR만 503")
    sys.exit(0)

users = []
for name in ("영수증총무", "다른총무", "외부사용자"):
    provider_id = str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('RECEIPT','{provider_id}','계약검증','{name}','FREE',UTC_TIMESTAMP())")
    users.append(int(sql(f"SELECT id FROM users WHERE provider_id='{provider_id}'")))
host, other, stranger = users
room, _ = call("POST", "/gatherings", host, expected=201)
gathering_id, settlement_unit_id = room["id"], room["settlementUnits"][0]["id"]
host_id = room["me"]["participantId"]
joined, _ = call("POST", "/join/" + room["shareToken"], other, {"settlementUnitId": settlement_unit_id})
other_id = joined["participantId"]
other_unit, _ = call("POST", f"/gatherings/{gathering_id}/settlement-units", other,
    {"requestId": str(uuid.uuid4()), "participantIds": [other_id, host_id]}, 201)
root = f"/gatherings/{gathering_id}/settlement-units/{settlement_unit_id}"
path = root + "/receipts/extract"
jpeg = b"\xff\xd8\xff\x00"

def snapshot():
    return sql(f"""SELECT input_revision,status FROM settlement_units WHERE id={settlement_unit_id};
        SELECT COUNT(*) FROM rounds WHERE settlement_unit_id={settlement_unit_id};
        SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={settlement_unit_id};
        SELECT COUNT(*) FROM timeline_entries WHERE gathering_id={gathering_id}""")

before = snapshot()
baseline = upstream_calls()
response, headers = call("POST", path, host, jpeg, media="image/jpeg")
check(response["schemaVersion"] == 2 and response["requiresConfirmation"] is True and headers["Cache-Control"] == "no-store", "WEB 총무가 schema v2 후보를 받음")
check(response["drinks"][0]["bottleCount"] is None and response["drinks"][0]["unit"] == "UNKNOWN", "미인식 병 수·단위를 보정하지 않음")
check(snapshot() == before, "추출로 revision·차수·정산·타임라인을 변경하지 않음")
check(upstream_calls() == baseline + 1, "내부 경로·서비스 키·이미지 매체로 모의 OCR 1회 호출")

denied = [(None, path, 401, "UNAUTHENTICATED"), (other, path, 403, "NOT_SETTLEMENT_UNIT_HOST"),
    (stranger, path, 403, "NOT_PARTICIPANT"),
    (host, f"/gatherings/{gathering_id + 100000}/settlement-units/{settlement_unit_id}/receipts/extract", 404, "SETTLEMENT_UNIT_NOT_FOUND"),
    (host, f"/gatherings/{gathering_id}/settlement-units/{other_unit['id']}/receipts/extract", 403, "NOT_SETTLEMENT_UNIT_HOST")]
for user, denied_path, expected, code in denied:
    rejected, _ = call("POST", denied_path, user, jpeg, expected, "image/jpeg")
    check(rejected["code"] == code, f"권한 범위 {code}")
call("POST", path, host, jpeg, 415, "application/pdf")
call("POST", path, None, b"malformed-multipart", 401, "multipart/form-data; boundary=missing")
call("POST", path, host, b"malformed-multipart", 415, "multipart/form-data; boundary=missing")
check(upstream_calls() == baseline + 1, "깨진 multipart도 선행 파싱 없이 401/415로 거절")
check(upstream_calls() == baseline + 1, "권한·매체 거절은 모델을 호출하지 않음")
connection = http.client.HTTPConnection("127.0.0.1", 18081, timeout=20)
try:
    # 큰 파일을 전송하기 전에 알려진 Content-Length만으로 거절되는지 확인한다.
    connection.putrequest("POST", "/api/v1" + path)
    connection.putheader("Content-Type", "image/jpeg")
    connection.putheader("Content-Length", str(8 * 1024 * 1024 + 1))
    connection.putheader("Cookie", "jeongsan_token=" + token(host, "WEB"))
    connection.putheader("Connection", "close")
    connection.endheaders()
    oversized = connection.getresponse()
    assert oversized.status == 413 and json.loads(oversized.read())["code"] == "IMAGE_TOO_LARGE"
finally:
    connection.close()
check(upstream_calls() == baseline + 1, "8MiB 초과 업로드는 모델 호출 전 거절")
sql(f"UPDATE settlement_units SET status='SETTLING' WHERE id={settlement_unit_id}")
closed, _ = call("POST", path, host, jpeg, 409, "image/jpeg")
check(closed["code"] == "SETTLEMENT_UNIT_NOT_OPEN" and upstream_calls() == baseline + 1, "정산 중 단위는 후보 추출 거절")
sql(f"UPDATE settlement_units SET status='OPEN' WHERE id={settlement_unit_id}")
call("POST", path, host, b"fake", 400, "image/jpeg")
response, _ = call("POST", path, host, jpeg, media="image/jpeg", client="APP")
check(response["requiresConfirmation"] is True, "APP Bearer 총무도 동일 후보 계약 사용")
for _ in range(7):
    call("POST", path, host, jpeg, media="image/jpeg", client="APP")
limited, headers = call("POST", path, host, jpeg, 429, "image/jpeg")
check(limited["code"] == "OCR_REQUEST_LIMIT" and 1 <= int(headers["Retry-After"]) <= 60, "실패 포함 열 번 후 429와 남은 대기 초")
check(upstream_calls() == baseline + 9 and snapshot() == before, "거절·잘못된 사진·재시도로 저장 부작용이 없음")

sql(f"DELETE FROM users WHERE id={stranger}")
removed, _ = call("POST", path, stranger, jpeg, 401, "image/jpeg", client="APP")
check(removed["code"] == "UNAUTHENTICATED", "삭제 계정의 기존 APP JWT는 모델 호출 전에 거절")
quick_before = sql(";".join(f"SELECT COUNT(*) FROM {table}" for table in
    ("users", "gatherings", "participants", "settlement_units", "rounds", "settlements", "timeline_entries")))
quick, quick_headers = call("POST", "/settlements/quick", other, {"total":10000, "headcount":3}, client="APP")
check(quick == {"total":10000, "headcount":3, "participantAmount":3334, "payerAmount":3332, "incomingTotal":6668}
    and quick_headers["Cache-Control"] == "no-store", "간단 정산의 총액은 core 1원 올림·결제자 잔액으로 보존")
invalid, _ = call("POST", "/settlements/quick", other, {"total":1, "headcount":3}, 422, client="APP")
check(invalid["errors"][0]["code"] == "NEGATIVE_ADJUSTED_AMOUNT", "간단 정산의 음수 잔액은422")
call("POST", "/settlements/quick", None, {"total":10000, "headcount":3}, 401)
call("POST", "/settlements/quick", stranger, {"total":10000, "headcount":3}, 401, client="APP")
check(sql(";".join(f"SELECT COUNT(*) FROM {table}" for table in
    ("users", "gatherings", "participants", "settlement_units", "rounds", "settlements", "timeline_entries"))) == quick_before,
    "간단 정산 성공·실패가 회원·술자리·명단·정산·타임라인을 만들지 않음")
simple_receipt, _ = call("POST", "/receipts/extract", other, jpeg, media="image/jpeg", client="APP")
check(simple_receipt["schemaVersion"] == 2 and simple_receipt["requiresConfirmation"] is True, "방 없는 OCR도 같은 후보만 반환")
call("POST", "/receipts/extract", None, jpeg, 401, "image/jpeg")
call("POST", "/receipts/extract", stranger, jpeg, 401, "image/jpeg", client="APP")
check(snapshot() == before, "간단 정산과 방 없는 OCR 이후 기존 단위의 입력과 결과가 그대로 유지")
other_path = f"/gatherings/{gathering_id}/settlement-units/{other_unit['id']}/receipts/extract"
before_delayed = upstream_calls()
with urlopen(Request("http://127.0.0.1:18082/control", method="POST", data=b'{"delaySeconds":2}',
                     headers={"Content-Type": "application/json"}), timeout=3):
    pass
with ThreadPoolExecutor(max_workers=1) as pool:
    future = pool.submit(call, "POST", other_path, other, jpeg, 409, "image/jpeg", "APP")
    for _ in range(50):
        if upstream_calls() > before_delayed:
            break
        time.sleep(0.02)
    assert upstream_calls() == before_delayed + 1, "지연 모델에 요청이 도달해야 한다"
    sql(f"SET innodb_lock_wait_timeout=1; UPDATE settlement_units SET status='SETTLING' WHERE id={other_unit['id']}")
    changed, _ = future.result(timeout=10)
check(changed["code"] == "SETTLEMENT_UNIT_NOT_OPEN", "추론 동안 단위를 잠그지 않고 확정 변경을 응답 전에 다시 확인")
check(snapshot() == before, "다른 단위의 추론 중 상태 변경도 기존 차수·정산·응답 수를 바꾸지 않음")
print(f"Receipt bridge verification: {checks} checks passed", flush=True)
