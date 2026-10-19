package app.jeongsan.server.receipt

import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

/** 모델 호출 중에는 모니터를 잡지 않는다. 단일 프로세스 한도이며 재시작하면 횟수가 초기화된다. */
@Component
class ReceiptAdmission {
    private data class Window(val startedAt: Long, var usedCount: Int)
    private val windows = LinkedHashMap<Long, Window>()
    private val permit = Semaphore(1)

    @Synchronized
    fun enter(userId: Long, observedNanos: Long? = null): AutoCloseable {
        val nowNanos = observedNanos ?: System.nanoTime()
        val iterator = windows.entries.iterator()
        while (iterator.hasNext()) {
            if (nowNanos - iterator.next().value.startedAt >= ReceiptPolicy.WINDOW_NANOS) iterator.remove() else break
        }
        val window = windows[userId]
        val retryAfter = window?.let { ReceiptPolicy.retryAfter(it.usedCount, nowNanos - it.startedAt) } ?: 0
        if (retryAfter > 0) throw ReceiptException("OCR_REQUEST_LIMIT", 429, retryAfter)
        if (window == null && windows.size >= 10_000) throw ReceiptException("OCR_BUSY", 503, 2)
        if (!permit.tryAcquire()) throw ReceiptException("OCR_BUSY", 503, 2)
        if (window == null) windows[userId] = Window(nowNanos, 1) else window.usedCount++
        val released = AtomicBoolean()
        return AutoCloseable { if (released.compareAndSet(false, true)) permit.release() }
    }
}
