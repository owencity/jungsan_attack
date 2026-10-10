"""전용 jeongsan-v4-verification / jeongsan_v4_test만 사용하는 재실행 가능한 실제 HTTP 검증.
실행 전 local 서버를 18080, DB를 13306으로 시작한다. 운영 URL·DB 이름을 받지 않는다.
"""
import base64, hashlib, hmac, json, subprocess, time, uuid
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from concurrent.futures import ThreadPoolExecutor

BASE="http://127.0.0.1:18080/api/v1"
CONTAINER="jeongsan-v4-verification"
DATABASE="jeongsan_v4_test"
checks=0
import sys
sys.stdout.reconfigure(encoding="utf-8")

def sql(query):
    result=subprocess.run(["docker","exec",CONTAINER,"mysql","--default-character-set=utf8mb4","-uroot","-plocal-v4-test","-N","-s",DATABASE,"-e",query],capture_output=True,text=True,encoding="utf-8")
    if result.returncode:raise AssertionError(result.stderr)
    return result.stdout.strip()
assert sql("SELECT DATABASE()") == DATABASE

def check(value,label):
    global checks
    assert value,label
    checks+=1
    print(f"PASS {checks}: {label}",flush=True)

def token(uid):
    def enc(value):return base64.urlsafe_b64encode(value).rstrip(b"=").decode()
    body=enc(b'{"alg":"HS256"}')+"."+enc(json.dumps({"sub":str(uid),"iat":int(time.time()),"exp":int(time.time())+3600}).encode())
    return body+"."+enc(hmac.new(b"local-only-dev-secret-please-change-32b",body.encode(),hashlib.sha256).digest())

def call(verb,path,uid=None,body=None,status=200,code=None):
    headers={"Content-Type":"application/json"}
    if uid is not None:headers["Cookie"]="jeongsan_token="+token(uid)
    request=Request(BASE+path,data=json.dumps(body,ensure_ascii=False).encode() if body is not None else None,headers=headers,method=verb)
    try:
        with urlopen(request,timeout=15) as response:actual=response.status;data=response.read()
    except HTTPError as error:actual=error.code;data=error.read()
    decoded=json.loads(data) if data else None
    assert actual==status,(verb,path,actual,status,decoded)
    if code:assert decoded["code"]==code,(path,decoded)
    return decoded

names=["김하나","김둘째","김셋째","김넷째"]
users=[]
for name in names:
    key=str(uuid.uuid4())
    sql(f"INSERT INTO users(provider,provider_id,nickname,display_name,tier,created_at) VALUES('V4_TEST','{key}','테스트','{name}','FREE',UTC_TIMESTAMP());")
    users.append(int(sql(f"SELECT id FROM users WHERE provider_id='{key}'")))
