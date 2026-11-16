package app.jeongsan.server.receipt

import app.jeongsan.server.common.ActiveUserAccess

import app.jeongsan.server.gathering.GatheringStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.mockito.Answers
import org.mockito.Mockito
import org.springframework.aop.framework.ProxyFactory
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.io.ByteArrayInputStream
import java.sql.Connection
import javax.sql.DataSource

class ReceiptTransactionSpec : StringSpec({
    "권한 조회 트랜잭션은 사진과 모델 처리 전에 끝나고 상위 트랜잭션 호출은 거절한다" {
        var commits = 0
        val source = Mockito.mock(DataSource::class.java) { invocation ->
            if (invocation.method.name == "getConnection") Mockito.mock(Connection::class.java) { call ->
                when (call.method.name) {
                    "getAutoCommit" -> true
                    "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED
                    "commit" -> { commits++; null }
                    else -> Answers.RETURNS_DEFAULTS.answer(call)
                }
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val manager = DataSourceTransactionManager(source)
        val attributes = AnnotationTransactionAttributeSource()
        fun proxied(target: Any): Any = ProxyFactory(target).apply {
            isProxyTargetClass = true
            addAdvice(TransactionInterceptor().apply { transactionManager = manager; transactionAttributeSource = attributes })
        }.proxy
        var reads = 0
        val store = Mockito.mock(GatheringStore::class.java) { invocation ->
            if (invocation.method.name == "rows") {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe true
                TransactionSynchronizationManager.isCurrentTransactionReadOnly() shouldBe true
                reads++
                if ((invocation.arguments[0] as String).contains("FROM users")) listOf(mapOf("id" to 12L))
                else listOf(mapOf("unit_status" to "OPEN", "participant_status" to "ACTIVE", "member_status" to "ACTIVE",
                    "participant_id" to 12L, "host_participant_id" to 12L))
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val access = proxied(DatabaseReceiptUnitAccess(store)) as ReceiptUnitAccess
        var modelCalls = 0
        val candidate = ReceiptExtractionResponse(2, null, emptyList(), listOf("TOTAL_NOT_FOUND"), true)
        val client = object : ReceiptOcrClient {
            override fun available() = true
            override fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
                TransactionSynchronizationManager.hasResource(source) shouldBe false
                commits shouldBe 1
                modelCalls++
                return candidate
            }
        }
        val service = proxied(ReceiptService(access, client, ReceiptAdmission(), ReceiptMetrics(SimpleMeterRegistry()), ActiveUserAccess { })) as ReceiptService
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
        service.extract(1, 10, 12, "image/jpeg", jpeg.size.toLong()) {
            TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
            ByteArrayInputStream(jpeg)
        } shouldBe candidate
        reads shouldBe 4
        commits shouldBe 2
        modelCalls shouldBe 1
        TransactionTemplate(manager).executeWithoutResult {
            shouldThrow<IllegalTransactionStateException> {
                service.extract(1, 10, 12, "image/jpeg", jpeg.size.toLong()) { error("열면 안 된다") }
            }
        }
        modelCalls shouldBe 1
        reads shouldBe 4
    }
})
