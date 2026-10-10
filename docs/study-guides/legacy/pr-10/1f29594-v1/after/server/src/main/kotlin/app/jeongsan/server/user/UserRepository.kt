package app.jeongsan.server.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    /** `uq_users_provider_id` 유니크 제약과 짝을 이루는 로그인 UPSERT 조회 키. */
    fun findByProviderAndProviderId(provider: String, providerId: String): User?

    /** 동시 최초 등록도 조건부 UPDATE로 하나만 성공시키고, 다시 읽어 저장된 값을 비교한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.displayName = :name WHERE u.id = :id AND u.displayName IS NULL")
    fun registerDisplayName(@Param("id") userId: Long, @Param("name") displayName: String): Int
}
