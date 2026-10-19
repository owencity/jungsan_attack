package app.jeongsan.server.receipt

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/** 외부 JSON의 숫자 자동 변환과 원문 필드 유입을 막는 HTTP 경계다. */
class ReceiptResponseReader(private val mapper: ObjectMapper) {
    private val categories = setOf("SOJU", "BEER", "MAKGEOLLI", "CHEONGJU_YAKJU", "WINE", "WHISKY_SPIRIT", "COCKTAIL_MIXED", "OTHER_ALCOHOL", "UNKNOWN")
    private val units = setOf("BOTTLE", "CAN", "GLASS", "PITCHER", "PACK", "UNKNOWN")
    private val codePattern = Regex("[A-Z][A-Z0-9_]{0,63}")

    fun read(bytes: ByteArray): ReceiptExtractionResponse = try {
        val root: JsonNode = mapper.readerFor(JsonNode::class.java).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(bytes)
        require(root.isObject && root["schemaVersion"]?.isIntegralNumber == true && root["schemaVersion"].canConvertToInt() && root["schemaVersion"].intValue() == 2)
        require(root["requiresConfirmation"]?.isBoolean == true && root["requiresConfirmation"].booleanValue())
        val drinks = root["drinks"]
        require(drinks != null && drinks.isArray && drinks.size() <= 1000)
        ReceiptExtractionResponse(2, number(root, "total", 0, 1_000_000_000), (0 until drinks.size()).map { index ->
            val drink = drinks[index]
            require(drink.isObject)
            val confidence = drink["confidence"]
            require(confidence != null && confidence.isNumber && confidence.doubleValue().isFinite() && confidence.doubleValue() in 0.0..1.0)
            ReceiptDrink(text(drink, "name").also { require(it.length in 1..120 && it.none { character -> character.isISOControl() }) },
                choice(drink, "category", categories), choice(drink, "unit", units),
                number(drink, "unitPrice", 1, 1_000_000_000), number(drink, "quantity", 1, 1000)?.toInt(),
                number(drink, "lineAmount", 1, 1_000_000_000), choice(drink, "servingType", setOf("DRAFT", "UNKNOWN")),
                number(drink, "volumeMl", 1, 100000)?.toInt(), number(drink, "bottleCount", 0, 1000)?.toInt(),
                confidence.doubleValue(), codes(drink, "reviewReasons"))
        }, codes(root, "warnings"), true)
    } catch (_: RuntimeException) { throw ReceiptException("OCR_BAD_RESPONSE", 502) }

    private fun text(node: JsonNode, field: String): String {
        val value = node[field]
        require(value != null && value.isString)
        return value.stringValue()
    }

    private fun choice(node: JsonNode, field: String, allowed: Set<String>): String = text(node, field).also { require(it in allowed) }

    private fun number(node: JsonNode, field: String, minimum: Long, maximum: Long): Long? {
        val value = node[field]
        require(value != null)
        if (value.isNull) return null
        require(value.isIntegralNumber && value.canConvertToLong())
        return value.longValue().also { require(it in minimum..maximum) }
    }

    private fun codes(node: JsonNode, field: String): List<String> {
        val values = node[field]
        require(values != null && values.isArray && values.size() <= 32)
        return (0 until values.size()).map { index ->
            val value = values[index]
            require(value.isString && codePattern.matches(value.stringValue()))
            value.stringValue()
        }
    }
}
