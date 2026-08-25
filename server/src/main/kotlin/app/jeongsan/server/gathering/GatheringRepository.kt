package app.jeongsan.server.gathering

import org.springframework.data.jpa.repository.JpaRepository

interface GatheringRepository : JpaRepository<Gathering, Long> {
    fun findByGroupIdOrderByGatheringDateDesc(groupId: Long): List<Gathering>

    /** 번개 모임에 술자리가 이미 있는지 — FLASH 는 하나만 갖는다(`API.md` §3-b.2). */
    fun countByGroupId(groupId: Long): Long
}
