package app.jeongsan.server.user

import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import java.time.Instant

class AppleDisplayNameSpec : StringSpec({
    "한국어 성·이름을 합치고 이메일과 폼 ID는 이름이나 계정 식별에 쓰지 않는다" {
        AppleDisplayName.fromCallback("""{"name":{"firstName":"동규","lastName":"김"},"id":"999","email":"unused@example.test"}""", 12) shouldBe "김동규"
        AppleDisplayName.fromCallback("""{"id":"999","email":"unused@example.test"}""", 12) shouldBe "총무12"
    }
    "Apple에서 받은 한 글자 이름도 재입력 없이 사용한다" {
        AppleDisplayName.fromCallback("""{"name":{"firstName":"김"}}""", 12) shouldBe "김"
    }
    "이름은 NFC로 정규화하고 제어·방향 문자와 꺾쇠를 제거하며 공백을 정리한다" {
        AppleDisplayName.fromCallback("""{"name":{"firstName":"  Jose\u0301 \t ","lastName":"\u202e <Kim>\u0000"}}""", 12) shouldBe "José Kim"
    }
    "10 code point 제한은 이모지의 UTF16 쌍을 자르지 않는다" {
        val actual = AppleDisplayName.fromCallback("""{"name":{"firstName":"${"😀".repeat(11)}"}}""", 12)
        actual shouldBe "😀".repeat(10)
        actual.codePointCount(0,actual.length) shouldBe 10
    }
    "이름 누락·깨진 JSON·잘못된 타입·빈 이름은 안정적인 기본 표시명을 사용한다" {
        listOf(null, "", "{", "null", "[]", "{}", """{"name":[]} """,
            """{"name":{"firstName":123,"lastName":true}}""", """{"name":{"firstName":"\u202e\u0000"}}""")
            .forEach { AppleDisplayName.fromCallback(it,12) shouldBe "총무12" }
        AppleDisplayName.fromCallback(null,Long.MAX_VALUE) shouldBe "총무54775807"
    }
    "8KiB를 넘는 user 폼은 이름만 무시하며 별도 필드를 보관하지 않는다" {
        AppleDisplayName.fromCallback("""{"name":{"firstName":"${"가".repeat(2800)}"}}""",12) shouldBe "총무12"
    }
    "Apple 신원 검증 실패에서는 선택 이름 폼이 있어도 계정 저장을 하지 않는다" {
        val db = Mockito.mock(GatheringStore::class.java)
        val gateway = Mockito.mock(ProviderGateway::class.java)
        val state = "s".repeat(43)
        Mockito.`when`(db.rows("SELECT * FROM auth_challenges WHERE state_hash=:s FOR UPDATE", "s" to AuthPolicy.hash(state)))
            .thenReturn(listOf(mapOf("expires_at" to java.sql.Timestamp.from(Instant.now().plusSeconds(300)),
                "provider" to "APPLE", "browser_hash" to AuthPolicy.hash("browser"), "nonce" to "nonce")))
        Mockito.`when`(gateway.identity("APPLE","invalid","nonce")).thenThrow(UnauthenticatedException())
        val flow = AuthFlowService(db,gateway,JwtService("01234567890123456789012345678901",30),"AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=")
        shouldThrow<UnauthenticatedException> {
            flow.finish("APPLE",state,"browser","invalid","""{"name":{"firstName":"다른사람"}}""")
        }
        Mockito.verify(db).rows("SELECT * FROM auth_challenges WHERE state_hash=:s FOR UPDATE", "s" to AuthPolicy.hash(state))
        Mockito.verifyNoMoreInteractions(db)
    }
})
