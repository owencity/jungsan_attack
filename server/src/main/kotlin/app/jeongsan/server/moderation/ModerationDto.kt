package app.jeongsan.server.moderation

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.time.Instant

data class MessageReportRequest(
    @field:Pattern(regexp = "ABUSE|SEXUAL|SPAM|OTHER") val reason: String,
)

data class UserBlockRequest(
    @field:Positive val gatheringId: Long,
    @field:Positive val participantId: Long,
)

data class UserBlockResponse(val userId: Long, val displayName: String, val blockedAt: Instant)
