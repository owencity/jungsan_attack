package app.jeongsan.server.quicksettlement

import app.jeongsan.core.ValidationError
import app.jeongsan.server.common.ApiException
import org.springframework.http.HttpStatus

/** 저장된 방의 상태 충돌과 구분한다. 간단 정산의 계산 불가능한 입력은 422다. */
class QuickSettlementValidationException(val details: List<ValidationError>) :
    ApiException("VALIDATION_FAILED", HttpStatus.valueOf(422), "금액과 인원을 확인해주세요.")
