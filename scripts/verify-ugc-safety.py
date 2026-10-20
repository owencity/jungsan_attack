"""FC-024: CI 전용 MySQL/HTTP에서 중복·권한·개인/전체 숨김·차단·탈퇴를 검증한다."""
import base64
import hashlib
import hmac
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.request import Request, urlopen

sys.stdout.reconfigure(encoding="utf-8")
BASE = "http://127.0.0.1:18081/api/v1"
checks = 0

def sql(query):
    process = subprocess.run(["docker", "exec", "jeongsan-v4-verification", "mysql", "--default-character-set=utf8mb4",
        "-uroot", "-plocal-v4-test", "-N", "-s", "jeongsan_auth_test", "-e", query],
        capture_output=True, text=True, encoding="utf-8")
    assert process.returncode == 0, process.stderr
    return process.stdout.strip()

assert sql("SELECT DATABASE()") == "jeongsan_auth_test"

def check(condition, label):
    global checks
    assert condition, label
    checks += 1
    print(f"PASS {checks}: {label}", flush=True)

def call(method, path, user=None, body=None, expected=200):
    def encode(value):
        return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    headers = {"Content-Type": "application/json"}
    if user is not None:
        token = encode(b'{"alg":"HS256"}') + "." + encode(json.dumps({
            "sub": str(user), "client": "WEB", "jti": str(uuid.uuid4()), "exp": int(time.time()) + 3600}).encode())
        signature = encode(hmac.new(b"local-only-dev-secret-please-change-32b", token.encode(), hashlib.sha256).digest())
        headers["Cookie"] = "jeongsan_token=" + token + "." + signature
    request = Request(BASE + path, headers=headers, method=method,
        data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=20) as response:
            status, raw = response.status, response.read()
    except HTTPError as error:
        status, raw = error.code, error.read()
    value = json.loads(raw) if raw else None
    assert status == expected, (method, path, status, value)
    return value

def new_user(name):
    identity = str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('UGC_TEST','{identity}','검증{name}','{name}','FREE',UTC_TIMESTAMP())")
    return int(sql(f"SELECT id FROM users WHERE provider_id='{identity}'"))

author, first, second, outsider = [new_user(name) for name in ("안전작성자", "첫신고자", "두신고자", "외부사용자")]
room = call("POST", "/gatherings", author, expected=201)
gathering = room["id"]
unit = room["settlementUnits"][0]["id"]
author_participant = room["me"]["participantId"]
def join(target_room, user):
    return call("POST", "/join/" + target_room["shareToken"], user,
        {"settlementUnitId": target_room["settlementUnits"][0]["id"]})["participantId"]
first_participant = join(room, first)
second_participant = join(room, second)
call("POST", f"/gatherings/{gathering}/settlement-units/{unit}/rounds", author,
    {"total": 30000, "payerParticipantId": author_participant}, 201)
other_room = call("POST", "/gatherings", author, expected=201)
other_gathering = other_room["id"]
join(other_room, first)
join(other_room, second)
other_author_participant = other_room["me"]["participantId"]

def message(user, text, gathering_id=gathering):
    return call("POST", f"/gatherings/{gathering_id}/messages", user, {"text": text}, 201)["id"]
def report(entry, user, reason="ABUSE", gathering_id=gathering, expected=204):
    return call("POST", f"/gatherings/{gathering_id}/messages/{entry}/reports", user, {"reason": reason}, expected)
def timeline(user, gathering_id=gathering):
    return call("GET", f"/gatherings/{gathering_id}", user)["timeline"]
def visible(user, entry, gathering_id=gathering):
    return any(row["id"] == entry for row in timeline(user, gathering_id))
def count_messages():
    return int(sql(f"SELECT COUNT(*) FROM timeline_entries WHERE gathering_id={gathering} AND type='MESSAGE'"))

check(sql("SELECT COUNT(*) FROM information_schema.keywords WHERE WORD IN ('MESSAGE_REPORTS','USER_BLOCKS','ENTRY','REPORT','BLOCK') AND RESERVED=1") == "0",
    "새 테이블·쿼리 별칭은 MySQL 예약어와 충돌하지 않음")
check(sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('message_reports','user_blocks') AND column_comment=''") == "0",
    "신규 신고·차단 모든 컬럼에 설명 존재")
check(sql(f"SELECT COUNT(*) FROM users WHERE id={author} AND message_banned_at IS NULL") == "1",
    "기존 사용자 메시지 금지는 NULL로 호환")
entry = message(author, "본문비공개표식 메시지")
for method, path, body in (
    ("POST", f"/gatherings/{gathering}/messages/{entry}/reports", {"reason": "ABUSE"}),
    ("POST", "/me/blocks", {"gatheringId": gathering, "participantId": author_participant}),
    ("GET", "/me/blocks", None), ("DELETE", f"/me/blocks/{author}", None)):
    call(method, path, body=body, expected=401)