a,b,c,d=users
call("POST","/gatherings",status=401,code="UNAUTHENTICATED")
room=call("POST","/gatherings",a,status=201);gid=room["id"];share=room["shareToken"];unitA=room["settlementUnits"][0]["id"];pidA=room["me"]["participantId"]
check(room["status"]=="OPEN" and room["date"][:4]=="2026","방과 초기 단위를 생성하고 날짜는 ISO 문자열")
public=call("GET","/join/"+share)
check("participants" not in public and "responses" not in public and "payout" not in json.dumps(public),"공개 미리보기에서 명단·응답·계좌 제외")
pidB=call("POST","/join/"+share,b,{"settlementUnitId":unitA})["participantId"]
pidC=call("POST","/join/"+share,c,{"settlementUnitId":unitA})["participantId"]
key=str(uuid.uuid4());payload={"requestId":key,"participantIds":[pidB,pidA,pidC]}
root=f"/gatherings/{gid}/settlement-units"
with ThreadPoolExecutor(4) as pool:units=list(pool.map(lambda _:call("POST",root,b,payload,status=201),range(4)))
unitB=units[0]["id"];check(all(x["id"]==unitB for x in units),"추가 총무 생성 동시 재시도는 동일 단위 1개")
call("POST",root,b,{"requestId":key,"participantIds":[pidB]},409,"IDEMPOTENCY_KEY_REUSED")
ua=root+f"/{unitA}";ub=root+f"/{unitB}"
call("POST",ua+"/rounds",b,{"total":10000,"payerParticipantId":pidA},403,"NOT_SETTLEMENT_UNIT_HOST")
roundA=call("POST",ua+"/rounds",a,{"total":10000,"payerParticipantId":pidA},201)
roundB=call("POST",ub+"/rounds",b,{"total":9000,"payerParticipantId":pidB},201)
check(roundA["seq"]==1 and roundB["seq"]==2,"차수는 동일 술자리 전체에서 채번")
call("PUT",ua+"/rounds/"+str(roundB["id"]),a,{"total":10000,"payerParticipantId":pidA},404,"ROUND_NOT_FOUND")
preview=call("GET",ua+"/settlement/preview",a)
call("PUT",ua+"/responses/me",c,{"answers":[{"roundId":roundA["id"],"type":"SOBER"}]},204)
call("POST",ua+"/settlement",a,{"inputRevision":preview["inputRevision"],"inputHash":preview["inputHash"]},409,"SETTLEMENT_INPUT_CHANGED")
check(sql(f"SELECT COUNT(*) FROM settlements WHERE gathering_id={gid}")=="0","바뀐 미리보기 거절은 스냅샷을 저장하지 않음")
rev=call("GET",ua+"/settlement/preview",a)["inputRevision"]
call("PUT",ua+"/responses/me",c,{"answers":[{"roundId":roundA["id"],"type":"ABSENT"},{"roundId":roundB["id"],"type":"ABSENT"}]},404,"ROUND_NOT_FOUND")
check(call("GET",ua+"/settlement/preview",a)["inputRevision"]==rev and sql(f"SELECT type FROM round_responses WHERE participant_id={pidC} AND round_id={roundA['id']}")=="SOBER","여러 응답 중 다른 단위 차수가 있으면 먼저 쓴 응답과 revision도 롤백")
preview=call("GET",ua+"/settlement/preview",a)
room=call("POST",ua+"/settlement",a,{"inputRevision":preview["inputRevision"],"inputHash":preview["inputHash"]})
check({x["id"]:x["status"] for x in room["settlementUnits"]}=={unitA:"SETTLING",unitB:"OPEN"},"A 정산 후 B는 OPEN 유지")
call("POST",ua+"/settlement/viewed",c,status=204)
check(sql(f"SELECT COUNT(*) FROM round_responses WHERE round_id={roundA['id']} AND source='AUTO'")=="2","미응답 2칸만 AUTO를 저장")
call("DELETE",ua+"/settlement",a,status=204)
check(sql(f"SELECT COUNT(*) FROM round_responses WHERE round_id={roundA['id']}")=="1" and sql(f"SELECT COUNT(*) FROM settlement_unit_members WHERE settlement_unit_id={unitA} AND settlement_viewed_at IS NOT NULL")=="0","되돌리기는 AUTO·열람만 지우고 SELF를 보존")
with ThreadPoolExecutor(2) as pool:
    def settle(item):
        url,host=item;pv=call("GET",url+"/settlement/preview",host)
        return call("POST",url+"/settlement",host,{"inputRevision":pv["inputRevision"],"inputHash":pv["inputHash"]})
    list(pool.map(settle,[(ua,a),(ub,b)]))
room=call("GET",f"/gatherings/{gid}",a)
check(room["status"]=="SETTLING" and all(x["status"]=="SETTLING" for x in room["settlementUnits"]),"A·B 동시 정산이 교착 없이 성공하고 최신 요약 유지")
check(all(sum(x["amount"] for x in t["basis"])==t["amount"] for t in room["transfers"]),"모든 송금의 차수 근거 합이 송금액과 일치")
t=next(x for x in room["transfers"] if x["settlementUnitId"]==unitA and x["fromParticipantId"]==pidB)
call("POST",f"/transfers/{t['id']}/sent",b,status=409,code="PAYOUT_MISSING")
call("PUT","/users/me/payout",a,{"bank":"카카오뱅크","accountNo":"3333-123456789","holder":"김하나"})
check("3333313233343536373839" not in sql(f"SELECT HEX(payout_encrypted) FROM users WHERE id={a}"),"DB는 원문 대신 AES-GCM 암호문 저장")
call("POST",f"/transfers/{t['id']}/sent",c,status=403,code="NOT_TRANSFER_OWNER")
call("POST",f"/transfers/{t['id']}/sent",b,status=204)
call("POST",f"/transfers/{t['id']}/not-received",a,status=204)
call("DELETE",ua+"/settlement",a,status=409,code="TRANSFER_ALREADY_SENT")
check(sql(f"SELECT status, sent_at IS NOT NULL FROM settlement_transfers WHERE id={t['id']}")=="WAITING\t1","미수취로 돌아가도 보낸 이력 보존·되돌리기 금지")
# 실제 입금은 보냈어요 여부와 무관하게 수취인이 확인할 수 있다.
for tr in room["transfers"]:
    recipient=a if tr["toParticipantId"]==pidA else b
    call("POST",f"/transfers/{tr['id']}/confirm",recipient,status=204)
