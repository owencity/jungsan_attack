package app.jeongsan.server.gathering

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GatheringRepository : JpaRepository<Gathering, Long> {
    fun findByGroupIdOrderByGatheringDateDesc(groupId: Long): List<Gathering>

    /** 번개 모임에 술자리가 이미 있는지 — FLASH 는 하나만 갖는다(`API.md` §3-b.2). */
    fun countByGroupId(groupId: Long): Long

    /** 모임별 술자리 수를 한 번에 센다 — 모임마다 count 를 날리면 N+1 이 된다. */
    @Query(
        """
        SELECT g.groupId, COUNT(g)
        FROM Gathering g
        WHERE g.groupId IN :groupIds
        GROUP BY g.groupId
        """,
    )
    fun countByGroupIds(@Param("groupIds") groupIds: Collection<Long>): List<Array<Any>>
}
