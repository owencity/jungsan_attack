package app.jeongsan.server.quicksettlement

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = QuickSettlementRequestDeserializer::class)
data class QuickSettlementRequest(
    @field:Min(1) @field:Max(10_000_000) val total: Long,
    @field:Min(2) @field:Max(50) val headcount: Int,
)

/** 소수 금액이나 소수 인원이 자동 절단되어 다른 계산으로 처리되지 않게 한다. */
class QuickSettlementRequestDeserializer : ValueDeserializer<QuickSettlementRequest>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): QuickSettlementRequest {
        val root = context.readTree(parser)
        val total = root["total"]
        val headcount = root["headcount"]
        if (!root.isObject || total?.isIntegralNumber != true || !total.canConvertToLong() ||
            headcount?.isIntegralNumber != true || !headcount.canConvertToInt()) {
            return context.reportInputMismatch(QuickSettlementRequest::class.java, "총액과 인원은 정수 숫자로 입력해주세요.")
        }
        return QuickSettlementRequest(total.longValue(), headcount.intValue())
    }
}

data class QuickSettlementResponse(
    val total: Long,
    val headcount: Int,
    val participantAmount: Long,
    val payerAmount: Long,
    val incomingTotal: Long,
)
