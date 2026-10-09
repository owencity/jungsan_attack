package app.jeongsan.server.gathering

import app.jeongsan.core.Validator
import app.jeongsan.core.ValidationPhase
import app.jeongsan.server.common.ShareToken
import app.jeongsan.server.gathering.SettlementWorkflow.fail
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.security.MessageDigest

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class GatheringService(private val db: GatheringStore, private val mapper: ObjectMapper, private val cipher: PayoutCipher) {
    private fun user(uid: Long): Map<String, Any?> = db.rows("SELECT * FROM users WHERE id=:id", "id" to uid).singleOrNull()
        ?: fail("UNAUTHENTICATED", status=HttpStatus.UNAUTHORIZED)
    private fun named(uid: Long): Map<String, Any?> = user(uid).also { if (it["display_name"] == null) fail("DISPLAY_NAME_REQUIRED") }
    private fun room(gid: Long, lock: Boolean = false): Map<String, Any?> = db.rows("SELECT * FROM gatherings WHERE id=:id" + if(lock) " FOR UPDATE" else "", "id" to gid)
        .singleOrNull() ?: fail("GATHERING_NOT_FOUND", status=HttpStatus.NOT_FOUND)
    private fun person(gid: Long, uid: Long): PersonInput {
        user(uid)
        val p = db.rows("SELECT p.id,p.user_id,u.display_name,p.name FROM participants p JOIN users u ON u.id=p.user_id WHERE p.gathering_id=:g AND p.user_id=:u AND p.status='ACTIVE'", "g" to gid,"u" to uid)
            .singleOrNull() ?: fail("NOT_PARTICIPANT",status=HttpStatus.FORBIDDEN)
        return PersonInput(p.long("id"),uid,(p["display_name"] ?: p["name"]).toString())
    }
    private fun unit(gid: Long, id: Long, lock: Boolean = true): UnitInput {
        val row=db.rows("SELECT * FROM settlement_units WHERE id=:id AND gathering_id=:g"+if(lock) " FOR UPDATE" else "", "id" to id,"g" to gid)
            .singleOrNull() ?: fail("SETTLEMENT_UNIT_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        val ids=db.rows("SELECT participant_id FROM settlement_unit_members WHERE settlement_unit_id=:id AND status='ACTIVE' ORDER BY participant_id","id" to id).map{it.long("participant_id")}
        return UnitInput(id,row.long("host_participant_id"),row.text("status"),row.long("input_revision"),ids)
    }
    private fun authorize(gid: Long, id: Long, uid: Long, edit: Boolean=true): UnitInput = unit(gid,id).also {
        SettlementWorkflow.host(it,person(gid,uid)); if(edit) SettlementWorkflow.open(it)
    }
    private fun data(gid: Long): Triple<List<PersonInput>,List<RoundInput>,List<ResponseInput>> {
        val people=db.rows("SELECT p.id,COALESCE(p.user_id,0) user_id,COALESCE(u.display_name,p.name) name FROM participants p LEFT JOIN users u ON p.user_id=u.id WHERE p.gathering_id=:g","g" to gid)
            .map{PersonInput(it.long("id"),it.long("user_id"),it.text("name"))}
        val drinks=db.rows("SELECT d.* FROM drink_items d JOIN rounds r ON r.id=d.round_id WHERE r.gathering_id=:g ORDER BY d.id","g" to gid).groupBy{it.long("round_id")}
        val rounds=db.rows("SELECT * FROM rounds WHERE gathering_id=:g ORDER BY seq","g" to gid).map{r->RoundInput(r.long("id"),r.long("settlement_unit_id"),r.long("seq").toInt(),r.long("total_amount"),r.long("payer_id"),
            drinks[r.long("id")].orEmpty().map{DrinkInput(it.text("name"),it.long("unit_price"),it.long("bottle_count").toInt())})}
        val responses=db.rows("SELECT a.* FROM round_responses a JOIN rounds r ON r.id=a.round_id WHERE r.gathering_id=:g","g" to gid)
            .map{ResponseInput(it.long("participant_id"),it.long("round_id"),it.text("type"),it.text("source"))}
        return Triple(people,rounds,responses)
    }
    private fun bump(gid: Long, id: Long) { db.update("UPDATE settlement_units SET input_revision=input_revision+1 WHERE id=:id","id" to id); touch(gid) }
    private fun touch(gid: Long) { db.update("UPDATE gatherings SET last_activity_at=:now WHERE id=:g","now" to Instant.now(),"g" to gid) }
    private fun event(gid: Long, unitId: Long?, code: String, body: String) {
        db.insert("INSERT INTO timeline_entries(gathering_id,settlement_unit_id,type,system_code,body,created_at) VALUES(:g,:s,'SYSTEM',:c,:b,:n)","g" to gid,"s" to unitId,"c" to code,"b" to body,"n" to Instant.now())
    }
    private fun notify(gid: Long, unitId: Long?, type: String, body: String, userIds: List<Long>, actor: Long) {
        userIds.distinct().filter{it > 0 && it != actor}.forEach { db.insert("INSERT INTO notifications(user_id,gathering_id,settlement_unit_id,type,title,body,created_at) VALUES(:u,:g,:s,:t,:t,:b,:n)","u" to it,"g" to gid,"s" to unitId,"t" to type,"b" to body,"n" to Instant.now()) }
    }
    private fun updateSummary(gid: Long) {
        room(gid,true)
        val all=db.rows("SELECT status,completed_at FROM settlement_units WHERE gathering_id=:g","g" to gid)
        val state=SettlementWorkflow.summary(all.map{it.text("status")})
        val completed=if(state=="COMPLETED") all.mapNotNull{it.instant("completed_at")}.maxOrNull() else null
        db.update("UPDATE gatherings SET status=:s,completed_at=:c,delete_scheduled_at=:d,last_activity_at=:n WHERE id=:g","s" to state,"c" to completed,"d" to completed?.plusSeconds(7*86400L),"n" to Instant.now(),"g" to gid)
    }
    fun create(uid: Long): Map<String,Any?> {
        if(db.rows("SELECT id FROM users WHERE id=:u FOR SHARE","u" to uid).isEmpty()) fail("UNAUTHENTICATED",status=HttpStatus.UNAUTHORIZED)
        val u=named(uid); val now=Instant.now(); val date=LocalDate.now(ZoneId.of("Asia/Seoul"))
        val gid=db.insert("INSERT INTO gatherings(name,host_user_id,gathering_date,status,share_token,expected_count,rounding_unit,revision,created_at,last_activity_at) VALUES(:t,:u,:d,'OPEN',:k,0,1,0,:n,:n)","t" to "${date.monthValue}/${date.dayOfMonth} 술자리","u" to uid,"d" to date,"k" to ShareToken.generate(),"n" to now)
        val pid=db.insert("INSERT INTO participants(gathering_id,user_id,name,status,created_at) VALUES(:g,:u,:name,'ACTIVE',:n)","g" to gid,"u" to uid,"name" to u.text("display_name"),"n" to now)
        val sid=db.insert("INSERT INTO settlement_units(gathering_id,host_participant_id,created_at) VALUES(:g,:p,:n)","g" to gid,"p" to pid,"n" to now)
        addMember(gid,sid,pid);event(gid,sid,"CREATED","${u.text("display_name")}님이 술자리를 만들었어요")
        return detail(gid,uid)
    }
    fun edit(gid: Long, uid: Long, request: GatheringEditRequest): Map<String,Any?> {
        person(gid,uid);val row=room(gid,true)
        if((row["host_user_id"] as? Number)?.toLong()!=uid) fail("NOT_GATHERING_CREATOR",status=HttpStatus.FORBIDDEN)
        val title=request.title?.trim() ?: row.text("name")
        if(title.isEmpty()||title.codePointCount(0,title.length)>20) fail("MALFORMED_REQUEST",status=HttpStatus.BAD_REQUEST)
        db.update("UPDATE gatherings SET name=:t,gathering_date=:d WHERE id=:g","t" to title,"d" to (request.date ?: row["gathering_date"]),"g" to gid);touch(gid)
        return detail(gid,uid)
    }
    fun createUnit(gid: Long, uid: Long, request: UnitCreateRequest): Map<String,Any?> {
        named(uid);val p=person(gid,uid);room(gid,true);named(uid)
        val ids=SettlementWorkflow.creationRoster(request.participantIds,p.id)
        val hash=MessageDigest.getInstance("SHA-256").digest(ids.joinToString(",").toByteArray()).joinToString(""){"%02x".format(it)}
        val previous=db.rows("SELECT * FROM settlement_unit_requests WHERE gathering_id=:g AND user_id=:u AND request_id=:r","g" to gid,"u" to uid,"r" to request.requestId.toString()).singleOrNull()
        if(previous!=null) { if(previous.text("payload_hash")!=hash) fail("IDEMPOTENCY_KEY_REUSED");return unitView(gid,previous.long("settlement_unit_id"),p.id) }
        val valid=db.rows("SELECT id FROM participants WHERE gathering_id=:g AND status='ACTIVE' AND user_id IS NOT NULL","g" to gid).map{it.long("id")}.toSet()
        if(!valid.containsAll(ids)) fail("NOT_PARTICIPANT",status=HttpStatus.FORBIDDEN)
        val id=db.insert("INSERT INTO settlement_units(gathering_id,host_participant_id,created_at) VALUES(:g,:p,:n)","g" to gid,"p" to p.id,"n" to Instant.now())
        ids.forEach{addMember(gid,id,it)}
        db.update("INSERT INTO settlement_unit_requests VALUES(:g,:u,:r,:h,:s)","g" to gid,"u" to uid,"r" to request.requestId.toString(),"h" to hash,"s" to id)
        db.update("UPDATE gatherings SET status='OPEN',completed_at=NULL,delete_scheduled_at=NULL,last_activity_at=:n WHERE id=:g","n" to Instant.now(),"g" to gid)
        event(gid,id,"UNIT_CREATED","${p.name}님이 추가 차수의 총무가 되었어요")
        return unitView(gid,id,p.id)
    }
    private fun addMember(gid: Long,id: Long,pid: Long) {
        db.update("INSERT INTO settlement_unit_members(settlement_unit_id,gathering_id,participant_id,status,joined_at) VALUES(:s,:g,:p,'ACTIVE',:n) ON DUPLICATE KEY UPDATE status='ACTIVE'","s" to id,"g" to gid,"p" to pid,"n" to Instant.now())
    }
    fun member(gid: Long,id: Long,uid: Long,pid: Long,remove: Boolean) {
        val u=authorize(gid,id,uid)
        if(db.rows("SELECT id FROM participants WHERE id=:p AND gathering_id=:g AND status='ACTIVE' AND user_id IS NOT NULL","p" to pid,"g" to gid).isEmpty()) fail("NOT_PARTICIPANT",status=HttpStatus.FORBIDDEN)
        if((pid in u.participantIds) == !remove) return
        if(remove) { SettlementWorkflow.remove(u,pid,data(gid).second);db.update("UPDATE settlement_unit_members SET status='REMOVED' WHERE settlement_unit_id=:s AND participant_id=:p","s" to id,"p" to pid) }
        else addMember(gid,id,pid)
        bump(gid,id)
    }
    fun saveRound(gid: Long,id: Long,uid: Long,rid: Long?,request: RoundRequest): Map<String,Any?> {
        val u=authorize(gid,id,uid);SettlementWorkflow.member(u,request.payerParticipantId)
        val loaded=data(gid)
        if(rid!=null && loaded.second.none{it.id==rid&&it.unitId==id}) fail("ROUND_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        room(gid,true)
        val seq=if(rid==null) room(gid).long("next_round_seq").toInt() else loaded.second.first{it.id==rid}.seq
        val candidate=RoundInput(rid ?: -1,id,seq,request.total,request.payerParticipantId,request.drinks.map{DrinkInput(it.name.trim(),it.unitPrice,it.quantity)})
        val replaced=loaded.second.filterNot{it.id==rid}+candidate
        val input=SettlementWorkflow.input(u,loaded.first,replaced,loaded.third)
        val errors=Validator.validate(input,ValidationPhase.SAVE); if(errors.isNotEmpty()) throw SettlementValidationException(errors)
        val roundId=if(rid==null) {
            db.update("UPDATE gatherings SET next_round_seq=next_round_seq+1 WHERE id=:g","g" to gid)
            db.insert("INSERT INTO rounds(gathering_id,settlement_unit_id,seq,label,total_amount,alcohol_amount,payer_id) VALUES(:g,:s,:seq,:label,:total,0,:p)","g" to gid,"s" to id,"seq" to seq,"label" to "${seq}차","total" to request.total,"p" to request.payerParticipantId)
        } else { db.update("UPDATE rounds SET total_amount=:total,payer_id=:p WHERE id=:r","total" to request.total,"p" to request.payerParticipantId,"r" to rid);rid }
        db.update("DELETE FROM drink_items WHERE round_id=:r","r" to roundId)
        request.drinks.forEach{db.insert("INSERT INTO drink_items(round_id,name,bottle_count,unit_price) VALUES(:r,:name,:q,:price)","r" to roundId,"name" to it.name.trim(),"q" to it.quantity,"price" to it.unitPrice)}
        bump(gid,id);event(gid,id,"ROUND_SAVED","${seq}차 ${request.total}원을 넣었어요")
        return roundView(data(gid).second.first{it.id==roundId})
    }
    fun deleteRound(gid: Long,id: Long,uid: Long,rid: Long) {
        authorize(gid,id,uid)
        if(db.rows("SELECT id FROM rounds WHERE id=:r AND settlement_unit_id=:s","r" to rid,"s" to id).isEmpty()) fail("ROUND_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        db.update("DELETE FROM drink_items WHERE round_id=:r","r" to rid);db.update("DELETE FROM attendances WHERE round_id=:r","r" to rid)
        db.update("DELETE FROM rounds WHERE id=:r","r" to rid);bump(gid,id);event(gid,id,"ROUND_DELETED","차수를 지웠어요")
    }
    fun respond(gid: Long,id: Long,uid: Long,pid: Long?,request: ResponseRequest) {
        val u=unit(gid,id);val actor=person(gid,uid);val target=pid ?: actor.id
        if(pid!=null) SettlementWorkflow.host(u,actor)
        val loaded=data(gid);val rounds=loaded.second
        val previous=loaded.third.filter{it.participantId==target}.associateBy{it.roundId}
        if(request.answers.map{it.roundId}.distinct().size!=request.answers.size) fail("MALFORMED_REQUEST",status=HttpStatus.BAD_REQUEST)
        request.answers.forEach { a -> SettlementWorkflow.response(u,target,a.roundId,a.type,rounds)
            val old=previous[a.roundId]
            if(pid==null && old?.type=="EXEMPT" && old.source=="HOST") return@forEach
            db.update("INSERT INTO round_responses(participant_id,round_id,type,source) VALUES(:p,:r,:t,:src) ON DUPLICATE KEY UPDATE type=VALUES(type),source=VALUES(source)","p" to target,"r" to a.roundId,"t" to a.type,"src" to if(pid==null) "SELF" else "HOST") }
        bump(gid,id);event(gid,id,"RESPONDED","${actor.name}님이 응답했어요")
    }
    @Transactional(readOnly=true,isolation=Isolation.READ_COMMITTED)
    fun joinPreview(token: String): Map<String,Any?> {
        val row=db.rows("SELECT * FROM gatherings WHERE share_token=:t","t" to token).singleOrNull() ?: fail("GATHERING_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        val gid=row.long("id");val all=data(gid)
        return mapOf("title" to row["name"],"date" to (row["gathering_date"] as java.sql.Date).toLocalDate(),"status" to row["status"],"participantCount" to all.first.size,
            "settlementUnits" to db.rows("SELECT s.*,COALESCE(u.display_name,p.name) display_name,COALESCE(u.spoon_count,0) spoon_count FROM settlement_units s JOIN participants p ON p.id=s.host_participant_id LEFT JOIN users u ON u.id=p.user_id WHERE s.gathering_id=:g ORDER BY s.id","g" to gid).map{s->
                mapOf("id" to s["id"],"status" to s["status"],"host" to mapOf("displayName" to s["display_name"],"spoonCount" to s["spoon_count"]),"rounds" to all.second.filter{it.unitId==s.long("id")}.map{mapOf("id" to it.id,"seq" to it.seq,"total" to it.total)})})
    }
    fun join(token: String,uid: Long,request: JoinRequest): Map<String,Long> {
        val usr=named(uid);val gid=db.rows("SELECT id FROM gatherings WHERE share_token=:t","t" to token).singleOrNull()?.long("id") ?: fail("GATHERING_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        val u=unit(gid,request.settlementUnitId);SettlementWorkflow.open(u);room(gid,true);named(uid)
        val old=db.rows("SELECT * FROM participants WHERE gathering_id=:g AND user_id=:u","g" to gid,"u" to uid).singleOrNull()
        val pid=old?.long("id") ?: db.insert("INSERT INTO participants(gathering_id,user_id,name,status,created_at) VALUES(:g,:u,:name,'ACTIVE',:n)","g" to gid,"u" to uid,"name" to usr.text("display_name"),"n" to Instant.now())
        val membership=db.rows("SELECT status FROM settlement_unit_members WHERE settlement_unit_id=:s AND participant_id=:p","s" to u.id,"p" to pid).singleOrNull()
        if(membership?.text("status")=="REMOVED") fail("NOT_SETTLEMENT_UNIT_MEMBER",status=HttpStatus.FORBIDDEN)
        if(membership!=null) return mapOf("gatheringId" to gid,"participantId" to pid)
        addMember(gid,u.id,pid)
        respond(gid,u.id,uid,null,ResponseRequest(request.responses))
        event(gid,u.id,"JOINED","${usr.text("display_name")}님이 들어왔어요")
        notify(gid,u.id,"JOINED","${usr.text("display_name")}님이 들어왔어요",data(gid).first.filter{it.id==u.hostId}.map{it.userId},uid)
        return mapOf("gatheringId" to gid,"participantId" to pid)
    }
    fun preview(gid: Long,id: Long,uid: Long): Preview {
        val u=authorize(gid,id,uid,false);val d=data(gid)
        return SettlementWorkflow.preview(u,d.first,d.second,d.third)
    }
    fun settle(gid: Long,id: Long,uid: Long,request: SettleRequest): Map<String,Any?> {
        val u=authorize(gid,id,uid);val d=data(gid);val preview=SettlementWorkflow.preview(u,d.first,d.second,d.third)
        if(preview.inputRevision!=request.inputRevision||preview.inputHash!=request.inputHash) fail("SETTLEMENT_INPUT_CHANGED")
        // 타임라인·알림 FK가 Gathering 공유 잠금을 먼저 잡으면 두 unit이 X 잠금으로 승격할 때 교착한다.
        // Core 검증 뒤, FK를 포함한 쓰기 전에 unit→Gathering 순서로 배타 잠금을 얻는다.
        room(gid,true)
        val input=SettlementWorkflow.input(u,d.first,d.second,d.third)
        input.attendance.forEach{(key,value)->db.update("INSERT IGNORE INTO round_responses(participant_id,round_id,type,source) VALUES(:p,:r,:t,'AUTO')","p" to key.participantId,"r" to key.roundId,"t" to value.name)}
        val sid=db.insert("INSERT INTO settlements(gathering_id,settlement_unit_id,input_revision,input_hash,grand_total,settled_at) VALUES(:g,:s,:rev,:h,:total,:n)","g" to gid,"s" to id,"rev" to u.revision,"h" to preview.inputHash,"total" to preview.grandTotal,"n" to Instant.now())
        preview.transfers.forEach{t->
            val tid=db.insert("INSERT INTO settlement_transfers(settlement_id,sender_participant_id,recipient_participant_id,amount) VALUES(:s,:f,:t,:a)","s" to sid,"f" to t.from,"t" to t.to,"a" to t.amount)
            t.basis.forEach{b->db.update("INSERT INTO settlement_transfer_items VALUES(:t,:r,:type,:a)","t" to tid,"r" to b.roundId,"type" to b.type,"a" to b.amount)}
        }
        val state=if(preview.transfers.isEmpty()) "COMPLETED" else "SETTLING"
        db.update("UPDATE settlement_units SET status=:state,completed_at=:n WHERE id=:id","state" to state,"n" to if(state=="COMPLETED") Instant.now() else null,"id" to id)
        event(gid,id,"SETTLED","${person(gid,uid).name}님이 담당 차수를 정산했어요")
        notify(gid,id,"SETTLED","정산이 나왔어요! 입금액을 확인해주세요",d.first.filter{it.id in u.participantIds}.map{it.userId},uid)
        preview.lines.filter{it.auto}.forEach{line->notify(gid,id,"AUTO_RESPONDED","응답이 없어 참석·알코올로 계산됐어요",d.first.filter{it.id==line.participantId||it.id==u.hostId}.map{it.userId},uid)}
        updateSummary(gid);return detail(gid,uid)
    }
    fun reopen(gid: Long,id: Long,uid: Long) {
        val u=authorize(gid,id,uid,false);if(u.state!="SETTLING") fail("SETTLEMENT_UNIT_NOT_SETTLING")
        val sent=db.rows("SELECT t.id FROM settlement_transfers t JOIN settlements s ON s.id=t.settlement_id WHERE s.settlement_unit_id=:s AND (t.sent_at IS NOT NULL OR t.confirmed_at IS NOT NULL)","s" to id)
        if(sent.isNotEmpty()) fail("TRANSFER_ALREADY_SENT")
        room(gid,true)
        db.update("DELETE a FROM round_responses a JOIN rounds r ON r.id=a.round_id WHERE r.settlement_unit_id=:s AND a.source='AUTO'","s" to id)
        db.update("DELETE FROM settlements WHERE settlement_unit_id=:s","s" to id)
        db.update("UPDATE settlement_unit_members SET settlement_viewed_at=NULL WHERE settlement_unit_id=:s","s" to id)
        db.update("UPDATE settlement_units SET status='OPEN',completed_at=NULL,input_revision=input_revision+1 WHERE id=:s","s" to id)
        event(gid,id,"SETTLEMENT_REVERTED","정산을 되돌렸어요");updateSummary(gid)
        notify(gid,id,"SETTLEMENT_REVERTED","정산이 되돌려졌어요",data(gid).first.filter{it.id in u.participantIds}.map{it.userId},uid)
    }
    fun viewed(gid: Long,id: Long,uid: Long) {
        val u=unit(gid,id);val p=person(gid,uid);SettlementWorkflow.member(u,p.id)
        if(u.state=="OPEN") fail("SETTLEMENT_UNIT_NOT_SETTLING")
        db.update("UPDATE settlement_unit_members SET settlement_viewed_at=COALESCE(settlement_viewed_at,:n) WHERE settlement_unit_id=:s AND participant_id=:p","n" to Instant.now(),"s" to id,"p" to p.id)
    }
    fun complete(gid: Long,id: Long,uid: Long) {
        val u=authorize(gid,id,uid,false);if(u.state=="COMPLETED") return
        if(u.state!="SETTLING") fail("SETTLEMENT_UNIT_NOT_SETTLING")
        markComplete(gid,id,uid)
    }
    private fun markComplete(gid: Long,id: Long,uid: Long) {
        room(gid,true)
        db.update("UPDATE settlement_units SET status='COMPLETED',completed_at=:n WHERE id=:s","n" to Instant.now(),"s" to id)
        event(gid,id,"COMPLETED","담당 차수의 정산이 완료됐어요");updateSummary(gid)
        val d=data(gid);val u=unit(gid,id,false)
        notify(gid,id,"COMPLETED","정산 완료!",d.first.filter{it.id in u.participantIds}.map{it.userId},uid)
    }
    fun transfer(tid: Long,uid: Long,action: String) {
        user(uid)
        val row=db.rows("SELECT t.*,s.gathering_id,s.settlement_unit_id FROM settlement_transfers t JOIN settlements s ON s.id=t.settlement_id WHERE t.id=:t","t" to tid).singleOrNull() ?: fail("TRANSFER_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        val gid=row.long("gathering_id");val u=unit(gid,row.long("settlement_unit_id"));val p=person(gid,uid)
        val fresh=db.rows("SELECT * FROM settlement_transfers WHERE id=:t","t" to tid).singleOrNull() ?: fail("TRANSFER_NOT_FOUND",status=HttpStatus.NOT_FOUND)
        val account=db.rows("SELECT u.payout_encrypted FROM participants p LEFT JOIN users u ON u.id=p.user_id WHERE p.id=:p","p" to fresh.long("recipient_participant_id")).single()["payout_encrypted"]!=null
        val state=SettlementWorkflow.transfer(fresh.text("status"),action,p.id,fresh.long("sender_participant_id"),fresh.long("recipient_participant_id"),u.state,account)
        if(state==fresh.text("status")) return
        room(gid,true)
        val column=when(action){"sent"->"sent_at";"confirm"->"confirmed_at";else->"not_received_at"}
        val stamp=if(action=="not-received") ":n" else "COALESCE($column,:n)"
        db.update("UPDATE settlement_transfers SET status=:s,$column=$stamp WHERE id=:t","s" to state,"n" to Instant.now(),"t" to tid)
        val code=when(action){"sent"->"SENT";"confirm"->"CONFIRMED";else->"NOT_RECEIVED"}
        event(gid,u.id,code,"${p.name}님이 ${when(action){"sent"->"보냈어요";"confirm"->"입금을 확인했어요";else->"아직 입금을 확인하지 못했어요"}}")
        val target=if(action=="sent") fresh.long("recipient_participant_id") else fresh.long("sender_participant_id")
        notify(gid,u.id,code,"송금 상태가 바뀌었어요",data(gid).first.filter{it.id==target}.map{it.userId},uid)
        if(action=="confirm"&&db.rows("SELECT id FROM settlement_transfers WHERE settlement_id=:s AND status<>'CONFIRMED'","s" to fresh.long("settlement_id")).isEmpty()) markComplete(gid,u.id,uid) else touch(gid)
    }
    private fun roundView(r: RoundInput)=mapOf("id" to r.id,"settlementUnitId" to r.unitId,"seq" to r.seq,"total" to r.total,"payerParticipantId" to r.payerId,"drinks" to r.drinks)
    private fun unitView(gid: Long,id: Long,pid: Long): Map<String,Any?> {
        val u=unit(gid,id,false);val viewed=db.rows("SELECT settlement_viewed_at FROM settlement_unit_members WHERE settlement_unit_id=:s AND participant_id=:p","s" to id,"p" to pid).singleOrNull()?.get("settlement_viewed_at")!=null
        return mapOf("id" to id,"hostParticipantId" to u.hostId,"status" to u.state,"inputRevision" to u.revision,"participantIds" to u.participantIds,"me" to mapOf("included" to (pid in u.participantIds),"settlementViewed" to viewed))
    }
    @Transactional(readOnly=true,isolation=Isolation.READ_COMMITTED)
    fun detail(gid: Long,uid: Long): Map<String,Any?> { person(gid,uid);return details(listOf(gid),uid).single() }
    @Transactional(readOnly=true,isolation=Isolation.READ_COMMITTED)
    fun list(uid: Long): List<Map<String,Any?>> { user(uid);val ids=db.rows("SELECT g.id FROM gatherings g JOIN participants p ON p.gathering_id=g.id WHERE p.user_id=:u AND p.status='ACTIVE' ORDER BY g.gathering_date DESC,g.id DESC","u" to uid).map{it.long("id")};return details(ids,uid) }
    private fun details(ids: List<Long>,uid: Long): List<Map<String,Any?>> {
        if(ids.isEmpty()) return emptyList()
        // 전체 목록도 테이블별 IN 배치 한 번씩. 상세 재귀 호출로 방마다 쿼리를 만들지 않는다.
        val g=db.rows("SELECT * FROM gatherings WHERE id IN (:ids)","ids" to ids).associateBy{it.long("id")}
        val people=db.rows("SELECT p.*,u.display_name,u.nickname,COALESCE(u.spoon_count,0) spoon_count,u.payout_encrypted FROM participants p LEFT JOIN users u ON u.id=p.user_id WHERE p.gathering_id IN (:ids) AND p.status='ACTIVE'","ids" to ids).groupBy{it.long("gathering_id")}
        val units=db.rows("SELECT * FROM settlement_units WHERE gathering_id IN (:ids) ORDER BY id","ids" to ids).groupBy{it.long("gathering_id")}
        val members=db.rows("SELECT * FROM settlement_unit_members WHERE gathering_id IN (:ids)","ids" to ids).groupBy{it.long("settlement_unit_id")}
        val rounds=db.rows("SELECT * FROM rounds WHERE gathering_id IN (:ids) ORDER BY seq","ids" to ids).groupBy{it.long("gathering_id")}
        val drinks=db.rows("SELECT d.* FROM drink_items d JOIN rounds r ON r.id=d.round_id WHERE r.gathering_id IN (:ids) ORDER BY d.id","ids" to ids).groupBy{it.long("round_id")}
        val responses=db.rows("SELECT a.*,r.gathering_id FROM round_responses a JOIN rounds r ON r.id=a.round_id WHERE r.gathering_id IN (:ids)","ids" to ids).groupBy{it.long("gathering_id")}
        val transfers=db.rows("SELECT t.*,s.gathering_id,s.settlement_unit_id FROM settlement_transfers t JOIN settlements s ON s.id=t.settlement_id WHERE s.gathering_id IN (:ids) ORDER BY t.id","ids" to ids).groupBy{it.long("gathering_id")}
        val basis=db.rows("SELECT b.* FROM settlement_transfer_items b JOIN settlement_transfers t ON t.id=b.transfer_id JOIN settlements s ON s.id=t.settlement_id WHERE s.gathering_id IN (:ids) ORDER BY b.round_id","ids" to ids).groupBy{it.long("transfer_id")}
        val timeline=db.rows("SELECT * FROM timeline_entries WHERE gathering_id IN (:ids) ORDER BY id","ids" to ids).groupBy{it.long("gathering_id")}
        return ids.map{gid-> val row=g.getValue(gid);val ps=people[gid].orEmpty();val me=ps.first{(it["user_id"] as? Number)?.toLong()==uid}.long("id")
            val ts=transfers[gid].orEmpty();val allowed=ts.filter{it.long("sender_participant_id")==me}.map{it.long("recipient_participant_id")}.toSet()+me
            mapOf("id" to gid,"title" to row["name"],"date" to (row["gathering_date"] as java.sql.Date).toLocalDate(),"createdByUserId" to row["host_user_id"],"shareToken" to row["share_token"],"status" to row["status"],"completedAt" to row.instant("completed_at"),"deleteScheduledAt" to row.instant("delete_scheduled_at"),
                "participants" to ps.map{p->mapOf("id" to p["id"],"userId" to p["user_id"],"displayName" to (p["display_name"] ?: p["name"]),"nickname" to p["nickname"],"spoonCount" to p["spoon_count"],"hasPayout" to (p["payout_encrypted"]!=null),"payout" to if(p.long("id") in allowed) payoutValue(p["payout_encrypted"]) else null)},
                "settlementUnits" to units[gid].orEmpty().map{u->val ms=members[u.long("id")].orEmpty();mapOf("id" to u["id"],"hostParticipantId" to u["host_participant_id"],"status" to u["status"],"inputRevision" to u["input_revision"],"completedAt" to u.instant("completed_at"),"participantIds" to ms.filter{it.text("status")=="ACTIVE"}.map{it.long("participant_id")},"me" to mapOf("included" to ms.any{it.long("participant_id")==me&&it.text("status")=="ACTIVE"},"settlementViewed" to ms.any{it.long("participant_id")==me&&it["settlement_viewed_at"]!=null}))},
                "rounds" to rounds[gid].orEmpty().map{r->mapOf("id" to r["id"],"settlementUnitId" to r["settlement_unit_id"],"seq" to r["seq"],"total" to r["total_amount"],"payerParticipantId" to r["payer_id"],"drinks" to drinks[r.long("id")].orEmpty().map{mapOf("name" to it["name"],"unitPrice" to it["unit_price"],"quantity" to it["bottle_count"])})},
                "responses" to responses[gid].orEmpty().map{mapOf("participantId" to it["participant_id"],"roundId" to it["round_id"],"type" to it["type"],"source" to it["source"])},
                "transfers" to ts.map{t->mapOf("id" to t["id"],"settlementUnitId" to t["settlement_unit_id"],"fromParticipantId" to t["sender_participant_id"],"toParticipantId" to t["recipient_participant_id"],"amount" to t["amount"],"status" to t["status"],"sentAt" to t.instant("sent_at"),"confirmedAt" to t.instant("confirmed_at"),"notReceivedAt" to t.instant("not_received_at"),"basis" to basis[t.long("id")].orEmpty().map{mapOf("roundId" to it["round_id"],"type" to it["type"],"amount" to it["amount"])})},
                "timeline" to timeline[gid].orEmpty().map{mapOf("id" to it["id"],"settlementUnitId" to it["settlement_unit_id"],"type" to it["type"],"authorParticipantId" to it["author_participant_id"],"body" to it["body"],"createdAt" to it.instant("created_at"))},"me" to mapOf("participantId" to me)) }
    }
    @Transactional(readOnly=true)
    fun meInfo(uid: Long): Map<String,Any?> {
        val row=user(uid)
        return mapOf("payout" to payoutValue(row["payout_encrypted"]),"spoonCount" to row.long("spoon_count"),
            "unreadNotificationCount" to db.rows("SELECT COUNT(*) n FROM notifications WHERE user_id=:u AND read_at IS NULL","u" to uid).single().long("n"))
    }

    fun deleteExpired(gid: Long, now: Instant): Boolean {
        val ids=db.rows("SELECT id FROM settlement_units WHERE gathering_id=:g ORDER BY id FOR UPDATE","g" to gid).map{it.long("id")}
        // 새 단위 생성과 동시에 삭제하면 재조회로 감지하고 이번 회차를 건너뛴다.
        val row=db.rows("SELECT * FROM gatherings WHERE id=:g FOR UPDATE","g" to gid).singleOrNull() ?: return false
        val fresh=db.rows("SELECT id,status FROM settlement_units WHERE gathering_id=:g ORDER BY id","g" to gid)
        val due=row.instant("delete_scheduled_at")
        if(fresh.map{it.long("id")}!=ids || !SettlementWorkflow.expired(fresh.map{it.text("status")},due,checkNotNull(row.instant("last_activity_at")),now)) return false
        db.update("UPDATE gatherings SET host_participant_id=NULL WHERE id=:g","g" to gid)
        for(table in listOf("timeline_entries","notifications","settlement_unit_requests","settlements"))
            db.update("DELETE FROM $table WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM notification_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(payload,'$.gatheringId'))=:g", "g" to gid.toString())
        db.update("DELETE d FROM drink_items d JOIN rounds r ON r.id=d.round_id WHERE r.gathering_id=:g","g" to gid)
        db.update("DELETE a FROM attendances a JOIN rounds r ON r.id=a.round_id WHERE r.gathering_id=:g","g" to gid)
        db.update("DELETE b FROM extra_item_bearers b JOIN extra_items e ON e.id=b.extra_item_id WHERE e.gathering_id=:g","g" to gid)
        db.update("DELETE FROM extra_items WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM rounds WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM settlement_unit_members WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM settlement_units WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM participants WHERE gathering_id=:g","g" to gid)
        db.update("DELETE FROM gatherings WHERE id=:g","g" to gid)
        return true
    }

    private fun payoutValue(value: Any?): Any? = (value as? ByteArray)?.let { mapper.readValue(cipher.decrypt(it),Map::class.java) }
    fun payout(uid: Long,request: PayoutRequest): Map<String,Any?> {
        user(uid)
        val value=SettlementWorkflow.payout(request.bank,request.accountNo,request.holder)
        val targets=db.rows("SELECT DISTINCT s.gathering_id,s.settlement_unit_id,p.user_id FROM settlement_transfers t JOIN settlements s ON s.id=t.settlement_id JOIN participants r ON r.id=t.recipient_participant_id JOIN participants p ON p.id=t.sender_participant_id WHERE r.user_id=:u AND t.status<>'CONFIRMED' AND p.user_id IS NOT NULL","u" to uid)
        targets.map{it.long("gathering_id")}.distinct().sorted().forEach{room(it,true)}
        val old=db.rows("SELECT payout_encrypted FROM users WHERE id=:u FOR UPDATE","u" to uid).singleOrNull()?.get("payout_encrypted")
        user(uid)
        db.update("UPDATE users SET payout_encrypted=:p WHERE id=:u","p" to cipher.encrypt(mapper.writeValueAsString(value)),"u" to uid)
        // 탈퇴와 같은 Gathering→users 순서로 FK 소식의 잠금 승격도 예방한다.
        if(old==null) targets.forEach{
            notify(it.long("gathering_id"),it.long("settlement_unit_id"),"PAYOUT_REGISTERED","계좌가 등록됐어요. 이제 보낼 수 있어요",listOf(it.long("user_id")),uid) }
        return mapOf("payout" to value)
    }
    fun message(gid: Long,uid: Long,request: MessageRequest): Map<String,Any?> {
        val p=person(gid,uid);val text=request.text.trim();if(text.isEmpty()||text.length>200) fail("MALFORMED_REQUEST",status=HttpStatus.BAD_REQUEST)
        room(gid,true);val now=Instant.now()
        val id=db.insert("INSERT INTO timeline_entries(gathering_id,type,author_participant_id,body,created_at) VALUES(:g,'MESSAGE',:p,:b,:n)","g" to gid,"p" to p.id,"b" to text,"n" to now);touch(gid)
        return mapOf("id" to id,"type" to "MESSAGE","authorParticipantId" to p.id,"body" to text,"createdAt" to now,"settlementUnitId" to null)
    }
    @Transactional(readOnly=true)
    fun notifications(uid: Long)=user(uid).let { db.rows("SELECT * FROM notifications WHERE user_id=:u ORDER BY id DESC LIMIT 50","u" to uid).map {
        mapOf("id" to it["id"],"type" to it["type"],"gatheringId" to it["gathering_id"],"settlementUnitId" to it["settlement_unit_id"],"title" to it["title"],"body" to it["body"],"createdAt" to it.instant("created_at"),"readAt" to it.instant("read_at"))
    } }
    fun readNotifications(uid: Long,id: Long?) {
        user(uid);db.update("UPDATE notifications SET read_at=COALESCE(read_at,:n) WHERE user_id=:u"+if(id==null) "" else " AND id=:id","n" to Instant.now(),"u" to uid,"id" to id)
    }
}
