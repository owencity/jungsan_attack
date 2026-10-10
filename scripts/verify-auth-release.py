"""전용 로컬 auth DB와 18081 서버만 검사한다. 실제 제공자 승인은 DB probe/서명 테스트와 별도다."""
import base64, hashlib, hmac, json, subprocess, time, uuid, sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from concurrent.futures import ThreadPoolExecutor
sys.stdout.reconfigure(encoding="utf-8")
BASE="http://127.0.0.1:18081/api/v1"
DATABASE="jeongsan_auth_test"
CONTAINER="jeongsan-v4-verification"
checks=0
def sql(query):
    result=subprocess.run(["docker","exec",CONTAINER,"mysql","--default-character-set=utf8mb4","-uroot","-plocal-v4-test","-N","-s",DATABASE,"-e",query],capture_output=True,text=True,encoding="utf-8")
    if result.returncode: raise AssertionError(result.stderr)
    return result.stdout.strip()
assert sql("SELECT DATABASE()") == DATABASE
def check(value,label):
    global checks
    assert value,label
    checks+=1; print(f"PASS {checks}: {label}",flush=True)
def enc(value): return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
def jwt(uid,client="WEB"):
    body=enc(b'{"alg":"HS256"}')+"."+enc(json.dumps({"sub":str(uid),"client":client,"jti":str(uuid.uuid4()),"exp":int(time.time())+3600}).encode())
    return body+"."+enc(hmac.new(b"local-only-dev-secret-please-change-32b",body.encode(),hashlib.sha256).digest())
def call(verb,path,token=None,client="WEB",body=None,status=200):
    headers={"Content-Type":"application/json"}
    if token:
        if client=="WEB": headers["Cookie"]="jeongsan_token="+token
        else: headers["Authorization"]="Bearer "+token
    request=Request(BASE+path,headers=headers,method=verb,data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request,timeout=20) as response: actual=response.status;data=response.read();rh=response.headers
    except HTTPError as e: actual=e.code;data=e.read();rh=e.headers
    decoded=json.loads(data) if data else None
    if status is not None: assert actual==status,(path,actual,status,decoded)
    return actual,decoded,rh
key=str(uuid.uuid4())
sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,created_at) VALUES('AUTH_TEST','{key}','로그인검증','김검증',UTC_TIMESTAMP());")
uid=int(sql(f"SELECT id FROM users WHERE provider_id='{key}'"))
web=jwt(uid);app=jwt(uid,"APP")
call("GET","/auth/me",web);call("GET","/auth/me",app,"APP");check(True,"실제 리졸버가 웹 쿠키·앱 Bearer 계정을 조회")
call("GET","/auth/me",web,"APP",status=401);call("GET","/auth/me",app,"WEB",status=401);check(True,"APP/WEB 교차 사용은 실제 HTTP 401")
call("POST","/auth/logout",app,"APP",status=204);call("GET","/auth/me",app,"APP",status=401);check(True,"로그아웃 후 동일 Bearer 복사본이 401")
verifier="v"*43;challenge=enc(hashlib.sha256(verifier.encode()).digest());ticket=enc(uuid.uuid4().bytes+uuid.uuid4().bytes);th=hashlib.sha256(ticket.encode()).hexdigest()
sql(f"INSERT INTO auth_tickets VALUES('{th}',{uid},'{challenge}',UTC_TIMESTAMP()+INTERVAL 50 SECOND)")
call("POST","/auth/app/exchange",body={"ticket":ticket,"codeVerifier":"x"*43},status=401)
check(sql(f"SELECT COUNT(*) FROM auth_tickets WHERE ticket_hash='{th}'")=="1","다른 verifier 거절은 정당한 교환 티켓을 소비하지 않음")
with ThreadPoolExecutor(4) as pool:
    replies=list(pool.map(lambda _:call("POST","/auth/app/exchange",body={"ticket":ticket,"codeVerifier":verifier},status=None),range(4)))
