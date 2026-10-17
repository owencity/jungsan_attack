package app.jeongsan.server.gathering

import app.jeongsan.server.gathering.javaimpl.SettlementWorkflow as JWorkflow
import app.jeongsan.server.gathering.javaimpl.PayoutCipher as JCipher
import app.jeongsan.server.common.ApiException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactly
import java.util.Base64

class WorkflowParitySpec : StringSpec({
    val u=UnitInput(10,1,"OPEN",7,listOf(1,2,3))
    val people=listOf(PersonInput(1,11,"김하나"),PersonInput(2,12,"김둘"),PersonInput(3,13,"김셋"),PersonInput(4,14,"김넷"))
    val rounds=listOf(RoundInput(101,10,1,10000,1,emptyList()),RoundInput(102,10,2,10000,1,emptyList()),
        RoundInput(103,10,3,10000,1,emptyList()),RoundInput(201,20,4,-1,4,emptyList()))
    fun UnitInput.j()=JWorkflow.UnitInput(id,hostId,state,revision,participantIds)
    fun List<PersonInput>.jp()=map{JWorkflow.PersonInput(it.id,it.userId,it.name)}
    fun List<RoundInput>.jr()=map{JWorkflow.RoundInput(it.id,it.unitId,it.seq,it.total,it.payerId,it.drinks.map{d->JWorkflow.DrinkInput(d.name,d.unitPrice,d.quantity)})}
    fun List<ResponseInput>.ja()=map{JWorkflow.ResponseInput(it.participantId,it.roundId,it.type,it.source)}
    val mapper=jacksonObjectMapper()
    fun parity(unit: UnitInput=u,rs: List<RoundInput> = rounds,answers: List<ResponseInput> = emptyList()): Preview {
        val k=SettlementWorkflow.preview(unit,people,rs,answers)
        val j=JWorkflow.preview(unit.j(),people.jp(),rs.jr(),answers.ja())
        mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(k) shouldBe mapper.valueToTree(j)
        k.transfers.forEach{it.basis.sumOf{b->b.amount} shouldBe it.amount}
        return k
    }
    fun errors(k: ()->Unit,j: ()->Unit): String {
        val a=shouldThrow<ApiException>(k);val b=shouldThrow<ApiException>(j)
        a.code shouldBe b.code;a.status shouldBe b.status;return a.code
    }
    "같은 결제자 3차를 합친 뒤 올림하며 B의 잘못된 입력은 A를 막지 않는다" {
        val result=parity()
        result.grandTotal shouldBe 30000
        result.lines.map{it.total}.shouldContainExactly(10000,10000,10000)
        result.transfers.map{it.amount}.shouldContainExactly(10000,10000)
        result.transfers.first().basis.map{it.amount}.shouldContainExactly(3333,3333,3334)
    }
    "미응답만 DRANK로 투영하고 본인 불참과 논알코올 응답은 보존한다" {
        val rs=listOf(RoundInput(101,10,1,10000,1,listOf(DrinkInput("소주",1000,2))))
        val result=parity(rs=rs,answers=listOf(ResponseInput(2,101,"ABSENT","SELF"),ResponseInput(3,101,"SOBER","HOST")))
        result.lines.first{it.participantId==2L}.total shouldBe 0
        result.lines.first{it.participantId==3L}.total shouldBe 4000
        result.lines.first{it.participantId==1L}.auto shouldBe true
        result.lines.first{it.participantId==3L}.auto shouldBe false
    }
    "이름·목록 순서·B 입력은 hash를 바꾸지 않고 A의 금액은 바꾼다" {
        val input=SettlementWorkflow.input(u,people,rounds,emptyList())
        val hash=SettlementWorkflow.hash(u,input)
        SettlementWorkflow.hash(u,SettlementWorkflow.input(u,people.reversed().map{it.copy(name="새 이름")},rounds.reversed(),emptyList())) shouldBe hash
        (SettlementWorkflow.hash(u,SettlementWorkflow.input(u,people,rounds.map{if(it.id==101L) it.copy(total=10001) else it},emptyList()))==hash) shouldBe false
        parity(rs=rounds.reversed())
    }
    "총무와 결제자는 자신의 단위에서 제외할 수 없고 다른 차수 응답도 거절한다" {
        errors({SettlementWorkflow.host(u,people[1])},{JWorkflow.host(u.j(),people.jp()[1])}) shouldBe "NOT_SETTLEMENT_UNIT_HOST"
        errors({SettlementWorkflow.remove(u,1,rounds)},{JWorkflow.remove(u.j(),1,rounds.jr())}) shouldBe "REMOVE_HOST"
        val rs=listOf(RoundInput(101,10,1,100,2,emptyList()))
        errors({SettlementWorkflow.remove(u,2,rs)},{JWorkflow.remove(u.j(),2,rs.jr())}) shouldBe "REMOVE_PAYER"
        errors({SettlementWorkflow.response(u,2,201,"DRANK",rounds)},{JWorkflow.response(u.j(),2,201,"DRANK",rounds.jr())}) shouldBe "ROUND_NOT_FOUND"
        errors({SettlementWorkflow.response(u,2,101,"EXEMPT",rounds)},{JWorkflow.response(u.j(),2,101,"EXEMPT",rounds.jr())}) shouldBe "MALFORMED_REQUEST"
        errors({SettlementWorkflow.member(u,4)},{JWorkflow.member(u.j(),4)}) shouldBe "NOT_SETTLEMENT_UNIT_MEMBER"
    }
    "A가 정산된 상태여도 B의 OPEN 작업은 별도 단위 상태로 허용한다" {
        errors({SettlementWorkflow.open(u.copy(state="SETTLING"))},{JWorkflow.open(u.copy(state="SETTLING").j())}) shouldBe "SETTLEMENT_UNIT_NOT_OPEN"
        SettlementWorkflow.open(u.copy(id=20,hostId=4));JWorkflow.open(u.copy(id=20,hostId=4).j())
        listOf(listOf("COMPLETED","OPEN"),listOf("COMPLETED","SETTLING"),listOf("COMPLETED","COMPLETED")).forEach {
            SettlementWorkflow.summary(it) shouldBe JWorkflow.summary(it)
        }
    }
    "송금은 역할·계좌·단위 상태를 검사하고 반복 확인은 멱등이다" {
        for(state in listOf("WAITING","SENT","CONFIRMED")) for(action in listOf("sent","confirm","not-received"))
            for(unit in listOf("OPEN","SETTLING","COMPLETED")) for(account in listOf(true,false)) for(actor in listOf(1L,2L,3L)) {
                fun attempt(block: ()->String): String=try{block()}catch(e: ApiException){e.code+":"+e.status.value()}
                attempt{SettlementWorkflow.transfer(state,action,actor,2,1,unit,account)} shouldBe
                    attempt{JWorkflow.transfer(state,action,actor,2,1,unit,account)}
            }
        SettlementWorkflow.transfer("SENT","confirm",1,2,1,"SETTLING",false) shouldBe "CONFIRMED"
        SettlementWorkflow.transfer("CONFIRMED","confirm",1,2,1,"COMPLETED",false) shouldBe "CONFIRMED"
        SettlementWorkflow.transfer("CONFIRMED","sent",2,2,1,"COMPLETED",false) shouldBe "CONFIRMED"
    }
    "너무 작은 금액은 두 엔진 모두 음수 조정 오류를 반환한다" {
        val rs=listOf(RoundInput(101,10,1,1,1,emptyList()))
        val k=shouldThrow<SettlementValidationException>{SettlementWorkflow.preview(u,people,rs,emptyList())}
        val j=shouldThrow<SettlementValidationException>{JWorkflow.preview(u.j(),people.jp(),rs.jr(),emptyList())}
        k.details shouldBe j.details;k.details.first().code.name shouldBe "NEGATIVE_ADJUSTED_AMOUNT"
    }
    "AES-GCM 양방향 호환·매번 새로운 nonce·변조 검출과 키 길이를 확인한다" {
        val key=Base64.getEncoder().encodeToString(ByteArray(32){it.toByte()})
        val k=PayoutCipher(key);val j=JCipher(key);val text="계좌 3333123456789"
        k.decrypt(j.encrypt(text)) shouldBe text;j.decrypt(k.encrypt(text)) shouldBe text
        k.encrypt(text).contentEquals(k.encrypt(text)) shouldBe false
        val changed=k.encrypt(text);changed[changed.lastIndex]=(changed.last().toInt() xor 1).toByte()
        shouldThrow<javax.crypto.AEADBadTagException>{k.decrypt(changed)}
        shouldThrow<javax.crypto.AEADBadTagException>{j.decrypt(changed)}
        shouldThrow<IllegalArgumentException>{PayoutCipher(Base64.getEncoder().encodeToString(ByteArray(16)))}
    }
    "명단·계좌 검증과 7일 완료 및 30일 미활동 삭제의 경계가 같다" {
        SettlementWorkflow.creationRoster(listOf(3,1,2),1) shouldBe JWorkflow.creationRoster(listOf(3L,1L,2L),1)
        errors({SettlementWorkflow.creationRoster(listOf(1,1),1)},{JWorkflow.creationRoster(listOf(1L,1L),1)}) shouldBe "MALFORMED_REQUEST"
        SettlementWorkflow.payout("국민","1234-567890"," 김하나 ") shouldBe JWorkflow.payout("국민","1234-567890"," 김하나 ")
        errors({SettlementWorkflow.payout("국민","1234","김하나")},{JWorkflow.payout("국민","1234","김하나")}) shouldBe "MALFORMED_REQUEST"
        val now=java.time.Instant.parse("2026-10-07T00:00:00Z")
        for(states in listOf(listOf("OPEN"),listOf("COMPLETED"))) for(due in listOf(null,now,now.plusSeconds(1)))
            for(last in listOf(now,now.minusSeconds(30*86400L),now.minusSeconds(30*86400L-1))) {
                SettlementWorkflow.expired(states,due,last,now) shouldBe JWorkflow.expired(states,due,last,now)
            }
        SettlementWorkflow.expired(listOf("OPEN"),null,now.minusSeconds(30*86400L),now) shouldBe true
        SettlementWorkflow.expired(listOf("COMPLETED"),now.plusSeconds(1),now,now) shouldBe false
    }
})
