package app.jeongsan.server.review

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 심사자가 공유받을 수 있는 고정 진입 안내다. 인증 정보를 만들거나 사용자 입력을 반영하지 않는다. */
@RestController
class ReviewDemoController {
    @GetMapping("/api/v1/review/demo", produces = [MediaType.TEXT_HTML_VALUE])
    fun demo(): ResponseEntity<String> = ResponseEntity.ok()
        .contentType(MediaType("text", "html", Charsets.UTF_8))
        .header("Cache-Control", "no-store")
        .header("Referrer-Policy", "no-referrer")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; style-src 'unsafe-inline'; script-src 'none'")
        .body("""<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>정산어택 데모 · Jeongsan Demo</title>
<style>body{font-family:system-ui,sans-serif;max-width:40rem;margin:3rem auto;padding:1.5rem;line-height:1.7;color:#171717;background:#fff}a{display:inline-block;background:#4f46e5;color:#fff;padding:.8rem 1.2rem;border-radius:.5rem}h1{font-size:1.6rem}</style></head>
<body><main><h1>정산어택 데모 체험</h1>
<p>새 정산어택 앱을 설치한 뒤 아래 버튼을 눌러주세요. 로그인 없이 가상 데이터로 체험할 수 있어요. 실제 송금은 발생하지 않아요.</p>
<p><a href="jeongsan://demo">앱에서 데모 열기 · Open demo in app</a></p>
<p>열리지 않으면 앱 로그인 화면에서 ‘로그인 없이 데모 체험’을 눌러주세요. 총무·참여자A·참여자B를 전환해 응답, 정산, 입금 확인, 메시지 신고·차단을 확인할 수 있어요. ‘처음부터’로 초기화하고 ‘체험 종료’로 나갈 수 있어요.</p>
<h2>App Review</h2><p>Install the updated app first. No credentials are required for the demo. You can also tap ‘로그인 없이 데모 체험’ on the login screen. Switch between the organizer and two participants using the top bar. All demo data is fictional; no real bank transfer is made. Use ‘처음부터’ to reset or ‘체험 종료’ to exit.</p>
</main></body></html>""")
}
