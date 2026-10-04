package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * ADR-013 의 REST API 서비스 — 뼈대 검증용 첫 조각.
 *
 * `API.md` §3.1 이 정한 계약(`GatheringSummary[]`, `date DESC`)의 최소 부분집합만
 * 구현했다. `participantCount`·`respondedCount`·`paidCount`·`payableCount` 는
 * `Participant` 조립이 필요해 이번 뼈대에 없다 — 다음 작업이다.
 *
 * v3 참여자 목록 구현 전에도 타인의 데이터를 공개하지 않도록 총무 본인의 행만 반환한다.
 */
@RestController
@RequestMapping("/api/v1/gatherings")
class GatheringController(
    private val gatheringRepository: GatheringRepository,
) {
    @GetMapping
    fun list(@LoginUser userId: Long): List<GatheringSummaryResponse> =
        gatheringRepository.findByHostUserIdOrderByGatheringDateDesc(userId)
            .map { it.toSummary() }
}

data class GatheringSummaryResponse(
    val id: Long,
    val name: String,
    val date: LocalDate,
    val status: GatheringStatus,
)

private fun Gathering.toSummary() = GatheringSummaryResponse(
    id = id,
    name = name,
    date = gatheringDate,
    status = status,
)