check(True, "신고·차단 4개 엔드포인트는 비인증 401")
report(entry, outsider, expected=403)
call("POST", "/me/blocks", outsider, {"gatheringId": gathering, "participantId": author_participant}, 403)
check(True, "비참여자는 신고·차단 불가")
report(entry, author, expected=400)
report(entry, first, "UNKNOWN", expected=400)
system_entry = next(row["id"] for row in timeline(author) if row["type"] == "SYSTEM")
report(system_entry, first, expected=400)
spoon = int(sql(f"INSERT INTO timeline_entries(gathering_id,type,body,created_at) VALUES({gathering},'SPOON','검증 스푼',UTC_TIMESTAMP()); SELECT LAST_INSERT_ID()"))
report(spoon, first, expected=400)
report(entry, first, gathering_id=other_gathering, expected=404)
check(sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id={entry}") == "0",
    "본인·SYSTEM·SPOON·잘못된 사유·다른 방 신고는 400/404이며 저장 없음")

with ThreadPoolExecutor(6) as pool:
    list(pool.map(lambda _: report(entry, first), range(6)))
check(sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id={entry}") == "1"
    and sql(f"SELECT hidden_at IS NULL FROM timeline_entries WHERE id={entry}") == "1",
    "같은 신고자 동시 요청은 1행이고 전체 숨김 없음")
check(not visible(first, entry) and visible(second, entry) and visible(author, entry), "첫 신고자는 즉시 숨김, 다른 사람은 계속 표시")
report(entry, first, "OTHER")
check(sql(f"SELECT reason FROM message_reports WHERE entry_id={entry}") == "ABUSE", "중복 신고는 첫 사유 유지하며 204")
with ThreadPoolExecutor(4) as pool:
    list(pool.map(lambda _: report(entry, second, "SPAM"), range(4)))
check(sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id={entry}") == "2"
    and sql(f"SELECT hidden_at IS NOT NULL FROM timeline_entries WHERE id={entry}") == "1",
    "서로 다른 2명 신고는 정확히 2행과 전체 숨김")
check(all(not visible(user, entry) for user in (author, first, second)), "작성자 포함 전체 상세에서 숨김")
check(all(all(row["id"] != entry for gathering_row in call("GET", "/me/gatherings", user) for row in gathering_row["timeline"])
    for user in (author, first, second)), "목록의 timeline에도 같은 숨김 적용")

simultaneous = message(author, "서로 다른 첫 신고 경합")
with ThreadPoolExecutor(2) as pool:
    list(pool.map(lambda user: report(simultaneous, user), (first, second)))
check(sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id={simultaneous}") == "2"
    and not visible(author, simultaneous), "서로 다른 최초 신고가 동시에 와도 전체 숨김")

personal_entry = message(author, "개인 신고는 차단 해제 후에도 숨김")
report(personal_entry, first, "OTHER")
remaining = message(author, "차단 검증 정상 메시지")
other_entry = message(author, "다른 방의 같은 작성자", other_gathering)
before = call("GET", f"/gatherings/{gathering}", first)
for target in (author_participant,):
    call("POST", "/me/blocks", first, {"gatheringId": gathering, "participantId": target}, 204)
blocked_at = sql(f"SELECT created_at FROM user_blocks WHERE blocker_user_id={first} AND blocked_user_id={author}")
with ThreadPoolExecutor(4) as pool:
    list(pool.map(lambda _: call("POST", "/me/blocks", first,
        {"gatheringId": other_gathering, "participantId": other_author_participant}, 204), range(4)))
check(sql(f"SELECT COUNT(*) FROM user_blocks WHERE blocker_user_id={first} AND blocked_user_id={author}") == "1"
    and sql(f"SELECT created_at FROM user_blocks WHERE blocker_user_id={first} AND blocked_user_id={author}") == blocked_at,
    "다른 방의 같은 사용자 차단도 멱등·최초 시각 보존")
blocks = call("GET", "/me/blocks", first)
check(len(blocks) == 1 and blocks[0]["userId"] == author and blocks[0]["displayName"] == "안전작성자"
    and isinstance(blocks[0]["blockedAt"], str), "차단 목록 실제 DTO·UTC 시각 확인")
check(not visible(first, remaining) and not visible(first, other_entry, other_gathering)
    and visible(second, remaining) and visible(second, other_entry, other_gathering), "차단은 모든 방의 본인 MESSAGE 조회에만 적용")
after = call("GET", f"/gatherings/{gathering}", first)
check({key: value for key, value in before.items() if key != "timeline"}
    == {key: value for key, value in after.items() if key != "timeline"}
    and any(row["id"] == system_entry for row in after["timeline"])
    and any(row["id"] == spoon for row in after["timeline"]),
    "차단 전후 정산·명단·응답·송금 전체 동일, SYSTEM·SPOON 유지")
