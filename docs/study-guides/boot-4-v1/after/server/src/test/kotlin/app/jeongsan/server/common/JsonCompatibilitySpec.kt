package app.jeongsan.server.common

import app.jeongsan.server.gathering.GatheringEditRequest
import app.jeongsan.server.gathering.PayoutCipher
import app.jeongsan.server.user.KakaoTokenResponse
import app.jeongsan.server.user.KakaoUserInfo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

/** 수동 생성한 테스트 매퍼만 통과하고 운영 자동 설정은 달라지는 회귀를 막는다. */
class JsonCompatibilitySpec : StringSpec({
    val context = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java))

    "Boot 매퍼는 기존 날짜 문자열과 null 오류 필드를 유지한다" {
        context.run { application ->
            val mapper = application.getBean(JsonMapper::class.java)
            val response = mapper.readTree(mapper.writeValueAsString(mapOf(
                "date" to LocalDate.parse("2026-10-17"),
                "expiresAt" to Instant.parse("2026-10-17T00:00:00Z"),
                "errors" to null,
                "amount" to 1_000_000_000_000L,
            )))
            response["date"].asString() shouldBe "2026-10-17"
            response["expiresAt"].asString() shouldBe "2026-10-17T00:00:00Z"
            response.has("errors") shouldBe true
            response["errors"].isNull shouldBe true
            response["amount"].longValue() shouldBe 1_000_000_000_000L
            mapper.readValue("""{"date":"2026-10-17"}""", GatheringEditRequest::class.java)
                .date shouldBe LocalDate.parse("2026-10-17")
        }
    }

    "이전 버전의 저장 계좌 JSON은 선행 0과 한글 예금주를 보존한다" {
        // Jackson 2가 저장하던 문자열을 고정한다. 새 매퍼로 만든 값을 기대값으로 쓰지 않는다.
        val previous = """{"bank":"국민","accountNo":"001234567890","holder":"김하나"}"""
        val cipher = PayoutCipher(Base64.getEncoder().encodeToString(ByteArray(32) { 7 }))
        val stored = cipher.encrypt(previous)
        context.run { application ->
            val mapper = application.getBean(JsonMapper::class.java)
            val account = mapper.readValue(cipher.decrypt(stored), Map::class.java)
            account shouldBe mapOf("bank" to "국민", "accountNo" to "001234567890", "holder" to "김하나")
            mapper.readTree(mapper.writeValueAsString(account)) shouldBe mapper.readTree(previous)
        }
    }

    "RestClient 기본 JSON 변환기는 카카오 응답의 별칭·큰 ID·추가 필드를 읽는다" {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()
        server.expect(requestTo("https://provider.test/token")).andRespond(withSuccess(
            """{"access_token":"dummy","expires_in":3600}""", MediaType.APPLICATION_JSON))
        server.expect(requestTo("https://provider.test/me")).andRespond(withSuccess(
            """{"id":9007199254740993,"kakao_account":{"profile":{"nickname":"김하나","profile_image_url":null},"extra":true},"extra":true}""",
            MediaType.APPLICATION_JSON))
        client.get().uri("https://provider.test/token").retrieve().body(KakaoTokenResponse::class.java)
            ?.accessToken shouldBe "dummy"
        val user = client.get().uri("https://provider.test/me").retrieve().body(KakaoUserInfo::class.java)!!
        user.id shouldBe 9007199254740993L
        user.kakaoAccount.profile.nickname shouldBe "김하나"
        user.kakaoAccount.profile.profileImageUrl shouldBe null
        server.verify()
    }
})
