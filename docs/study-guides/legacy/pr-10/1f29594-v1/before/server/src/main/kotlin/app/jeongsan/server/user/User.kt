package app.jeongsan.server.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * `users` 테이블 매핑. 컬럼 목록은 `server/src/main/resources/db/changelog/001-users.yaml`
 * 이 유일한 진실이다 — 이 엔티티는 그것을 따라간다, 거꾸로가 아니다(`ddl-auto: none`).
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var provider: String = "",

    @Column(name = "provider_id")
    var providerId: String = "",

    var nickname: String = "",

    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,

    var tier: String = "FREE",

    @Column(name = "tier_expires_at")
    var tierExpiresAt: Instant? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),
)
