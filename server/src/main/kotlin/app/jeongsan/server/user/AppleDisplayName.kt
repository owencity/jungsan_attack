package app.jeongsan.server.user

import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.text.Normalizer

/** Apple의 user 폼은 표시용 이름만 제공한다. 계정 식별에는 검증된 id_token의 subject만 쓴다. */
object AppleDisplayName {
    private val mapper = jacksonMapperBuilder().build()
    private val spaces = Regex("[\\p{Z}\\s]+")
    private val invisible = Regex("[\\p{C}<>]")

    fun fromCallback(user: String?, userId: Long): String {
        require(userId > 0)
        val fallback = "총무" + userId.toString().takeLast(8)
        // 이름을 다시 받기 위해 인증을 실패시키지 않는다. 이메일·기타 필드는 읽거나 보관하지 않는다.
        if (user == null || user.toByteArray(Charsets.UTF_8).size > 8192) return fallback
        return try {
            val name = mapper.readTree(user)?.get("name") ?: return fallback
            fun part(field: String): String = name.get(field)?.takeIf { it.isString }?.asString()?.let {
                invisible.replace(spaces.replace(Normalizer.normalize(it, Normalizer.Form.NFC), " "), "").trim()
            }.orEmpty()
            val first = part("firstName")
            val last = part("lastName")
            // 한국어 이름은 성을 앞에 붙이고, 다른 이름은 제공된 이름·성 사이에 공백을 둔다.
            val parts = listOf(first, last).filter { it.isNotEmpty() }
            val full = if (parts.all { Regex("[\\p{IsHangul}]+").matches(it) }) last + first else parts.joinToString(" ")
            if (full.isEmpty()) fallback
            else full.substring(0, full.offsetByCodePoints(0, minOf(10, full.codePointCount(0, full.length)))).trimEnd()
        } catch (_: Exception) {
            // user는 최초 동의에만 올 수 있는 선택 필드다. 원문·이름을 로그에 남기지 않는다.
            fallback
        }
    }
}
