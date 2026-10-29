package app.jeongsan.server.review

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ReviewDemoSpec : StringSpec({
    val mvc = MockMvcBuilders.standaloneSetup(ReviewDemoController()).build()

    "로그인 없이 고정 데모 링크와 설치·가상 데이터 안내를 반환한다" {
        val response = mvc.perform(get("/api/v1/review/demo"))
            .andExpect(status().isOk).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().doesNotExist("Set-Cookie")).andReturn().response
        response.characterEncoding shouldBe "UTF-8"
        response.contentAsString.contains("href=\"jeongsan://demo\"") shouldBe true
        response.contentAsString.contains("가상 데이터") shouldBe true
        response.contentAsString.contains("Install the updated app first") shouldBe true
        response.getHeader("Content-Security-Policy") shouldBe
            "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; style-src 'unsafe-inline'; script-src 'none'"
    }

    "공격 query·헤더는 본문·목적지에 반영되지 않고 인증 값도 발급하지 않는다" {
        val clean = mvc.perform(get("/api/v1/review/demo")).andReturn().response.contentAsString
        val response = mvc.perform(get("/api/v1/review/demo").param("returnTo", "https://evil.test/<script>")
            .param("ticket", "private-ticket").header("User-Agent", "<img src=https://evil.test>"))
            .andExpect(status().isOk).andExpect(header().doesNotExist("Location"))
            .andExpect(header().doesNotExist("Set-Cookie")).andReturn().response
        response.contentAsString shouldBe clean
        response.contentAsString.contains("<script") shouldBe false
        response.contentAsString.contains("private-ticket") shouldBe false
    }
})
