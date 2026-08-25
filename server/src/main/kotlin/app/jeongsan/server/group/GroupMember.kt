package app.jeongsan.server.group

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/**
 * `group_members` 테이블 매핑 — 한글명 **모임 멤버**.
 *
 * PK 가 `(group_id, user_id)` 복합키다 — 그 자체가 "같은 사람이 같은 모임에 두 번
 * 못 들어온다"는 제약이다(`002-groups.yaml`). 그래서 별도 유니크 제약이 없다.
 *
 * `ADR-009` — **소속만 기록한다.** 다음 술자리 참여를 자동으로 만들지 않는다.
 */
@Entity
@Table(name = "group_members")
class GroupMember(
    @EmbeddedId
    var id: GroupMemberId = GroupMemberId(),

    @Enumerated(EnumType.STRING)
    var role: GroupRole = GroupRole.MEMBER,

    @Column(name = "joined_at")
    var joinedAt: Instant = Instant.now(),
)

@Embeddable
data class GroupMemberId(
    @Column(name = "group_id")
    var groupId: Long = 0,

    @Column(name = "user_id")
    var userId: Long = 0,
) : Serializable

enum class GroupRole {
    /** 모임을 만든 사람. 멤버 제거 등 관리 기능을 쓸 수 있고, 자기 자신은 못 뺀다. */
    OWNER,
    MEMBER,
}
