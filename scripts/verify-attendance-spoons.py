"""FC-031: 폐기 가능한 CI MySQL/HTTP에서 보상 중복·경합·롤백을 검증한다."""
import base64
import hashlib
import hmac
import json
import subprocess
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = "http://127.0.0.1:18081/api/v1"
checks = 0


def sql(query):
    result = subprocess.run(["docker", "exec", "jeongsan-v4-verification", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-plocal-v4-test", "-N", "-s", "jeongsan_auth_test", "-e", query],
        capture_output=True, text=True, encoding="utf-8", timeout=20)
    assert result.returncode == 0, result.stderr
    return result.stdout.strip()


assert sql("SELECT DATABASE()") == "jeongsan_auth_test"


def check(condition, label):
    global checks
    assert condition, label
    checks += 1
    print(f"PASS {checks}: {label}", flush=True)


def call(method, path, cookie=None, body=None, expected=200):
    headers = {"Content-Type": "application/json", "Origin": "http://localhost:5173"}
    if cookie:
        headers["Cookie"] = cookie
    request = Request(BASE + path, method=method, headers=headers,
        data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=30) as response:
            status, raw, response_headers = response.status, response.read(), response.headers
    except HTTPError as error:
        status, raw, response_headers = error.code, error.read(), error.headers
    assert status == expected, (method, path, status, raw)
    return json.loads(raw) if raw else None, response_headers


def member_cookie(user_id):
    def encode(value):
        return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    signed = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({"sub": str(user_id), "client": "WEB",
        "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600}).encode())
    return "jeongsan_token=" + signed + "." + encode(hmac.new(b"local-only-dev-secret-please-change-32b", signed.encode(), hashlib.sha256).digest())


def session():
    parsed = SimpleCookie()
    parsed.load(call("POST", "/guest/session", expected=204)[1]["Set-Cookie"])
    return "jeongsan_guest=" + parsed["jeongsan_guest"].value


def make_user():
    provider_id = str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('SPOON_TEST','{provider_id}','검증','검증총무','FREE',UTC_TIMESTAMP())")
    return int(sql(f"SELECT id FROM users WHERE provider_id='{provider_id}'"))


host_id = make_user()
host_cookie = member_cookie(host_id)


def balance():
    return int(sql(f"SELECT spoon_count FROM users WHERE id={host_id}"))


def create(cookie=host_cookie):
    gathering = call("POST", "/gatherings", cookie, expected=201)[0]
    unit = gathering["settlementUnits"][0]
    root = f"/gatherings/{gathering['id']}/settlement-units/{unit['id']}"
    round_entry = call("POST", root + "/rounds", cookie,
        {"total": 10000, "payerParticipantId": gathering["me"]["participantId"]}, 201)[0]
    return gathering, unit, root, round_entry["id"]


def join(room, browser, kind="SOBER", expected=200):
    gathering, unit, _, round_id = room
    return call("POST", "/guest/join/" + gathering["shareToken"], browser,
        {"displayName": "김참석", "settlementUnitId": unit["id"],
         "responses": [{"roundId": round_id, "type": kind}] if kind else []}, expected)[0]


def respond(room, browser, kind):
    gathering, unit, _, round_id = room
    call("PUT", f"/guest/gatherings/{gathering['id']}/settlement-units/{unit['id']}/responses/me", browser,
        {"answers": [{"roundId": round_id, "type": kind}]}, 204)


room = create()
gathering, unit, root, round_id = room
call("PUT", root + "/responses/me", host_cookie, {"answers": [{"roundId": round_id, "type": "DRANK"}]}, 204)
check(balance() == 0, "술자리 생성·총무 본인 참석은 0개")
browser = session()
with ThreadPoolExecutor(2) as executor:
    joined = list(executor.map(lambda _: join(room, browser), range(2)))
participant = joined[0]["participantId"]
check(balance() == 10 and joined[1]["participantId"] == participant, "동일 브라우저 동시 참여는 한 번 +10")
check(sql(f"SELECT COUNT(*) FROM spoon_attendance_rewards WHERE gathering_id={gathering['id']}") == "1", "지급 유일 이력 한 건")
respond(room, browser, "DRANK")
respond(room, browser, "ABSENT")
respond(room, browser, "SOBER")
check(balance() == 10, "음주 변경·불참 전환·재참석에도 회수/추가 적립 없음")
call("DELETE", root + f"/participants/{participant}", host_cookie, expected=204)
call("DELETE", root + f"/participants/{participant}", host_cookie, expected=204)
check(balance() == 10, "명단 제외와 반복 제외 뒤에도 이미 지급한 스푼 유지")
call("PUT", root + f"/participants/{participant}", host_cookie, expected=204)
respond(room, browser, "SOBER")
check(balance() == 10, "총무 재추가 뒤 새 응답에도 중복 지급 없음")

absent_browser = session()
join(room, absent_browser, "ABSENT")
check(balance() == 10, "전부 불참 응답은 지급하지 않음")
respond(room, absent_browser, "DRANK")
check(balance() == 20, "불참자가 처음 참석으로 변경하면 +10")
blank_browser = session()
blank = join(room, blank_browser, None)
check(balance() == 20, "빈 응답·링크 접속만으로는 지급하지 않음")
call("PUT", root + f"/participants/{blank['participantId']}/responses", host_cookie,
    {"answers": [{"roundId": round_id, "type": "SOBER"}]}, 204)
check(balance() == 30, "총무 대리 참석 응답도 한 번 지급")

second_unit = call("POST", f"/gatherings/{gathering['id']}/settlement-units", host_cookie,
    {"requestId": str(uuid.uuid4()), "participantIds": [gathering['me']['participantId'], participant]}, 201)[0]
second_root = f"/gatherings/{gathering['id']}/settlement-units/{second_unit['id']}"
second_round = call("POST", second_root + "/rounds", host_cookie,
    {"total": 10000, "payerParticipantId": gathering['me']['participantId']}, 201)[0]
respond((gathering, second_unit, second_root, second_round['id']), browser, "SOBER")
check(balance() == 30, "같은 술자리·총무의 단위를 추가해도 같은 사람 중복 지급 없음")

concurrent_rooms = [create(), create()]
concurrent_browsers = [session(), session()]
with ThreadPoolExecutor(2) as executor:
    list(executor.map(lambda pair: join(*pair), zip(concurrent_rooms, concurrent_browsers)))
check(balance() == 50, "서로 다른 술자리 동시 응답도 잔액 덮어쓰기 없이 +20")
same_room_browsers = [session(), session()]
with ThreadPoolExecutor(2) as executor:
    list(executor.map(lambda visitor: join(room, visitor), same_room_browsers))
check(balance() == 70, "같은 술자리의 동명이인 두 신원은 각각 +10")

new_room = create()
join(new_room, browser)
check(balance() == 80, "같은 브라우저라도 다른 술자리 참석은 새 지급")
before = balance()
overflow_room, overflow_browser = create(), session()
sql(f"UPDATE users SET spoon_count=9223372036854775807 WHERE id={host_id}")
try:
    join(overflow_room, overflow_browser, expected=500)
    check(sql(f"SELECT COUNT(*) FROM spoon_attendance_rewards WHERE gathering_id={overflow_room[0]['id']}") == "0"
        and sql(f"SELECT COUNT(*) FROM guest_participations WHERE gathering_id={overflow_room[0]['id']}") == "0",
        "잔액 증가 실패 시 지급 이력·비회원 참여·응답 전체 롤백")
finally:
    sql(f"UPDATE users SET spoon_count={before} WHERE id={host_id}")
join(overflow_room, overflow_browser)
check(balance() == before + 10, "실패한 동일 참여 재시도는 정상 1회 적립")
check(call("GET", "/auth/me", host_cookie)[0]["spoonCount"] == balance(), "내 정보 API에 실제 누적 스푼 반영")
detail = call("GET", f"/gatherings/{gathering['id']}", host_cookie)[0]
check(next(person for person in detail["participants"] if person["userId"] == host_id)["spoonCount"] == balance(),
    "술자리 명단의 총무 스푼도 같은 값")
check(sql("SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD='SPOON_ATTENDANCE_REWARDS' AND RESERVED=1") == "0",
    "신규 테이블명 예약어 충돌 없음")

other_host_id = make_user()
other_host_cookie = member_cookie(other_host_id)
other_host = call("POST", "/join/" + gathering["shareToken"], other_host_cookie,
    {"settlementUnitId": unit['id'], "responses": [{"roundId": round_id, "type": "SOBER"}]})[0]
other_unit = call("POST", f"/gatherings/{gathering['id']}/settlement-units", other_host_cookie,
    {"requestId": str(uuid.uuid4()), "participantIds": [other_host['participantId'], participant]}, 201)[0]
other_root = f"/gatherings/{gathering['id']}/settlement-units/{other_unit['id']}"
other_round = call("POST", other_root + "/rounds", other_host_cookie,
    {"total": 10000, "payerParticipantId": other_host['participantId']}, 201)[0]
respond((gathering, other_unit, other_root, other_round['id']), browser, "SOBER")
check(sql(f"SELECT spoon_count FROM users WHERE id={other_host_id}") == "10", "복수 총무는 본인 단위 참석에 대해서만 각각 지급")

host_before = balance()
other_before = int(sql(f"SELECT spoon_count FROM users WHERE id={other_host_id}"))
for attempt in range(3):
    first_room, second_room = create(), create(other_host_cookie)
    def cross_join(arguments):
        target_room, visitor_cookie = arguments
        return call("POST", "/join/" + target_room[0]['shareToken'], visitor_cookie,
            {"settlementUnitId": target_room[1]['id'], "responses": [{"roundId": target_room[3], "type": "SOBER"}]})
    with ThreadPoolExecutor(2) as executor:
        list(executor.map(cross_join, [(first_room, other_host_cookie), (second_room, host_cookie)]))
check(balance() == host_before + 30 and int(sql(f"SELECT spoon_count FROM users WHERE id={other_host_id}")) == other_before + 30,
    "두 총무가 서로의 방에 동시에 가입해도 FK/보상 잠금 교착 없이 각각 +30")

before = balance()
automatic_room = create()
join(automatic_room, session(), None)
automatic_root = automatic_room[2]
preview = call("GET", automatic_root + "/settlement/preview", host_cookie)[0]
call("POST", automatic_root + "/settlement", host_cookie,
    {"inputRevision": preview['inputRevision'], "inputHash": preview['inputHash']})
check(balance() == before, "확정 시 AUTO 기본 참석만으로는 스푼을 지급하지 않음")

expired_gathering_id = new_room[0]['id']
sql(f"UPDATE gatherings SET last_activity_at=UTC_TIMESTAMP()-INTERVAL 31 DAY WHERE id={expired_gathering_id}")
deadline = time.monotonic() + 90
while time.monotonic() < deadline and sql(f"SELECT COUNT(*) FROM gatherings WHERE id={expired_gathering_id}") != "0":
    time.sleep(2)
check(sql(f"SELECT COUNT(*) FROM gatherings WHERE id={expired_gathering_id}") == "0"
    and sql(f"SELECT COUNT(*) FROM spoon_attendance_rewards WHERE gathering_id={expired_gathering_id}") == "0"
    and balance() == before, "실제 만료 배치가 술자리·지급 이력을 지워도 누적 스푼은 유지")
print(f"ATTENDANCE-SPOONS: {checks} checks passed", flush=True)
