"""전용 검증 컨테이너의 v1 fixture를 Liquibase로 v4로 올리는 재현 절차.
DB 이름은 jeongsan_v4_upgrade로 고정한다. pre-v4 스키마가 먼저 필요하다.
"""
import subprocess,sys,json
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8")
root=Path.cwd().resolve()
assert (root/"server/src/main/resources/db/changelog/016-independent-settlement-units.yaml").exists()
jar=next((Path("C:/Users/kim/.gradle/caches/modules-2/files-2.1/com.mysql/mysql-connector-j/9.7.0")).rglob("*.jar"))
def sql(text):
    p=subprocess.run(["docker","exec","jeongsan-v4-verification","mysql","-uroot","-plocal-v4-test","--default-character-set=utf8mb4","-N","-s","jeongsan_v4_upgrade","-e",text],capture_output=True,text=True,encoding="utf-8")
    assert p.returncode==0,p.stderr
    return p.stdout.strip()
def update():
    return subprocess.run(["docker","run","--rm","-v",str(root/"server/src/main/resources")+":/liquibase/changelog:ro","-v",str(jar)+":/liquibase/lib/mysql.jar:ro","liquibase/liquibase","--search-path=/liquibase/changelog","--url=jdbc:mysql://host.docker.internal:13306/jeongsan_v4_upgrade?allowPublicKeyRetrieval=true&useSSL=false","--username=root","--password=local-v4-test","--changelog-file=db/changelog/db.changelog-master.yaml","update"],capture_output=True,text=True,encoding="utf-8")
assert sql("SELECT DATABASE()") == "jeongsan_v4_upgrade"
sql("""INSERT INTO users(id,provider,provider_id,nickname,display_name,created_at) VALUES(1,'TEST','one','테스트','김하나',UTC_TIMESTAMP()),(2,'TEST','two','테스트','김둘째',UTC_TIMESTAMP());
INSERT INTO gatherings(id,name,host_user_id,gathering_date,status,share_token,expected_count,created_at) VALUES(1,'옛 술자리',1,'2026-10-01','CONFIRMED','v4upgrade001',2,UTC_TIMESTAMP());
INSERT INTO participants(id,gathering_id,user_id,name,status,created_at) VALUES(1,1,2,'김둘째','JOINED',UTC_TIMESTAMP());
INSERT INTO rounds(id,gathering_id,seq,label,total_amount,alcohol_amount,payer_id) VALUES(1,1,3,'3차',10000,0,1);
INSERT INTO attendances(participant_id,round_id,attended,drank) VALUES(1,1,1,0);""")
p=update()
assert p.returncode!=0 and "precondition" in (p.stdout+p.stderr).lower(),"CONFIRMED v1은 새 DDL 전에 중단해야 함"
assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='settlement_units'")=="0"
print("PASS: v1 CONFIRMED 자료는 024 DDL 전에 중단",flush=True)
sql("UPDATE gatherings SET status='COLLECTING' WHERE id=1")
p=update()
assert p.returncode==0,p.stdout+p.stderr
assert sql("SELECT COUNT(*) FROM settlement_units WHERE gathering_id=1")=="1"
assert sql("SELECT COUNT(*) FROM participants WHERE gathering_id=1")=="2"
assert sql("SELECT status,next_round_seq FROM gatherings WHERE id=1")=="OPEN\t4"
assert sql("SELECT type,source FROM round_responses WHERE participant_id=1 AND round_id=1")=="SOBER\tSELF"
assert sql("SELECT COUNT(*) FROM settlement_unit_members WHERE gathering_id=1 AND status='ACTIVE'")=="2"
print("PASS: 기존 OPEN 입력을 초기 단위로 전환, 빠진 생성자 신원 추가·명단·SELF·다음 seq 보존",flush=True)
# 다른 술자리 사람을 같은 unit host에 넣으면 compound FK가 막는다.
sql("INSERT INTO gatherings(id,name,host_user_id,gathering_date,status,share_token,expected_count,created_at) VALUES(2,'다른 방',1,'2026-10-01','OPEN','v4upgrade002',1,UTC_TIMESTAMP()); INSERT INTO participants(id,gathering_id,user_id,name,status,created_at) VALUES(99,2,1,'김하나','ACTIVE',UTC_TIMESTAMP());")
p=subprocess.run(["docker","exec","jeongsan-v4-verification","mysql","-uroot","-plocal-v4-test","jeongsan_v4_upgrade","-e","INSERT INTO settlement_units(gathering_id,host_participant_id,created_at) VALUES(1,99,UTC_TIMESTAMP())"],capture_output=True,text=True)
assert p.returncode!=0 and "foreign key" in p.stderr.lower()
print("PASS: 다른 방의 총무 참조를 compound FK로 거절",flush=True)
assert update().returncode==0
print("PASS: 재실행에서 checksum 변경 없이 이미 적용한 changelog 통과",flush=True)
print(json.dumps({"upgradeChecks":4,"database":"jeongsan_v4_upgrade","liquibase":"5.0.4 CLI","applicationFreshMigration":"Spring Boot bundled Liquibase separately verified"}))
