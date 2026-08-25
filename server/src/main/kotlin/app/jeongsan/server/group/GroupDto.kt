package app.jeongsan.server.group

import app.jeongsan.server.gathering.GatheringStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * `POST /groups` 요청 — `API.md` §3-b.2.
 *
 * `gatheringDate`·`expectedCount` 는 **FLASH 일 때만 필수**다. 애너테이션만으로는
 * "다른 필드 값에 따라 필수"를 표현할 수 없어서 `GroupService` 가 검사한다.
 */
data class CreateGroupRequest(
    @field:NotBlank(message = "모임 이름을 입력해주세요.")
    @field:Size(max = 50, message = "모임 이름은 50자를 넘을 수 없습니다.")
    val name: String,

    val groupType: GroupType,

    /** FLASH 필수 · RECURRING 무시. */
    val gatheringDate: LocalDate? = null,

    /** FLASH 필수 · RECURRING 무시. 본인 포함 예상 인원 — 정원 초과 승인 게이트의 기준. */
    @field:Min(value = 1, message = "예상 인원은 1명 이상이어야 합니다.")
    val expectedCount: Int? = null,
)

data class CreateGroupResponse(
    val id: Long,
    val name: String,
    val groupType: GroupType,
    val shareToken: String,
    val role: GroupRole,
    /** FLASH 면 함께 만들어진 술자리 id, RECURRING 이면 null. */
    val gatheringId: Long?,
)

data class GroupSummaryResponse(
    val id: Long,
    val name: String,
    val groupType: GroupType,
    val memberCount: Long,
    val gatheringCount: Long,
)

data class GroupDetailResponse(
    val id: Long,
    val name: String,
    val groupType: GroupType,
    val shareToken: String?,
    val members: List<GroupMemberResponse>,
    val gatherings: List<GroupGatheringResponse>,
)

data class GroupMemberResponse(
    val userId: Long,
    val role: GroupRole,
)

data class GroupGatheringResponse(
    val id: Long,
    val name: String,
    val date: LocalDate,
    val status: GatheringStatus,
)
