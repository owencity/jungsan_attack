package app.jeongsan.server.gathering

import org.springframework.data.domain.Sort
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
 * ⚠ 인증(§2)도 아직 안 걸었다. 지금은 전체 모임을 반환한다.
 * 실제로 붙이기 전에 로그인한 사용자(`hostUserId`) 기준으로 필터링해야 한다.
 */
@RestController
@RequestMapping("/api/v1/gatherings")
class GatheringController(
    private val gatheringRepository: GatheringRepository,
) {
    @GetMapping
    fun list(): List<GatheringSummaryResponse> =
        gatheringRepository.findAll(Sort.by(Sort.Direction.DESC, "gatheringDate"))
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