room=call("GET",f"/gatherings/{gid}",a)
check(room["status"]=="COMPLETED" and room["deleteScheduledAt"] is not None,"모든 단위 완료 후 7일 삭제 예약")
call("POST",f"/transfers/{t['id']}/confirm",a,status=204)
call("GET",f"/gatherings/{gid}",d,status=403,code="NOT_PARTICIPANT")
# 완료 A·B의 명단이나 snapshot을 바꾸지 않고 늦게 들어온 새 사람을 C에만 추가.
uc=call("POST",root,b,{"requestId":str(uuid.uuid4()),"participantIds":[pidB,pidA]},201)["id"]
pidD=call("POST","/join/"+share,d,{"settlementUnitId":uc})["participantId"]
room=call("GET",f"/gatherings/{gid}",d)
check(pidD not in next(x for x in room["settlementUnits"] if x["id"]==unitA)["participantIds"] and room["deleteScheduledAt"] is None,"새 총무 단위의 늦은 참여는 완료 A의 분모를 바꾸지 않고 삭제 예약 해제")
check(next(x for x in room["participants"] if x["id"]==pidA)["payout"] is None,"송금 없는 새 참여자에게 A의 계좌 비공개")
url=root+f"/{uc}"
call("POST",url+"/rounds",b,{"total":1,"payerParticipantId":pidB},201)
call("GET",url+"/settlement/preview",b,status=409,code="VALIDATION_FAILED")
check(sql(f"SELECT COUNT(*) FROM settlements WHERE settlement_unit_id={uc}")=="0","Core 오류는 HTTP 409로 변환하고 AUTO·snapshot을 저장하지 않음")
# 명단 제외는 해당 단위에만 적용, 호스트·결제자 제외 보호.
call("DELETE",url+f"/participants/{pidB}",b,status=409,code="REMOVE_HOST")
call("DELETE",url+f"/participants/{pidD}",b,status=204)
call("POST","/join/"+share,d,{"settlementUnitId":uc},403,"NOT_SETTLEMENT_UNIT_MEMBER")
call("PUT",url+f"/participants/{pidD}",b,status=204)
rs=next(x for x in call("GET",f"/gatherings/{gid}",b)["rounds"] if x["settlementUnitId"]==uc)
call("PUT",url+f"/rounds/{rs['id']}",b,{"total":100,"payerParticipantId":pidB})
settle((url,b));call("POST",url+"/complete",b,status=204)
room=call("GET",f"/gatherings/{gid}",b)
check(room["status"]=="COMPLETED" and any(t["status"]=="WAITING" for t in room["transfers"]),"수동 완료는 미확인 송금을 CONFIRMED로 위조하지 않음")
call("POST",f"/gatherings/{gid}/messages",c,{"text":"확인했습니다"},201)
check(len(call("GET","/me/gatherings",a))>=1,"내 목록은 상세와 동일한 배열")
notifications=call("GET","/me/notifications",b)
check(len(notifications)>0 and "T" in notifications[0]["createdAt"],"알림과 UTC 시각 제공")
call("POST","/me/notifications/read-all",b,status=204)
check(call("GET","/auth/me",b)["unreadNotificationCount"]==0,"알림 일괄 읽음과 내 정보 안 읽은 수 일치")
inactive=call("POST","/gatherings",d,status=201)["id"]
sql(f"UPDATE gatherings SET last_activity_at=UTC_TIMESTAMP()-INTERVAL 31 DAY WHERE id={inactive}")
# 이 스크립트가 만든 방 하나만 삭제 후보로 바꾼다. 앱의 실제 배치가 FK 순서대로 삭제한다.
sql(f"UPDATE gatherings SET delete_scheduled_at=UTC_TIMESTAMP()-INTERVAL 1 DAY WHERE id={gid}")
for _ in range(35):
    if sql(f"SELECT COUNT(*) FROM gatherings WHERE id={gid}")=="0":break
    time.sleep(2)
check(sql(f"SELECT COUNT(*) FROM gatherings WHERE id={gid}")=="0","실제 삭제 배치가 완료 술자리 제거")
check(sql(f"SELECT COUNT(*) FROM participants WHERE gathering_id={gid}")=="0" and sql(f"SELECT COUNT(*) FROM notifications WHERE gathering_id={gid}")=="0","술자리 명단·알림 개인정보 함께 삭제")
check(sql(f"SELECT COUNT(*) FROM gatherings WHERE id={inactive}")=="0","실제 삭제 배치가 30일 미활동 OPEN 방도 제거")
check(sql(f"SELECT COUNT(*) FROM users WHERE id={a}")=="1","술자리 삭제가 사용자 계정을 삭제하지 않음")
print(json.dumps({"checks":checks,"http":"real Spring Boot","db":"MySQL 8.4","isolatedDatabase":DATABASE}),flush=True)
