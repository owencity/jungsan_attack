package app.jeongsan.server.common

import app.jeongsan.server.gathering.GatheringStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

fun interface ActiveUserAccess {
    fun requireUser(userId: Long)
}

/** 외부 호출 뒤에도 계정 삭제가 반영되도록 JWT와 별개로 현재 계정 존재를 확인한다. */
@Component
class DatabaseActiveUserAccess(private val store: GatheringStore) : ActiveUserAccess {
    @Transactional(readOnly = true)
    override fun requireUser(userId: Long) {
        if (store.rows("SELECT id FROM users WHERE id=:userId", "userId" to userId).isEmpty()) throw UnauthenticatedException()
    }
}
