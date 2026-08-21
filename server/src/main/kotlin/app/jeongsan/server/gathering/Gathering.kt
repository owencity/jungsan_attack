package app.jeongsan.server.gathering

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * `gatherings` 테이블 매핑. 컬럼 목록은 `server/src/main/resources/db/changelog/003-gatherings.yaml`
 * 이 유일한 진실이다 — 이 엔티티는 그것을 따라간다, 거꾸로가 아니다(`ddl-auto: none`).
 *
 * 계산 로직은 여기 없다. `core` 모듈이 유일한 계산 주체다(ADR-005) — 이 엔티티는
 * `Attendance` 등을 모아 계산을 요청하는 조립 지점일 뿐이다.
 */
@Entity
@Table(name = "gatherings")
class Gathering(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "group_id")
    var groupId: Long? = null,

    var name: String = "",

    @Column(name = "host_user_id")
    var hostUserId: Long = 0,

    @Column(name = "host_participant_id")
    var hostParticipantId: Long? = null,

    @Column(name = "gathering_date")
    var gatheringDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    var status: GatheringStatus = GatheringStatus.COLLECTING,

    @Column(name = "share_token")
    var shareToken: String = "",

    @Column(name = "expected_count")
    var expectedCount: Int = 0,

    @Column(name = "rounding_unit")
    var roundingUnit: Int = 10,

    var revision: Int = 0,

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null,

    @Column(name = "delete_scheduled_at")
    var deleteScheduledAt: Instant? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),
)

enum class GatheringStatus { COLLECTING, CONFIRMED }
