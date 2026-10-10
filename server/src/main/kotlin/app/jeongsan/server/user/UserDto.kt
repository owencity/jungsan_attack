package app.jeongsan.server.user

data class DisplayNameRequest(val displayName: String)

data class MeResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val displayName: String?,
    val needsName: Boolean,
    val payout: Any? = null,
    val spoonCount: Long = 0,
    val unreadNotificationCount: Long = 0,
)

fun User.toMeResponse() = MeResponse(id, nickname, profileImageUrl, displayName, displayName == null)
