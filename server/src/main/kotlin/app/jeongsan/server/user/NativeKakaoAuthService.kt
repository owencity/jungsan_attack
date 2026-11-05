package app.jeongsan.server.user

import org.springframework.stereotype.Service

/** 네트워크를 기다리는 동안 DB 트랜잭션/연결을 잡지 않는다. */
@Service
class NativeKakaoAuthService(private val gateway: NativeKakaoGateway, private val flow: AuthFlowService) {
    fun login(request: NativeKakaoLoginRequest): AppExchangeResponse =
        flow.completeNativeKakao(gateway.identity(request.accessToken))
}
