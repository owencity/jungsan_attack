package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/gatherings/{gid}/settlement-units")
class SettlementUnitController(private val service: GatheringService) {
    @PutMapping("/{id}/headcount")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun headcount(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long,
        @Valid @RequestBody request: HeadcountRequest) = service.headcount(gid,id,userId,request.headcount)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable gid: Long, @LoginUser userId: Long, @Valid @RequestBody request: UnitCreateRequest) = service.createUnit(gid,userId,request)

    @PutMapping("/{id}/participants/{pid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun include(@PathVariable gid: Long, @PathVariable id: Long, @PathVariable pid: Long, @LoginUser userId: Long) = service.member(gid,id,userId,pid,false)

    @DeleteMapping("/{id}/participants/{pid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable gid: Long, @PathVariable id: Long, @PathVariable pid: Long, @LoginUser userId: Long) = service.member(gid,id,userId,pid,true)

    @PostMapping("/{id}/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    fun round(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long, @Valid @RequestBody request: RoundRequest) = service.saveRound(gid,id,userId,null,request)

    @PutMapping("/{id}/rounds/{rid}")
    fun editRound(@PathVariable gid: Long, @PathVariable id: Long, @PathVariable rid: Long, @LoginUser userId: Long, @Valid @RequestBody request: RoundRequest) = service.saveRound(gid,id,userId,rid,request)

    @DeleteMapping("/{id}/rounds/{rid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRound(@PathVariable gid: Long, @PathVariable id: Long, @PathVariable rid: Long, @LoginUser userId: Long) = service.deleteRound(gid,id,userId,rid)

    @PutMapping("/{id}/responses/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun respond(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long, @Valid @RequestBody request: ResponseRequest) = service.respond(gid,id,userId,null,request)

    @PutMapping("/{id}/participants/{pid}/responses")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun proxy(@PathVariable gid: Long, @PathVariable id: Long, @PathVariable pid: Long, @LoginUser userId: Long, @Valid @RequestBody request: ResponseRequest) = service.respond(gid,id,userId,pid,request)

    @GetMapping("/{id}/settlement/preview")
    fun preview(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long) = service.preview(gid,id,userId)

    @PostMapping("/{id}/settlement")
    fun settle(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long, @Valid @RequestBody request: SettleRequest) = service.settle(gid,id,userId,request)

    @DeleteMapping("/{id}/settlement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reopen(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long) = service.reopen(gid,id,userId)

    @PostMapping("/{id}/settlement/viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun viewed(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long) = service.viewed(gid,id,userId)

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun complete(@PathVariable gid: Long, @PathVariable id: Long, @LoginUser userId: Long) = service.complete(gid,id,userId)

}
