package app.jeongsan.server.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * `users` 테이블 매핑. 기본 컬럼은 `001-users.yaml`, 실명은 `015-user-display-name.yaml`
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

    // 일반 save(카카오 로그인 포함)로 실명을 덮어쓰지 않고 최초 등록 조건부 UPDATE만 허용한다.
    @Column(name = "display_name", updatable = false)
    val displayName: String? = null,

    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,

    var tier: String = "FREE",

    @Column(name = "tier_expires_at")
    var tierExpiresAt: Instant? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),
)
