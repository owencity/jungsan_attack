package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import org.springframework.http.HttpStatus

object AccountPolicy {
    fun deletable(states: List<String>) {
        if(states.any { it=="OPEN" || it=="SETTLING" || it=="COLLECTING" || it=="CONFIRMED" })
            throw ApiException("ACTIVE_GATHERING_EXISTS",HttpStatus.CONFLICT,"진행 중인 정산이 끝난 뒤 탈퇴할 수 있어요")
        if(states.any { it!="COMPLETED" }) throw ApiException("ACCOUNT_STATE_CHANGED",HttpStatus.CONFLICT,"계정 상태를 확인한 뒤 다시 시도해 주세요.")
    }
    fun unchanged(before: List<Long>, after: List<Long>) {
        if(before!=after) throw ApiException("ACCOUNT_STATE_CHANGED",HttpStatus.CONFLICT,"계정 상태가 바뀌었어요. 다시 시도해 주세요.")
    }
}
