package app.jeongsan.server.user

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/** 브라우저가 자동 앱 전환을 차단해도 직접 누를 수 있는 링크를 남긴다. 외부 자원은 불러오지 않는다. */
internal object AppReturnPage {
    // 티켓은 검증된 링크 속성에만 둔다. 실행할 코드는 고정해 CSP 해시로 제한한다.
    const val SCRIPT = """try { history.replaceState(null, '', location.pathname); } catch (_) {}
try { location.replace(document.getElementById('return-to-app').href); } catch (_) {}"""

    val contentSecurityPolicy: String = "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; " +
        "style-src 'unsafe-inline'; script-src 'sha256-" +
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(SCRIPT.toByteArray(StandardCharsets.UTF_8))) + "'"

    fun html(ticket: String, userAgent: String?): String {
        val link = AuthPolicy.appReturnLink(ticket, userAgent)
        return """<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>정산어택 로그인 완료</title>
<style>body{margin:0;background:#f6f4ee;color:#20251f;font-family:system-ui,sans-serif;line-height:1.6}main{max-width:28rem;margin:12vh auto;padding:1.5rem}h1{font-size:1.75rem}a{display:block;margin:2rem 0;padding:1rem;background:#244d38;color:white;text-align:center;border-radius:.75rem;text-decoration:none;font-weight:700}p{font-size:1rem}</style></head>
<body><main><h1>로그인이 완료됐어요</h1><p>앱이 열리지 않으면 아래 버튼을 눌러주세요.</p>
<a id="return-to-app" href="$link">정산어택 앱으로 돌아가기</a>
<p>5분 안에 눌러주세요. 시간이 지났다면 앱에서 로그인을 다시 시작해주세요.</p></main>
<script>$SCRIPT</script></body></html>"""
    }
}
