"""실제 DB 대상은 전용 로컬 auth DB/18081뿐이다. 운영이나 프론트 개발 DB를 받지 않는다."""
import base64, hashlib, hmac, json, subprocess, time, uuid, sys
from datetime import datetime, timezone
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding="utf-8")
BASE="http://127.0.0.1:18081/api/v1"
DATABASE="jeongsan_auth_test"
checks=0

def sql(query):
    reply=subprocess.run(["docker","exec","jeongsan-v4-verification","mysql","--default-character-set=utf8mb4",
        "-uroot","-plocal-v4-test","-N","-s",DATABASE,"-e",query],capture_output=True,text=True,encoding="utf-8")
    assert reply.returncode==0,reply.stderr
    return reply.stdout.strip()

assert sql("SELECT DATABASE()")==DATABASE

def check(value,label):
    global checks
    assert value,label
    checks+=1
    print(f"PASS {checks}: {label}",flush=True)

def call(verb,path,user=None,body=None,status=200):
    headers={"Content-Type":"application/json"}
    if user:
        def enc(value): return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
        signed=enc(b'{"alg":"HS256"}')+"."+enc(json.dumps({"sub":str(user),"client":"WEB","jti":str(uuid.uuid4()),"exp":int(time.time())+3600}).encode())
        headers["Cookie"]="jeongsan_token="+signed+"."+enc(hmac.new(b"local-only-dev-secret-please-change-32b",signed.encode(),hashlib.sha256).digest())
    request=Request(BASE+path,method=verb,headers=headers,data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request,timeout=20) as response: actual=response.status;raw=response.read()
    except HTTPError as error: actual=error.code;raw=error.read()
    data=json.loads(raw) if raw else None
    if status is not None: assert actual==status,(verb,path,actual,status,data)
    return actual,data

users=[]
for name in ("자동총무","자동둘째","자동셋째","자동넷째"):
    key=str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('AUTO_TEST','{key}','검증','{name}','FREE',UTC_TIMESTAMP())")
    users.append(int(sql(f"SELECT id FROM users WHERE provider_id='{key}'")))
host,second,third,fourth=users

def create(headcount):
    _,room=call("POST","/gatherings",host,{"headcount":headcount},201)
    unit=room["settlementUnits"][0]
    assert unit["headcount"]==headcount
    return room,unit,f"/gatherings/{room['id']}/settlement-units/{unit['id']}"

def add_round(root,payer,total=10000):
    return call("POST",root+"/rounds",host,{"total":total,"payerParticipantId":payer},201)[1]["id"]

def join(room,unit,user,answers=None):
    return call("POST","/join/"+room["shareToken"],user,{"settlementUnitId":unit["id"],"responses":answers or []})[1]["participantId"]

def respond(root,user,round_id,kind="SOBER",status=204):
    return call("PUT",root+"/responses/me",user,{"answers":[{"roundId":round_id,"type":kind}]},status)

room,unit,root=create(3)
host_id=room["me"]["participantId"]
round_id=add_round(root,host_id,184000)
answer=[{"roundId":round_id,"type":"SOBER"}]
second_id=join(room,unit,second,answer)
third_id=join(room,unit,third,answer)
fourth_id=join(room,unit,fourth,answer)
respond(root,fourth,round_id,"DRANK")
_,notifications=call("GET","/me/notifications",host)
check(sum(item["type"]=="HEADCOUNT_EXCEEDED" and item["settlementUnitId"]==unit["id"] for item in notifications)==1,"인원 초과 알림은 같은 초과 구간에서 1건")
other=call("POST",f"/gatherings/{room['id']}/settlement-units",fourth,{"requestId":str(uuid.uuid4()),"participantIds":[fourth_id,host_id]},201)[1]
with ThreadPoolExecutor(2) as pool:
    results=list(pool.map(lambda _:respond(root,host,round_id,status=None),range(2)))
check(sorted(result[0] for result in results)==[204,204],"마지막 응답 동시 2건 모두 저장하고 총무 확인 대기")
_,detail=call("GET",f"/gatherings/{room['id']}",host)
active=next(item for item in detail["settlementUnits"] if item["id"]==unit["id"])
check(active["status"]=="OPEN" and active["participantIds"]==[host_id,second_id,third_id,fourth_id],"전원 응답해도 초과 명단까지 보존하고 OPEN 유지")
check(sql(f"SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={unit['id']}")=="0","응답으로 스냅샷을 만들지 않음")
check(sql(f"SELECT COUNT(*) FROM round_responses WHERE round_id={round_id} AND participant_id={fourth_id}")=="1","목표 인원 밖 응답도 유지")
_,before=call("GET",root+"/settlement/preview",host)
call("DELETE",root+f"/participants/{fourth_id}",second,status=403)
call("DELETE",root+f"/participants/{fourth_id}",host,status=204)
check(call("POST",root+"/settlement",host,{"inputRevision":before["inputRevision"],"inputHash":before["inputHash"]},409)[1]["code"]=="SETTLEMENT_INPUT_CHANGED","제외 전 미리보기로 확정 불가")
_,preview=call("GET",root+"/settlement/preview",host)
check(len(preview["lines"])==3 and len(before["lines"])==4,"총무 제외 후 세 명으로 예상 금액 재계산")
request={"inputRevision":preview["inputRevision"],"inputHash":preview["inputHash"]}
call("POST",root+"/settlement",second,request,403)
with ThreadPoolExecutor(2) as pool:
    confirmations=list(pool.map(lambda _:call("POST",root+"/settlement",host,request,status=None),range(2)))
