package app.jeongsan.server.user

import app.jeongsan.server.common.DisplayNameAlreadySetException
import app.jeongsan.server.common.MalformedRequestException
import app.jeongsan.server.common.UnauthenticatedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {
    @Transactional(readOnly = true)
    fun me(userId: Long): MeResponse = findUser(userId).toMeResponse()

    @Transactional
    fun registerDisplayName(userId: Long, request: DisplayNameRequest): MeResponse {
        val name = request.displayName.trim()
        // UTF-16 length는 이모지를 두 글자로 세므로 프론트와 같은 코드포인트 기준으로 검증한다.
        val count = name.codePointCount(0, name.length)
        if (count == 0) throw MalformedRequestException("이름을 넣어주세요")
        if (count == 1) throw MalformedRequestException("성까지 적어주세요")
        if (count > 10) throw MalformedRequestException("이름은 10자까지예요")

        userRepository.registerDisplayName(userId, name)
        val user = findUser(userId)
        if (user.displayName != name) {
            throw DisplayNameAlreadySetException()
        }
        return user.toMeResponse()
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { UnauthenticatedException() }
}
