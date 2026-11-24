package app.jeongsan.server.receipt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** 수신 메타데이터뿐이며 사진을 재조회할 식별자를 제공하지 않는다. */
data class ReceiptReceptionResponse(val receivedBytes: Int, val contentType: String, val ocrProcessed: Boolean = false)

/** 후보의 null은 미인식이다. 병 수나 금액을 서버에서 보정하지 않는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptExtractionResponse(
    val schemaVersion: Int,
    val total: Long?,
    val drinks: List<ReceiptDrink>,
    val warnings: List<String>,
    val requiresConfirmation: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptDrink(
    val name: String,
    val category: String,
    val unit: String,
    val unitPrice: Long?,
    val quantity: Int?,
    val lineAmount: Long?,
    val servingType: String,
    val volumeMl: Int?,
    val bottleCount: Int?,
    val confidence: Double,
    val reviewReasons: List<String>,
)
