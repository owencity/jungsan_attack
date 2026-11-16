package app.jeongsan.server.quicksettlement

import app.jeongsan.server.common.ActiveUserAccess
import org.springframework.stereotype.Service

@Service
class QuickSettlementService(private val users: ActiveUserAccess) {
    fun calculate(userId: Long, request: QuickSettlementRequest): QuickSettlementResponse {
        users.requireUser(userId)
        return QuickSettlementCalculator.calculate(request.total, request.headcount)
    }
}
