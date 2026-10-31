"""FC-028: 전용 로컬 CI DB/HTTP만 사용해 비회원의 동시 참여·권한·자동 정산을 검증한다."""
import base64
import hashlib
import hmac
import json
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from http.cookies import SimpleCookie
from urllib.error import HTTPError
from urllib.request import Request, urlopen

sys.stdout.reconfigure(encoding="utf-8")
BASE = "http://127.0.0.1:18081/api/v1"
ORIGIN = "http://localhost:5173"
checks = 0


def sql(query):
    process = subprocess.run(["docker", "exec", "jeongsan-v4-verification", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-plocal-v4-test", "-N", "-s", "jeongsan_auth_test", "-e", query],
        capture_output=True, text=True, encoding="utf-8", timeout=20)
    assert process.returncode == 0, process.stderr
    return process.stdout.strip()


assert sql("SELECT DATABASE()") == "jeongsan_auth_test"


def check(condition, label):
    global checks
    assert condition, label
    checks += 1
    print(f"PASS {checks}: {label}", flush=True)


def call(method, path, cookie=None, body=None, expected=200, origin=ORIGIN):
    headers = {"Content-Type": "application/json"}
    if origin is not None:
        headers["Origin"] = origin
    if cookie:
        headers["Cookie"] = cookie
    request = Request(BASE + path, method=method, headers=headers,
        data=json.dumps(body, ensure_ascii=False).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=30) as response:
            actual, raw, response_headers = response.status, response.read(), response.headers
    except HTTPError as error:
        actual, raw, response_headers = error.code, error.read(), error.headers
    # MVC CORS 차단은 컨트롤러의 JSON 예외 처리 전에 text/plain 403을 반환한다.
    data = (json.loads(raw) if "json" in response_headers.get("Content-Type", "") else raw.decode("utf-8")) if raw else None
    if expected is not None:
        assert actual == expected, (method, path, actual, expected, data)
    return data, response_headers, actual


def session():
    _, headers, _ = call("POST", "/guest/session", expected=204)
    parsed = SimpleCookie()
    parsed.load(headers["Set-Cookie"])
    return "jeongsan_guest=" + parsed["jeongsan_guest"].value


def member_cookie(user_id):
    def encode(value):
        return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    signed = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({"sub": str(user_id), "client": "WEB",
        "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600}).encode())
    return "jeongsan_token=" + signed + "." + encode(hmac.new(b"local-only-dev-secret-please-change-32b", signed.encode(), hashlib.sha256).digest())


provider_id = str(uuid.uuid4())
sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('GUEST_TEST','{provider_id}','검증','검증총무','FREE',UTC_TIMESTAMP())")
host_id = int(sql(f"SELECT id FROM users WHERE provider_id='{provider_id}'"))
host_cookie = member_cookie(host_id)


def create(headcount=None):
    body = {"headcount": headcount} if headcount else None
    gathering = call("POST", "/gatherings", host_cookie, body, 201)[0]
    settlement_unit = gathering["settlementUnits"][0]
    root = f"/gatherings/{gathering['id']}/settlement-units/{settlement_unit['id']}"
    round_entry = call("POST", root + "/rounds", host_cookie,
        {"total": 10000, "payerParticipantId": gathering["me"]["participantId"]}, 201)[0]
    return gathering, settlement_unit, root, round_entry["id"]


def join(gathering, settlement_unit, browser, round_id=None, name="김참여", expected=200):
    return call("POST", "/guest/join/" + gathering["shareToken"], browser,
        {"displayName": name, "settlementUnitId": settlement_unit["id"],
         "responses": [{"roundId": round_id, "type": "SOBER"}] if round_id is not None else []}, expected)[0]


def respond(gathering_id, unit_id, browser, round_id, kind="DRANK", expected=204):
    return call("PUT", f"/guest/gatherings/{gathering_id}/settlement-units/{unit_id}/responses/me", browser,
        {"answers": [{"roundId": round_id, "type": kind}]}, expected)[0]


gathering, settlement_unit, root, round_id = create()
browser = session()
guest_path = f"/guest/gatherings/{gathering['id']}/me"
check(call("POST", "/guest/session", browser, expected=204)[1].get("Set-Cookie") is None, "초기화 재시도는 유효 브라우저를 회전하지 않음")
call("POST", "/guest/session", expected=403, origin=None)
call("POST", "/guest/session", expected=403, origin="https://sibling.devkdk.com")
check(True, "브라우저 초기화부터 Origin 누락·형제 호스트 거절")
call("GET", guest_path, expected=401)
call("GET", guest_path, "jeongsan_guest=" + host_cookie.split("=", 1)[1], expected=401)
check(True, "무인증·회원 토큰의 비회원 API 우회 거절")
with ThreadPoolExecutor(2) as executor:
    duplicates = list(executor.map(lambda _: join(gathering, settlement_unit, browser, round_id), range(2)))
participant_id = duplicates[0]["participantId"]
check(duplicates[1]["participantId"] == participant_id and sql(f"SELECT COUNT(*) FROM guest_participations WHERE gathering_id={gathering['id']}") == "1",
      "같은 브라우저 동시 참여 두 건은 신원 한 개")
check(sql(f"SELECT COUNT(*) FROM timeline_entries WHERE gathering_id={gathering['id']} AND system_code='JOINED'") == "1", "동시 참여 소식은 한 번만 생성")
check(sql(f"SELECT COUNT(*) FROM notifications WHERE gathering_id={gathering['id']} AND type='JOINED'") == "1", "동시 참여의 총무 알림도 한 번만 생성")
check(duplicates[0]["responses"] == [{"roundId": round_id, "type": "SOBER", "source": "SELF"}], "참여·초기 응답을 함께 저장")
second_browser = session()
second = join(gathering, settlement_unit, second_browser, round_id)
check(second["participantId"] != participant_id, "동명이인도 다른 브라우저에서는 별도 신원")
respond(gathering["id"], settlement_unit["id"], browser, round_id)
current = call("GET", guest_path, browser)[0]
check(current["responses"][0]["type"] == "DRANK" and call("GET", guest_path, second_browser)[0]["responses"][0]["type"] == "SOBER", "수정은 본인 응답에만 반영")
check(not {"participants", "transfers", "timeline", "payout", "token", "shareToken"}.intersection(current), "비회원 조회에 전체 명단·계좌·대화·인증 값 없음")
revision_before = current["settlementUnits"][0]["inputRevision"]
notifications_before = sql(f"SELECT COUNT(*) FROM notifications WHERE gathering_id={gathering['id']}")
respond(gathering["id"], settlement_unit["id"], browser, round_id)
check(call("GET", guest_path, browser)[0]["settlementUnits"][0]["inputRevision"] == revision_before and
      sql(f"SELECT COUNT(*) FROM notifications WHERE gathering_id={gathering['id']}") == notifications_before,
      "같은 본인 응답 재시도는 버전·알림을 늘리지 않음")
check(join(gathering, settlement_unit, browser, round_id)["responses"][0]["type"] == "DRANK", "POST 재시도는 기존 수정 응답을 덮지 않음")
check(join(gathering, settlement_unit, browser, name="김다름", expected=409)["code"] == "GUEST_NAME_MISMATCH", "다른 이름 재참여로 기존 신원을 바꾸지 않음")
call("POST", "/gatherings", "jeongsan_token=" + browser.split("=", 1)[1], expected=401)
check(True, "비회원 JWT로 회원 총무 API 우회 거절")
new_browser = session()
before = sql(f"SELECT COUNT(*) FROM participants WHERE gathering_id={gathering['id']}")
join(gathering, settlement_unit, new_browser, name="김", expected=400)
join(gathering, settlement_unit, new_browser, round_id=999999999, expected=404)
check(sql(f"SELECT COUNT(*) FROM participants WHERE gathering_id={gathering['id']}") == before, "이름·차수 검증 실패 시 참여자·권한·명단 삽입 롤백")
recovered = join(gathering, settlement_unit, new_browser, round_id)
check(recovered["participantId"] != participant_id, "실패한 브라우저도 유효 요청으로 다시 참여 가능")
other_gathering, other_unit, _, other_round = create()
rollback_browser = session()
counts_before = sql(f"SELECT (SELECT COUNT(*) FROM participants WHERE gathering_id={gathering['id']}),"
    f"(SELECT COUNT(*) FROM guest_participations WHERE gathering_id={gathering['id']}),"
    f"(SELECT COUNT(*) FROM notifications WHERE gathering_id={gathering['id']})")
call("POST", "/guest/join/" + gathering["shareToken"], rollback_browser,
     {"displayName": "김롤백", "settlementUnitId": settlement_unit["id"],
      "responses": [{"roundId": round_id, "type": "SOBER"}, {"roundId": other_round, "type": "SOBER"}]}, 404)
counts_after = sql(f"SELECT (SELECT COUNT(*) FROM participants WHERE gathering_id={gathering['id']}),"
    f"(SELECT COUNT(*) FROM guest_participations WHERE gathering_id={gathering['id']}),"
    f"(SELECT COUNT(*) FROM notifications WHERE gathering_id={gathering['id']})")
check(counts_after == counts_before, "첫 답변 저장 뒤 다음 답변 실패도 신원·응답·알림 전체 롤백")
call("GET", f"/guest/gatherings/{other_gathering['id']}/me", browser, expected=403)
respond(other_gathering["id"], other_unit["id"], browser, other_round, expected=403)
respond(gathering["id"], settlement_unit["id"], browser, other_round, expected=404)
check(True, "다른 술자리 조회·수정과 다른 단위 차수 응답 거절")
call("PUT", f"/guest/gatherings/{gathering['id']}/settlement-units/{settlement_unit['id']}/responses/me", browser,
     {"answers": [{"roundId": round_id, "type": "ABSENT"}]}, expected=403, origin="https://evil.test")
check(call("GET", guest_path, browser)[0]["responses"][0]["type"] == "DRANK", "다른 Origin의 쓰기는 기존 응답을 바꾸지 않음")
respond(gathering["id"], settlement_unit["id"], browser, round_id, "EXEMPT", expected=400)
check(call("GET", guest_path, browser)[0]["responses"][0]["type"] == "DRANK", "비회원 본인 면제 요청은 400이며 원래 응답 유지")
extra = call("POST", f"/gatherings/{gathering['id']}/settlement-units", host_cookie,
    {"requestId": str(uuid.uuid4()), "participantIds": [gathering["me"]["participantId"]]}, 201)[0]
join(gathering, extra, browser)
check(sql(f"SELECT COUNT(*) FROM guest_participations WHERE gathering_id={gathering['id']}") == "3", "다른 단위 참여도 공유 비회원 신원 재사용")
shared_browser = session()
with ThreadPoolExecutor(2) as executor:
    two_units = list(executor.map(lambda target_unit: join(gathering, target_unit, shared_browser), [settlement_unit, extra]))
check(two_units[0]["participantId"] == two_units[1]["participantId"] and
      sql(f"SELECT COUNT(*) FROM settlement_unit_members WHERE participant_id={two_units[0]['participantId']}") == "2",
      "서로 다른 단위에 동시에 참여해도 한 신원과 두 명단으로 저장")
parallel_gathering, parallel_unit, _, parallel_round = create()
parallel_browsers = [session(), session()]
with ThreadPoolExecutor(2) as executor:
    independent = list(executor.map(lambda separate_browser: join(parallel_gathering, parallel_unit, separate_browser, parallel_round), parallel_browsers))
check(independent[0]["participantId"] != independent[1]["participantId"] and
      sql(f"SELECT COUNT(*) FROM guest_participations WHERE gathering_id={parallel_gathering['id']}") == "2",
      "서로 다른 브라우저의 동시 참여는 동명이인이어도 각각 저장")
with_guest = call("POST", f"/gatherings/{gathering['id']}/settlement-units", host_cookie,
    {"requestId": str(uuid.uuid4()), "participantIds": [gathering["me"]["participantId"], participant_id]}, 201)[0]
check(participant_id in with_guest["participantIds"], "추가 총무 단위의 명단에서도 비회원 재사용")
call("POST", root + "/rounds", host_cookie, {"total": 10000, "payerParticipantId": participant_id}, 409)
check(True, "계좌 권한 없는 비회원 결제자 지정은 거절")
call("PUT", root + f"/participants/{participant_id}/responses", host_cookie, {"answers": [{"roundId": round_id, "type": "SOBER"}]}, 204)
check(call("GET", guest_path, browser)[0]["responses"][0]["source"] == "HOST", "총무의 비회원 대리 응답 유지")
call("DELETE", root + f"/participants/{participant_id}", host_cookie, expected=204)
respond(gathering["id"], settlement_unit["id"], browser, round_id, expected=403)
join(gathering, settlement_unit, browser, expected=403)
check(next(unit for unit in call("GET", guest_path, browser)[0]["settlementUnits"] if unit["id"] == settlement_unit["id"])["included"] is False,
      "총무 제외 후 자기 수정·재가입 거절과 제외 상태 재조회")
call("PUT", root + f"/participants/{participant_id}", host_cookie, expected=204)
check(next(unit for unit in call("GET", guest_path, browser)[0]["settlementUnits"] if unit["id"] == settlement_unit["id"])["included"] is True,
      "총무가 비회원 명단을 다시 포함할 수 있음")

automatic_gathering, automatic_unit, automatic_root, automatic_round = create(2)
call("PUT", automatic_root + "/responses/me", host_cookie, {"answers": [{"roundId": automatic_round, "type": "SOBER"}]}, 204)
automatic_browser = session()
with ThreadPoolExecutor(2) as executor:
    results = list(executor.map(lambda _: join(automatic_gathering, automatic_unit, automatic_browser, automatic_round), range(2)))
check(results[0]["participantId"] == results[1]["participantId"] and all(result["settlementUnits"][0]["status"] == "SETTLING" for result in results),
      "마지막 비회원 참여는 한 번 자동 정산하고 동시 재시도도 200")
check(sql(f"SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={automatic_unit['id']}") == "1" and
      sql(f"SELECT amount FROM settlement_transfers WHERE sender_participant_id={results[0]['participantId']}") == "5000",
      "자동 정산 스냅샷 한 개와 기존 Core의 5000원 송금")
respond(automatic_gathering["id"], automatic_unit["id"], automatic_browser, automatic_round, expected=409)
check(True, "정산 후 비회원 응답은 잠김")
check(sql("SELECT COUNT(*) FROM notifications WHERE user_id=0") == "0", "비회원 알림은 가짜 회원 0번에 저장하지 않음")
host_detail = call("GET", f"/gatherings/{automatic_gathering['id']}", host_cookie)[0]
check(any(person["id"] == results[0]["participantId"] and person["userId"] is None for person in host_detail["participants"]), "총무의 기존 상세 응답에 비회원과 정산 결과 표시")

# 응답·송금이 없는 별도 fixture에서 권한 표식이 참여자 삭제와 함께 없어지는지 검증한다.
cleanup_gathering, cleanup_unit, _, _ = create()
cleanup_browser = session()
cleanup_participant = join(cleanup_gathering, cleanup_unit, cleanup_browser)["participantId"]
sql(f"DELETE FROM settlement_unit_members WHERE participant_id={cleanup_participant}; DELETE FROM participants WHERE id={cleanup_participant}")
check(sql(f"SELECT COUNT(*) FROM guest_participations WHERE participant_id={cleanup_participant}") == "0", "참여자 삭제 시 비회원 권한 CASCADE 삭제")
call("GET", f"/guest/gatherings/{cleanup_gathering['id']}/me", cleanup_browser, expected=403)
check(True, "서명된 쿠키가 남아도 삭제된 참여자 권한은 복구되지 않음")
check(sql("SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEYWORDS WHERE WORD='GUEST_PARTICIPATIONS' AND RESERVED=1") == "0", "새 테이블 이름의 MySQL 예약어 검사")

# FC-028 2차: 개인 확정 금액은 표 재계산과 무관하게 기존 송금 스냅샷을 따른다.
money_path = f"/guest/gatherings/{automatic_gathering['id']}/settlement-units/{automatic_unit['id']}/settlement"
money = call("GET", money_path, automatic_browser)[0]
transfer_id = money["transfers"][0]["id"]
check(money["total"] == 5000 and money["isEstimate"] is False and money["transfers"][0]["payout"] is None,
      "개인 확정 금액과 계좌 미등록 상태 조회")
check(sum(item["amount"] for item in money["transfers"][0]["basis"]) == 5000 and money["settledAt"].endswith("Z"),
      "개인 근거 합계는 송금액과 같고 확정 시각은 UTC")
call("POST", f"/guest/transfers/{transfer_id}/sent", automatic_browser, expected=409)
call("GET", money_path, browser, expected=403)
call("POST", f"/guest/transfers/{transfer_id}/sent", browser, expected=403)
check(True, "계좌 미등록 보냈어요와 타방 개인 금액·송금 변경 거절")
call("PUT", "/users/me/payout", host_cookie, {"bank": "국민", "accountNo": "001234567890", "holder": "검증총무"})
money = call("GET", money_path, automatic_browser)[0]
check(money["transfers"][0]["payout"] == {"bank": "국민", "accountNo": "001234567890", "holder": "검증총무"},
      "확정 송금의 수취인 계좌만 선행 0을 보존해 조회")
estimated_path = f"/guest/gatherings/{gathering['id']}/settlement-units/{settlement_unit['id']}/settlement"
estimated = call("GET", estimated_path, browser)[0]
check(estimated["available"] and estimated["isEstimate"] and all(item["payout"] is None and item["id"] is None for item in estimated["transfers"]),
      "예상 금액에는 계좌·실제 송금 ID를 주지 않음")
table_path = automatic_root + "/settlement/table"
table = call("GET", table_path, host_cookie)[0]
check(table["isEstimate"] is False and table["table"]["grandTotal"] == 10000 and table["generatedAt"].endswith("Z"),
      "총무 확정 표의 상태·총액·기준 시각 조회")
check(table["table"]["transfers"] == [{"fromParticipantId": money["participantId"],
      "toParticipantId": money["transfers"][0]["toParticipantId"], "amount": 5000}], "캡처용 송금 안내도 확정 스냅샷과 같은 송금자·수취인·금액")
call("GET", table_path, automatic_browser, expected=401)
check("payout" not in json.dumps(table) and "numerator" not in json.dumps(table), "총무 표에는 계좌·유리수 내부 표현 없음")
open_table = call("GET", root + "/settlement/table", host_cookie)[0]["table"]
check(any(cell["source"] == "UNANSWERED" for row in open_table["rounds"] for cell in row["cells"]), "예상 표에서 미응답을 불참과 구분")
for line in open_table["lines"]:
    displayed = sum(cell["amount"] for row in open_table["rounds"] for cell in row["cells"] if cell["participantId"] == line["participantId"])
    adjustment = sum(row["cells"][str(line["participantId"])] for row in open_table["adjustments"])
    assert displayed + adjustment == line["total"]
check(sum(line["total"] for line in open_table["lines"]) == open_table["grandTotal"], "표 셀·조정·최종 개인합계와 전체 원금 일치")
manual_preview = call("GET", root + "/settlement/preview", host_cookie)[0]
call("POST", root + "/settlement", host_cookie, {"inputRevision": manual_preview["inputRevision"], "inputHash": manual_preview["inputHash"]})
my_transfer = call("GET", estimated_path, browser)[0]["transfers"][0]["id"]
call("POST", f"/guest/transfers/{my_transfer}/sent", second_browser, expected=403)
check(sql(f"SELECT status FROM settlement_transfers WHERE id={my_transfer}") == "WAITING", "같은 술자리의 다른 참여자도 내 송금을 보냈다고 표시할 수 없음")
with ThreadPoolExecutor(2) as executor:
    sent = list(executor.map(lambda _: call("POST", f"/guest/transfers/{transfer_id}/sent", automatic_browser, expected=204), range(2)))
sent_at = sql(f"SELECT sent_at FROM settlement_transfers WHERE id={transfer_id}")
call("POST", f"/guest/transfers/{transfer_id}/sent", automatic_browser, expected=204)
check(sent_at and sql(f"SELECT sent_at FROM settlement_transfers WHERE id={transfer_id}") == sent_at and
      sql(f"SELECT COUNT(*) FROM notifications WHERE gathering_id={automatic_gathering['id']} AND type='SENT'") == "1",
      "보냈어요 동시·반복 요청은 최초 시각과 알림 한 건을 보존")
call("DELETE", automatic_root + "/settlement", host_cookie, expected=409)
call("POST", f"/transfers/{transfer_id}/confirm", host_cookie, expected=204)
confirmed = call("GET", money_path, automatic_browser)[0]
check(confirmed["status"] == "COMPLETED" and confirmed["total"] == 5000 and confirmed["transfers"][0]["status"] == "CONFIRMED",
      "총무 확인 뒤에도 개인 총부담은 유지하고 송금 상태만 확인됨으로 표시")
call("POST", f"/guest/transfers/{transfer_id}/sent", automatic_browser, expected=204)
check(call("GET", money_path, automatic_browser)[0]["transfers"][0]["status"] == "CONFIRMED", "확인된 송금의 늦은 재시도는 상태를 되돌리지 않음")
sql(f"UPDATE rounds SET total_amount=11000 WHERE id={automatic_round}")
try:
    rejected = call("GET", table_path, host_cookie, expected=409)[0]
    check(rejected["code"] == "SETTLEMENT_VIEW_UNAVAILABLE" and call("GET", money_path, automatic_browser)[0]["total"] == 5000,
          "확정 입력·스냅샷 불일치는 표를 거절하고 개인 확정 송금은 변경하지 않음")
finally:
    sql(f"UPDATE rounds SET total_amount=10000 WHERE id={automatic_round}")
empty_room = call("POST", "/gatherings", host_cookie, expected=201)[0]
empty_unit = empty_room["settlementUnits"][0]
empty_browser = session()
join(empty_room, empty_unit, empty_browser)
unavailable = call("GET", f"/guest/gatherings/{empty_room['id']}/settlement-units/{empty_unit['id']}/settlement", empty_browser)[0]
check(unavailable["available"] is False and unavailable["errorCode"] == "NO_ROUNDS" and unavailable["total"] is None,
      "아직 차수 없는 술자리는 개인 금액 준비 중으로 표시")
print(f"GUEST-PARTICIPATION: {checks} checks passed", flush=True)
