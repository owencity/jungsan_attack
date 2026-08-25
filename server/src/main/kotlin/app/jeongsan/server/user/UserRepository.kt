package app.jeongsan.server.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    /** `uq_users_provider_id` 유니크 제약과 짝을 이루는 로그인 UPSERT 조회 키. */
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
}
