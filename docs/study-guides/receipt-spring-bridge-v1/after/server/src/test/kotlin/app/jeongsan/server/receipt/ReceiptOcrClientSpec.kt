package app.jeongsan.server.receipt

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.net.InetSocketAddress
import java.net.URI

class ReceiptOcrClientSpec : StringSpec({
    val mapper = jacksonMapperBuilder().build()
    val fixture = javaClass.getResourceAsStream("/receipt/response-v2.json")!!.readBytes().toString(Charsets.UTF_8)
    val key = "ci-only-receipt-service-key-32-characters"
    val settings = ReceiptOcrSettings(URI("http://127.0.0.1:18082"), key)
    val reader = ReceiptResponseReader(mapper)

    "추가 원문 필드는 버리고 미인식과 0을 구분한다" {
        val raw = fixture.replace("\"total\": 50000", "\"total\": 0, \"rawText\": \"카드 원문\"")
            .replace("\"bottleCount\": null", "\"bottleCount\": 0, \"cardNumber\": \"출력하면 안 됨\"")
        val result = reader.read(raw.toByteArray())
        result.total shouldBe 0L; result.drinks.single().bottleCount shouldBe 0; result.drinks.single().volumeMl shouldBe null
        mapper.readTree(mapper.writeValueAsBytes(result)).has("rawText") shouldBe false
        mapper.readTree(mapper.writeValueAsBytes(result))["drinks"][0].has("cardNumber") shouldBe false
    }
    val invalidResponses = listOf(
        "깨진 JSON" to "{", "루트 배열" to "[]", "후행 JSON" to "$fixture {}",
        "거짓 확인 플래그" to fixture.replace("\"requiresConfirmation\": true", "\"requiresConfirmation\": false"),
        "스키마 오버플로" to fixture.replace("\"schemaVersion\": 2", "\"schemaVersion\": 18446744073709551618"),
        "문자 금액" to fixture.replace("\"total\": 50000", "\"total\": \"50000\""),
        "실수 금액" to fixture.replace("\"total\": 50000", "\"total\": 50000.1"),
        "음수 금액" to fixture.replace("\"total\": 50000", "\"total\": -1"),
        "큰 금액" to fixture.replace("\"total\": 50000", "\"total\": 1000000001"),
        "병 수 추측" to fixture.replace("\"bottleCount\": null", "\"bottleCount\": 1.2"),
        "과도한 수량" to fixture.replace("\"quantity\": 3", "\"quantity\": 1001"),
        "빠진 후보" to fixture.replace("\"volumeMl\": null,", ""),
        "신뢰도 초과" to fixture.replace("\"confidence\": 0.98", "\"confidence\": 1.01"),
        "임의 코드 원문" to fixture.replace("ITEMS_NEED_REVIEW", "영수증 원문"),
        "새 단위 값" to fixture.replace("\"unit\": \"UNKNOWN\"", "\"unit\": \"TOTAL\""))
    invalidResponses.forEach { (label, raw) ->
        "$label 응답은 공개 후보로 변환하지 않는다" {
            shouldThrow<ReceiptException> { reader.read(raw.toByteArray()) }.code shouldBe "OCR_BAD_RESPONSE"
        }
    }
    "HTTP 어댑터는 내부 고정 경로와 키와 원본 바이트를 한번 보낸다" {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val bytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
        server.expect(requestTo("http://127.0.0.1:18082/api/v1/receipts/extract"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("X-Receipt-Service-Key", key))
            .andExpect(content().contentType("image/jpeg")).andExpect(content().bytes(bytes))
            .andRespond(withSuccess(fixture, MediaType.APPLICATION_JSON))
        val result = HttpReceiptOcrClient(settings, builder.build(), mapper).extract(bytes, "image/jpeg")
        result.requiresConfirmation shouldBe true; result.drinks.single().bottleCount shouldBe null
        server.verify()
    }
    val errors = listOf(Triple(503, "OCR_BUSY", "OCR_BUSY"), Triple(503, "OCR_UNAVAILABLE", "OCR_UNAVAILABLE"),
        Triple(401, "SERVICE_AUTH_REQUIRED", "OCR_UNAVAILABLE"), Triple(500, "secret-provider-error", "OCR_BAD_RESPONSE"),
        Triple(400, "IMAGE_INVALID", "IMAGE_INVALID"), Triple(400, "IMAGE_DIMENSIONS_TOO_LARGE", "IMAGE_DIMENSIONS_TOO_LARGE"),
        Triple(400, "IMAGE_FORMAT_UNSUPPORTED", "IMAGE_FORMAT_UNSUPPORTED"), Triple(413, "IMAGE_TOO_LARGE", "IMAGE_TOO_LARGE"))
    errors.forEach { (status, internalCode, expected) ->
        "내부 $status $internalCode 오류는 허용 코드로만 노출한다" {
            val builder = RestClient.builder()
            val server = MockRestServiceServer.bindTo(builder).build()
            server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                .body("""{"detail":{"code":"$internalCode","message":"private-value"}}"""))
            val failure = shouldThrow<ReceiptException> { HttpReceiptOcrClient(settings, builder.build(), mapper).extract(byteArrayOf(1), "image/png") }
            failure.code shouldBe expected; failure.message.contains("private-value") shouldBe false
            if (expected == "OCR_BUSY") failure.retryAfter shouldBe 2L
            server.verify()
        }
    }
    "응답 길이가 한도를 넘으면 502다" {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(anything()).andRespond(withSuccess(" ".repeat(256 * 1024 + 1), MediaType.APPLICATION_JSON))
        shouldThrow<ReceiptException> { HttpReceiptOcrClient(settings, builder.build(), mapper).extract(byteArrayOf(1), "image/png") }.code shouldBe "OCR_BAD_RESPONSE"
        server.verify()
    }
    "실제 소켓 읽기 시간 초과는 504로 닫힌다" {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/v1/receipts/extract") { exchange ->
            exchange.requestBody.readAllBytes()
            Thread.sleep(250)
            exchange.close()
        }
        server.start()
        try {
            val factory = SimpleClientHttpRequestFactory().apply { setConnectTimeout(500); setReadTimeout(30) }
            val configured = ReceiptOcrSettings(URI("http://127.0.0.1:${server.address.port}"), key)
            shouldThrow<ReceiptException> { HttpReceiptOcrClient(configured, RestClient.builder().requestFactory(factory).build(), mapper)
                .extract(byteArrayOf(1), "image/png") }.code shouldBe "OCR_TIMEOUT"
        } finally { server.stop(0) }
    }
    "실제 리다이렉트는 내부 키를 다른 경로로 전송하지 않는다" {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var redirectedCalls = 0
        server.createContext("/api/v1/receipts/extract") { exchange ->
            exchange.requestBody.readAllBytes()
            exchange.responseHeaders.add("Location", "http://127.0.0.1:${server.address.port}/redirect-target")
            exchange.sendResponseHeaders(307, -1)
            exchange.close()
        }
        server.createContext("/redirect-target") { exchange -> redirectedCalls++; exchange.sendResponseHeaders(200, -1); exchange.close() }
        server.start()
        try {
            val environment = MockEnvironment().withProperty("receipt.ocr.base-url", "http://127.0.0.1:${server.address.port}")
                .withProperty("receipt.ocr.service-key", key)
            val client = ReceiptOcrConfiguration().receiptOcrClient(environment, mapper)
            shouldThrow<ReceiptException> { client.extract(byteArrayOf(1), "image/png") }.code shouldBe "OCR_BAD_RESPONSE"
            redirectedCalls shouldBe 0
        } finally { server.stop(0) }
    }
    "실제 연결 거부는 내부 주소를 노출하지 않고 503이다" {
        val socket = java.net.ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))
        val port = socket.localPort
        socket.close()
        val factory = SimpleClientHttpRequestFactory().apply { setConnectTimeout(500); setReadTimeout(500) }
        val client = HttpReceiptOcrClient(ReceiptOcrSettings(URI("http://127.0.0.1:$port"), key), RestClient.builder().requestFactory(factory).build(), mapper)
        val failure = shouldThrow<ReceiptException> { client.extract(byteArrayOf(1), "image/png") }
        failure.code shouldBe "OCR_UNAVAILABLE"
        failure.message.contains("127.0.0.1") shouldBe false
    }
    "미설정과 부분 설정과 외부 주소와 잘못된 키를 구분한다" {
        ReceiptOcrSettings.read(MockEnvironment()) shouldBe null
        val invalidSettings = listOf("https://127.0.0.1:8091", "http://external.example:8091", "http://localhost", "http://localhost:0",
            "http://localhost:8091/path", "http://name@localhost:8091", "http://localhost:8091?secret=x", "http://localhost:8091#x")
        invalidSettings.forEach { url ->
            shouldThrow<IllegalArgumentException> { ReceiptOcrSettings.read(MockEnvironment().withProperty("receipt.ocr.base-url", url).withProperty("receipt.ocr.service-key", key)) }
        }
        shouldThrow<IllegalArgumentException> { ReceiptOcrSettings.read(MockEnvironment().withProperty("receipt.ocr.base-url", "http://localhost:8091")) }
        shouldThrow<IllegalArgumentException> { ReceiptOcrSettings.read(MockEnvironment().withProperty("receipt.ocr.base-url", "http://localhost:8091").withProperty("receipt.ocr.service-key", "short")) }
        ReceiptOcrSettings.read(MockEnvironment().withProperty("receipt.ocr.base-url", "http://receipt-ocr:8091/").withProperty("receipt.ocr.service-key", key))!!.baseUrl.toString() shouldBe "http://receipt-ocr:8091"
    }
})
