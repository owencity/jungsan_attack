package app.jeongsan.server.group

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupRepository : JpaRepository<Group, Long> {

    /**
     * 내가 속한 모임 목록. 소프트 삭제된 것은 뺀다.
     *
     * 목록에서만 사라질 뿐 `findById` 로는 여전히 읽힌다 — 지난 정산 기록을
     * 결제 내역에서 볼 수 있어야 하기 때문이다(`009-group-type-and-lifecycle.yaml`).
     */
    @Query(
        """
        SELECT g FROM Group g
        WHERE g.deletedAt IS NULL
          AND g.id IN (SELECT m.id.groupId FROM GroupMember m WHERE m.id.userId = :userId)
        ORDER BY g.createdAt DESC
        """,
    )
    fun findMyGroups(@Param("userId") userId: Long): List<Group>

    fun findByShareToken(shareToken: String): Group?
}

interface GroupMemberRepository : JpaRepository<GroupMember, GroupMemberId> {
    fun findByIdGroupId(groupId: Long): List<GroupMember>
    fun countByIdGroupId(groupId: Long): Long
    fun existsByIdGroupIdAndIdUserId(groupId: Long, userId: Long): Boolean
}