check(sorted(r[0] for r in replies)==[200,401,401,401],"HTTP 동시 티켓 교환 4건 중 1건 성공")
exchange=next(r[1] for r in replies if r[0]==200)
check(isinstance(exchange["expiresAt"],str) and "T" in exchange["expiresAt"],"실제 Boot 응답 만료는 ISO UTC 문자열")
call("GET","/auth/me",exchange["token"],"APP")
room=call("POST","/gatherings",web,status=201)[1];gid=room["id"];unit=room["settlementUnits"][0]["id"]
response=call("DELETE","/users/me",web,status=409)[1]
check(response["code"]=="ACTIVE_GATHERING_EXISTS" and sql(f"SELECT COUNT(*) FROM users WHERE id={uid}")=="1","OPEN 술자리 탈퇴는 거절하고 개인정보 보존")
sql(f"UPDATE settlement_units SET status='COMPLETED',completed_at=UTC_TIMESTAMP() WHERE id={unit}; UPDATE gatherings SET status='COMPLETED',completed_at=UTC_TIMESTAMP(),delete_scheduled_at=UTC_TIMESTAMP()+INTERVAL 7 DAY WHERE id={gid};")
_,_,headers=call("DELETE","/users/me",web,status=204)
check("Max-Age=0" in headers.get("Set-Cookie",""),"탈퇴 응답은 웹 쿠키를 만료")
call("GET","/auth/me",web,status=401);call("GET","/auth/me",exchange["token"],"APP",status=401)
check(sql(f"SELECT COUNT(*) FROM users WHERE id={uid}")=="0" and sql(f"SELECT COUNT(*) FROM participants WHERE gathering_id={gid} AND user_id IS NULL AND name='탈퇴한 사용자'")=="1","완료 좌석 익명화·계정 삭제·모든 기존 토큰 차단")
preview=call("GET","/join/"+room["shareToken"])[1]
check(len(preview["settlementUnits"])==1 and preview["settlementUnits"][0]["host"]["displayName"]=="탈퇴한 사용자","총무 탈퇴 후에도 공개 미리보기의 단위가 사라지지 않음")
columns=sql("SELECT CONCAT(TABLE_NAME,':',COLUMN_NAME,':',IS_NULLABLE) FROM information_schema.columns WHERE table_schema=DATABASE() AND ((TABLE_NAME='participants' AND COLUMN_NAME='user_id') OR (TABLE_NAME='gatherings' AND COLUMN_NAME='host_user_id'))")
check("participants:user_id:YES" in columns and "gatherings:host_user_id:YES" in columns,"실제 FK 열 nullable 마이그레이션 확인")
call("POST","/auth/app/exchange",body={"ticket":"x"},status=400);check(True,"교환 verifier 누락 JSON은 400")
call("GET","/auth/kakao/callback?code=x",status=400);check(True,"필수 callback state 누락은 실제 HTTP 400")
for attempt in range(4):
    owners=[]
    for _ in range(2):
        identity=str(uuid.uuid4())
        sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,created_at) VALUES('AUTH_TEST','{identity}','경합검증','김경합',UTC_TIMESTAMP())")
        owners.append(int(sql(f"SELECT id FROM users WHERE provider_id='{identity}'")))
    a,b=owners; ta,tb=jwt(a),jwt(b)
    room=call("POST","/gatherings",ta,status=201)[1];gid=room["id"];initial=room["settlementUnits"][0]["id"];pa=room["me"]["participantId"]
    pb=call("POST","/join/"+room["shareToken"],tb,body={"settlementUnitId":initial})[1]["participantId"]
    sql(f"UPDATE settlement_units SET status='COMPLETED',completed_at=UTC_TIMESTAMP() WHERE id={initial}; UPDATE gatherings SET status='COMPLETED',completed_at=UTC_TIMESTAMP(),delete_scheduled_at=UTC_TIMESTAMP()+INTERVAL 7 DAY WHERE id={gid}")
    with ThreadPoolExecutor(2) as pool:
        deletion=pool.submit(call,"DELETE","/users/me",ta,status=None)
        addition=pool.submit(call,"POST",f"/gatherings/{gid}/settlement-units",tb,body={"requestId":str(uuid.uuid4()),"participantIds":[pa,pb]},status=None)
        deleted,added=deletion.result(),addition.result()
    assert (deleted[0],added[0]) in [(204,403),(409,201)],(deleted[0],added[0],deleted[1],added[1])
    if deleted[0]==204:
        assert sql(f"SELECT COUNT(*) FROM users WHERE id={a}")=="0"
        assert sql(f"SELECT COUNT(*) FROM settlement_units WHERE gathering_id={gid} AND status='OPEN'")=="0"
    else:
        assert deleted[1]["code"] in ["ACTIVE_GATHERING_EXISTS","ACCOUNT_STATE_CHANGED"]
        assert sql(f"SELECT COUNT(*) FROM users WHERE id={a}")=="1"
    check(True,f"탈퇴↔추가 단위 생성 경합 {attempt+1}: 사용자 삭제와 활성 정산이 함께 성공하지 않음")
print(json.dumps({"checks":checks,"http":"real Spring Boot","database":DATABASE}),flush=True)
