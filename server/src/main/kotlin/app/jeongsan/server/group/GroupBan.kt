package app.jeongsan.server.group

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/** 관리자가 명시적으로 해제하기 전까지 모임 재가입을 막는 차단 기록. */
@Entity
@Table(name = "group_bans")
class GroupBan(
    @EmbeddedId
    var id: GroupBanId = GroupBanId(),

    @Column(name = "banned_by_user_id")
    var bannedByUserId: Long = 0,

    @Column(name = "banned_at")
    var bannedAt: Instant = Instant.now(),

    @Column(name = "released_by_user_id")
    var releasedByUserId: Long? = null,

    @Column(name = "released_at")
    var releasedAt: Instant? = null,
) {
    val isActive: Boolean
        get() = releasedAt == null
}

@Embeddable
data class GroupBanId(
    @Column(name = "group_id")
    var groupId: Long = 0,

    @Column(name = "user_id")
    var userId: Long = 0,
) : Serializable