check(sorted(result[0] for result in confirmations)==[200,409],"동시 총무 확정은 한 번만 성공")
check(sql(f"SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={unit['id']}")=="1","명시적 확정 스냅샷 정확히 한 건")
_,detail=call("GET",f"/gatherings/{room['id']}",host)
check(any(item["id"]==fourth_id for item in detail["participants"]) and set(next(item for item in detail["settlementUnits"] if item["id"]==other["id"])["participantIds"])=={fourth_id,host_id},"공유 신원과 다른 단위 명단은 보존")
check(len([item for item in detail["transfers"] if item["settlementUnitId"]==unit["id"]])==2,"인원 밖 사람의 송금은 생성하지 않음")
check(any("184,000원" in item["body"] for item in detail["timeline"]),"새 타임라인 금액은 천 단위 쉼표")
created=datetime.fromisoformat(detail["timeline"][0]["createdAt"].replace("Z","+00:00"))
check(abs((datetime.now(timezone.utc)-created).total_seconds())<120,"Asia/Seoul JVM에서도 DATETIME 저장·조회·JSON은 실제 UTC")
_,notifications=call("GET","/me/notifications",host)
check(any(item["type"]=="SETTLED_HOST" and item["title"]=="계산이 끝났어요" for item in notifications),"마지막 응답자인 총무도 서버 계산 완료 알림 수신")
_,notifications=call("GET","/me/notifications",fourth)
check(any(item["type"]=="REMOVED" for item in notifications),"제외된 사람에게 내부 알림 기록")
call("DELETE",root+"/settlement",host,status=204)
check(sql(f"SELECT status FROM settlement_units WHERE id={unit['id']}")=="OPEN" and sql(f"SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={unit['id']}")=="0","되돌리기 직후 자동 재정산하지 않음")
respond(root,host,round_id)
check(sql(f"SELECT status FROM settlement_units WHERE id={unit['id']}")=="OPEN","같은 응답 재전송은 되돌린 단위를 다시 정산하지 않음")
respond(root,host,round_id,"DRANK")
check(sql(f"SELECT status FROM settlement_units WHERE id={unit['id']}")=="OPEN","되돌린 뒤 실제 응답 변경도 총무 확인 대기")

room,unit,root=create(3)
host_id=room["me"]["participantId"]
round_id=add_round(root,host_id,1)
join(room,unit,second,[{"roundId":round_id,"type":"SOBER"}]);join(room,unit,third,[{"roundId":round_id,"type":"SOBER"}])
respond(root,host,round_id)
_,detail=call("GET",f"/gatherings/{room['id']}",host)
check(detail["settlementUnits"][0]["autoSettlementError"]=="NEGATIVE_ADJUSTED_AMOUNT" and sql(f"SELECT COUNT(*) FROM round_responses WHERE round_id={round_id}")=="3","Core 실패는 응답을 보존하고 자동 확정을 보류")
call("PUT",root+f"/rounds/{round_id}",host,{"total":3,"payerParticipantId":host_id})
check(sql(f"SELECT status,auto_settlement_error IS NULL FROM settlement_units WHERE id={unit['id']}")=="OPEN\t1","차수 수정 후 오류를 지워도 OPEN 유지")

room,unit,root=create(2)
host_id=room["me"]["participantId"]
second_id=join(room,unit,second);third_id=join(room,unit,third)
round_id=add_round(root,third_id)
respond(root,host,round_id);respond(root,second,round_id)
check(sql(f"SELECT status,auto_settlement_error IS NULL FROM settlement_units WHERE id={unit['id']}")=="OPEN\t1","목표 인원 밖 결제자도 자동 제외하지 않음")
call("PUT",root+"/headcount",second,{"headcount":3},403)
call("PUT",root+"/headcount",host,{"headcount":3},204)
respond(root,third,round_id)
check(sql(f"SELECT status FROM settlement_units WHERE id={unit['id']}")=="OPEN","인원 수정과 결제자 응답 뒤에도 총무 확인 대기")

room,unit,root=create(4)
host_id=room["me"]["participantId"]
round_id=add_round(root,host_id)
join(room,unit,second);join(room,unit,third)
_,preview=call("GET",root+"/settlement/preview",host)
_,detail=call("POST",root+"/settlement",host,{"inputRevision":preview["inputRevision"],"inputHash":preview["inputHash"]})
check(len(detail["settlementUnits"][0]["participantIds"])==3 and sql(f"SELECT COUNT(*) FROM round_responses WHERE round_id={round_id} AND source='AUTO'")=="3","수동 예비 정산은 현재 명단 전원과 미응답 AUTO 유지")

room,unit,root=create(2)
host_id=room["me"]["participantId"]
round_id=add_round(root,host_id);empty_round=add_round(root,host_id)
join(room,unit,second,[{"roundId":round_id,"type":"SOBER"}]);respond(root,host,round_id)
call("DELETE",root+f"/rounds/{empty_round}",host,status=204)
check(sql(f"SELECT status FROM settlement_units WHERE id={unit['id']}")=="OPEN","빈 응답 차수 삭제 뒤에도 총무 확인 대기")
print(f"HOST-REVIEW/UTC/NOTIFICATION: {checks} checks passed")
