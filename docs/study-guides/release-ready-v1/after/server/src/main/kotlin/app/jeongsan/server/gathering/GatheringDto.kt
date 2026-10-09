package app.jeongsan.server.gathering

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

data class GatheringCreateRequest(@field:Min(2) @field:Max(50) val headcount: Int? = null)
data class HeadcountRequest(@field:Min(2) @field:Max(50) val headcount: Int)
data class UnitCreateRequest(val requestId: java.util.UUID, @field:Size(min=1,max=1000) val participantIds: List<Long>,
    @field:Min(2) @field:Max(50) val headcount: Int? = null)
data class GatheringEditRequest(@field:Size(min=1, max=20) val title: String? = null, val date: LocalDate? = null)
data class DrinkRequest(@field:Size(min=1,max=20) val name: String, @field:Min(1) val unitPrice: Long, @field:Min(1) val quantity: Int)
data class RoundRequest(@field:Min(1) @field:Max(1_000_000_000_000) val total: Long, val payerParticipantId: Long,
    @field:Valid @field:Size(max=100) val drinks: List<DrinkRequest> = emptyList())
data class AnswerRequest(val roundId: Long, val type: String)
data class ResponseRequest(@field:Size(max=1000) val answers: List<AnswerRequest>)
data class JoinRequest(val settlementUnitId: Long, @field:Size(max=1000) val responses: List<AnswerRequest> = emptyList())
data class SettleRequest(val inputRevision: Long, @field:Pattern(regexp="[a-f0-9]{64}") val inputHash: String)
data class MessageRequest(@field:Size(min=1,max=200) val text: String)
data class PayoutRequest(val bank: String, val accountNo: String, val holder: String)
