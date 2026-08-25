package app.jeongsan.server.group

import app.jeongsan.server.common.MalformedRequestException
import app.jeongsan.server.common.NotFoundException
import app.jeongsan.server.common.ShareToken
import app.jeongsan.server.gathering.Gathering
import app.jeongsan.server.gathering.GatheringRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 모임 유스케이스.
 *
 * 계산은 하지 않는다 — 정산 계산은 `core` 모듈이 유일한 주체다(`ADR-005`).
 * 여기는 읽고·조립하고·저장하는 껍데기다.
 */
@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val gatheringRepository: GatheringRepository,
) {

    @Transactional(readOnly = true)
    fun listMyGroups(userId: Long): List<GroupSummaryResponse> =
        groupRepository.findMyGroups(userId).map { group ->
            GroupSummaryResponse(
                id = group.id,
                name = group.name,
                groupType = group.groupType,
                memberCount = groupMemberRepository.countByIdGroupId(group.id),
                gatheringCount = gatheringRepository.countByGroupId(group.id),
            )
        }

    /**
     * 모임 생성.
     *
     * 두 가지를 **같은 트랜잭션에서** 한다.
     *   ① 생성자를 OWNER 로 등록 — 주인 없는 모임을 만들지 않는다(`API.md` §3-b.2)
     *   ② FLASH 면 술자리를 1개 함께 만든다 — 번개는 "오늘 한 번" 쓰는 것이라
     *      모임 만들고 술자리를 또 만들라고 하면 번거롭다
     *
     * 하나라도 실패하면 전부 되돌아간다. 모임만 있고 OWNER 가 없는 상태가 생기면
     * 그 모임은 아무도 관리할 수 없게 된다.
     */
    @Transactional
    fun create(userId: Long, request: CreateGroupRequest): CreateGroupResponse {
        val isFlash = request.groupType == GroupType.FLASH

        // 번개는 술자리를 같이 만들므로 그에 필요한 값이 반드시 있어야 한다.
        if (isFlash) {
            if (request.gatheringDate == null) {
                throw MalformedRequestException("번개 모임은 날짜가 필요합니다.")
            }
            if (request.expectedCount == null) {
                throw MalformedRequestException("번개 모임은 예상 인원이 필요합니다.")
            }
        }

        val group = groupRepository.save(
            Group(
                name = request.name,
                groupType = request.groupType,
                shareToken = ShareToken.generate(),
                createdByUserId = userId,
                createdAt = Instant.now(),
            ),
        )

        groupMemberRepository.save(
            GroupMember(
                id = GroupMemberId(groupId = group.id, userId = userId),
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )

        val gatheringId = if (isFlash) {
            gatheringRepository.save(
                Gathering(
                    groupId = group.id,
                    name = request.name,
                    hostUserId = userId,
                    gatheringDate = request.gatheringDate!!,
                    shareToken = ShareToken.generate(),
                    expectedCount = request.expectedCount!!,
                    createdAt = Instant.now(),
                ),
            ).id
        } else {
            null
        }

        return CreateGroupResponse(
            id = group.id,
            name = group.name,
            groupType = group.groupType,
            shareToken = group.shareToken!!,
            role = GroupRole.OWNER,
            gatheringId = gatheringId,
        )
    }

    /**
     * 모임 상세.
     *
     * **멤버가 아니면 404 다.** 403 을 주면 "그 모임은 존재한다"를 알려주는 셈이다
     * (`API.md` §1.3).
     */
    @Transactional(readOnly = true)
    fun detail(userId: Long, groupId: Long): GroupDetailResponse {
        if (!groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
            throw NotFoundException("모임을 찾을 수 없습니다.")
        }
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("모임을 찾을 수 없습니다.") }

        return GroupDetailResponse(
            id = group.id,
            name = group.name,
            groupType = group.groupType,
            shareToken = group.shareToken,
            members = groupMemberRepository.findByIdGroupId(groupId).map {
                GroupMemberResponse(userId = it.id.userId, role = it.role)
            },
            gatherings = gatheringRepository.findByGroupIdOrderByGatheringDateDesc(groupId).map {
                GroupGatheringResponse(id = it.id, name = it.name, date = it.gatheringDate, status = it.status)
            },
        )
    }
}