call("POST", "/me/blocks", first, {"gatheringId": gathering, "participantId": first_participant}, 400)
call("POST", "/me/blocks", first, {"gatheringId": other_gathering, "participantId": author_participant}, 400)
check(True, "자기 자신과 다른 방의 participant ID 차단 거절")
call("DELETE", f"/me/blocks/{author}", second, expected=204)
check(len(call("GET", "/me/blocks", first)) == 1, "다른 사람은 내 차단을 해제할 수 없음")
call("DELETE", f"/me/blocks/{author}", first, expected=204)
call("DELETE", f"/me/blocks/{author}", first, expected=204)
check(call("GET", "/me/blocks", first) == [] and visible(first, remaining)
    and visible(first, other_entry, other_gathering) and not visible(first, personal_entry)
    and not visible(first, entry), "차단 해제는 멱등 204, 신고 숨김은 유지")

message_count = count_messages()
for text in ("씨 ! 발", "씨\u200b발", "병.신", "ＦＵＣＫ", "섹 스"):
    response = call("POST", f"/gatherings/{gathering}/messages", author, {"text": text}, 422)
    assert response["code"] == "MESSAGE_NOT_ALLOWED" and response["message"] == "부적절한 표현이 있어요"
check(count_messages() == message_count, "금칙어·공백·특수문자·전각 우회는 422이며 저장 없음")
sql(f"UPDATE users SET message_banned_at=UTC_TIMESTAMP(6) WHERE id={author}")
response = call("POST", f"/gatherings/{gathering}/messages", author, {"text": "정상 문장"}, 403)
check(response["code"] == "MESSAGE_BANNED" and count_messages() == message_count, "운영 메시지 금지는 403이며 저장 없음")
call("GET", f"/gatherings/{gathering}", author)
call("GET", f"/gatherings/{gathering}/settlement-units/{unit}/settlement/preview", author)
check(True, "메시지 금지 계정의 상세·정산 접근은 유지")
sql(f"UPDATE users SET message_banned_at=NULL WHERE id={author}")
message(author, "가" * 200)
check(count_messages() == message_count + 1, "정상 최대 길이 메시지는 201")
sql(f"UPDATE message_reports SET handled_at=UTC_TIMESTAMP(6) WHERE entry_id IN ({entry},{personal_entry})")
check(not visible(first, personal_entry) and not visible(author, entry), "처리 완료는 개인·전체 숨김을 해제하지 않음")

# 신고자 탈퇴 뒤에도 이미 받은 서로 다른 신고는 집계하고 양방향 차단 FK는 제거한다.
late_entry = message(author, "신고자 탈퇴 후 검토")
report(late_entry, first)
call("POST", "/me/blocks", first, {"gatheringId": gathering, "participantId": author_participant}, 204)
call("POST", "/me/blocks", second, {"gatheringId": gathering, "participantId": first_participant}, 204)
sql(f"UPDATE settlement_units SET status='COMPLETED',completed_at=UTC_TIMESTAMP() WHERE gathering_id IN ({gathering},{other_gathering});"
    f"UPDATE gatherings SET status='COMPLETED',completed_at=UTC_TIMESTAMP(),delete_scheduled_at=UTC_TIMESTAMP()+INTERVAL 7 DAY WHERE id IN ({gathering},{other_gathering})")
call("DELETE", "/users/me", first, expected=204)
check(sql(f"SELECT COUNT(*) FROM user_blocks WHERE blocker_user_id={first} OR blocked_user_id={first}") == "0"
    and sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id={late_entry} AND reporter_user_id IS NULL") == "1",
    "실제 탈퇴는 신고자 익명화·양방향 차단 FK 삭제")
report(late_entry, second)
check(not visible(author, late_entry), "신고자 탈퇴 전후 서로 다른 2행이면 전체 숨김")

logs = Path("server/build/release-http.log").read_text(encoding="utf-8")
report_lines = [line for line in logs.splitlines() if " MESSAGE_REPORTED " in line]
check(all(re.search(r"MESSAGE_REPORTED entryId=\d+ gatheringId=\d+ reason=(ABUSE|SEXUAL|SPAM|OTHER)$", line) for line in report_lines)
    and len([line for line in report_lines if f"entryId={entry} " in line]) == 2
    and "본문비공개표식" not in "\n".join(report_lines),
    "커밋 신고만 정해진 한 줄 로그, 재시도 중복·본문·개인정보 없음")
call("DELETE", "/users/me", author, expected=204)
check(sql(f"SELECT COUNT(*) FROM message_reports WHERE entry_id IN ({entry},{simultaneous},{personal_entry},{late_entry})") == "0",
    "실제 작성자 탈퇴로 메시지와 신고 FK CASCADE 삭제")
print(json.dumps({"checks": checks, "http": "real Spring Boot", "database": "jeongsan_auth_test"}), flush=True)
