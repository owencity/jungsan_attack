package app.jeongsan.server.receipt

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.HttpURLConnection

/** 키를 자동 출력하는 data class를 피한다. 설정은 기동 시 한 번만 검증한다. */
class ReceiptOcrSettings(val baseUrl: URI, val serviceKey: String) {
    companion object {
        fun read(environment: Environment): ReceiptOcrSettings? {
            val baseUrl = environment.getProperty("receipt.ocr.base-url")
            val serviceKey = environment.getProperty("receipt.ocr.service-key")
            if (baseUrl == null && serviceKey == null) return null
            require(baseUrl != null && serviceKey != null) { "영수증 연결 URL과 내부 키를 함께 설정해야 합니다." }
            val uri = try { URI(baseUrl) } catch (_: Exception) { throw IllegalArgumentException("영수증 연결 URL이 올바르지 않습니다.") }
            require(uri.scheme == "http" && uri.host in setOf("127.0.0.1", "localhost", "[::1]", "::1", "receipt-ocr") &&
                uri.port in 1..65535 && uri.rawPath in setOf("", "/") && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
                "영수증 연결은 포트가 명시된 내부 HTTP 주소만 허용합니다."
            }
            require(serviceKey.length >= 32 && serviceKey.all { it.code in 33..126 }) { "영수증 내부 키는 공백 없는 ASCII 32자 이상이어야 합니다." }
            return ReceiptOcrSettings(URI(baseUrl.removeSuffix("/")), serviceKey)
        }
    }
}

@Configuration
class ReceiptOcrConfiguration {
    @Bean
    fun receiptOcrClient(environment: Environment, mapper: ObjectMapper): ReceiptOcrClient {
        val settings = ReceiptOcrSettings.read(environment)
        val factory = object : SimpleClientHttpRequestFactory() {
            override fun prepareConnection(connection: HttpURLConnection, httpMethod: String) {
                super.prepareConnection(connection, httpMethod)
                // 내부 키가 리다이렉트 대상에 전달되지 않게 한다.
                connection.instanceFollowRedirects = false
            }
        }.apply { setConnectTimeout(2000); setReadTimeout(45000) }
        return HttpReceiptOcrClient(settings, RestClient.builder().requestFactory(factory).build(), mapper)
    }
}
