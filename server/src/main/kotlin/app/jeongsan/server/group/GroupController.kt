package app.jeongsan.server.group

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 모임 API — `API.md` §3-b.
 *
 * 로그인이 필수다. `@LoginUser` 가 쿠키에서 userId 를 꺼내고, 없거나 무효하면
 * 401 을 던진다 — 컨트롤러가 인증을 신경 쓰지 않는다.
 *
 * **클라이언트가 "나는 N번 유저"라고 보내지 않는다.** 서버가 쿠키에서 직접 꺼내므로
 * 남의 계정으로 모임을 만들 수 없다.
 */
@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
) {
    @GetMapping
    fun list(@LoginUser userId: Long): List<GroupSummaryResponse> =
        groupService.listMyGroups(userId)

    @PostMapping
    fun create(
        @LoginUser userId: Long,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ResponseEntity<CreateGroupResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(userId, request))

    @GetMapping("/{groupId}")
    fun detail(
        @LoginUser userId: Long,
        @PathVariable groupId: Long,
    ): GroupDetailResponse = groupService.detail(userId, groupId)
}
