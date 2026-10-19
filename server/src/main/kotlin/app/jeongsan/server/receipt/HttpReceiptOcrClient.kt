package app.jeongsan.server.receipt

import org.springframework.http.MediaType
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.SocketTimeoutException

class HttpReceiptOcrClient(private val settings: ReceiptOcrSettings?, private val http: RestClient, private val mapper: ObjectMapper) : ReceiptOcrClient {
    private val reader = ReceiptResponseReader(mapper)
    override fun available() = settings != null

    override fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse {
        val configured = settings ?: throw ReceiptException("OCR_UNAVAILABLE", 503)
        try {
            return http.post().uri(configured.baseUrl.resolve("/api/v1/receipts/extract"))
                .header("X-Receipt-Service-Key", configured.serviceKey).contentType(MediaType.parseMediaType(mediaType)).body(bytes)
                .exchange { _, response ->
                    if (response.statusCode.value() != 200) {
                        val errorBytes = response.body.readNBytes(4097)
                        val code = if (errorBytes.size <= 4096) try { mapper.readTree(errorBytes)["detail"]?.get("code")?.stringValue() } catch (_: RuntimeException) { null } else null
                        val mapped = when {
                            response.statusCode.value() == 503 && code == "OCR_BUSY" -> ReceiptException("OCR_BUSY", 503, 2)
                            response.statusCode.value() == 503 && code == "OCR_UNAVAILABLE" -> ReceiptException("OCR_UNAVAILABLE", 503)
                            response.statusCode.value() == 401 -> ReceiptException("OCR_UNAVAILABLE", 503)
                            response.statusCode.value() == 413 && code == "IMAGE_TOO_LARGE" -> ReceiptException(code, 413)
                            response.statusCode.value() in setOf(400, 415) && code == "IMAGE_FORMAT_UNSUPPORTED" -> ReceiptException(code, 415)
                            response.statusCode.value() == 400 && code in setOf("IMAGE_INVALID", "IMAGE_SIZE_INVALID", "IMAGE_DIMENSIONS_TOO_LARGE") -> ReceiptException(code!!, 400)
                            else -> ReceiptException("OCR_BAD_RESPONSE", 502)
                        }
                        throw mapped
                    }
                    val content = response.body.readNBytes(256 * 1024 + 1)
                    if (content.size > 256 * 1024) throw ReceiptException("OCR_BAD_RESPONSE", 502)
                    reader.read(content)
                }
        } catch (failure: ResourceAccessException) {
            throw transportError(failure)
        } catch (failure: IOException) {
            throw transportError(failure)
        }
    }

    private fun transportError(failure: Throwable): ReceiptException =
        if (generateSequence(failure) { it.cause }.any { it is SocketTimeoutException }) ReceiptException("OCR_TIMEOUT", 504)
        else ReceiptException("OCR_UNAVAILABLE", 503)
}
