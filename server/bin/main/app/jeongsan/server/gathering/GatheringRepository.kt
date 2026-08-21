package app.jeongsan.server.gathering

import org.springframework.data.jpa.repository.JpaRepository

interface GatheringRepository : JpaRepository<Gathering, Long>
