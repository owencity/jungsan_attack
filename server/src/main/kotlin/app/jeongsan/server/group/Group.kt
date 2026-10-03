package app.jeongsan.server.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * `user_groups` 테이블의 진실은 changelog `002`, `009`, `011`~`014`이다.
 *
 * 모임 안에 술자리(`Gathering`)가 들어간다. 번개(FLASH)는 술자리를 **하나만** 갖는다.
 */
/*
 * 테이블명이 `user_groups` 인 이유 — **GROUPS 가 MySQL 예약어다**(011 changelog 참조).
 * 클래스명은 `Group` 그대로 둔다. 자바 예약어가 아니고, 도메인 용어가 "모임"이라
 * 코드에서는 이 이름이 맞다.
 */
@Entity
@Table(name = "user_groups")
class Group(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    var groupType: GroupType = GroupType.RECURRING,

    @Column(name = "share_token")
    var shareToken: String? = null,

    @Column(name = "created_by_user_id")
    var createdByUserId: Long = 0,

    /** 현재 유일한 관리자. 생성 이력인 createdByUserId와 달리 관리자 양도 때 바뀐다. */
    @Column(name = "admin_user_id")
    var adminUserId: Long = 0,

    /** 가입 비밀번호의 단방향 해시. 기존 모임은 API 전환 전까지 null일 수 있다. */
    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    /** 삭제 예정 시각. FLASH 의 정산이 확정될 때 확정시각 + 14일로 채운다. RECURRING 은 항상 null. */
    @Column(name = "delete_scheduled_at")
    var deleteScheduledAt: Instant? = null,

    /** 소프트 삭제. 목록에서만 숨기고 결제 내역에서는 그대로 보인다. */
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
)

enum class GroupType {
    /** 번개 — 1회성. 술자리 1개만 갖고, 정산 확정 +14일 뒤 목록에서 사라진다. */
    FLASH,

    /** 주기 — 계속 만나는 고정 멤버. 술자리를 여러 개 갖고 만료가 없다. */
    RECURRING,
}
