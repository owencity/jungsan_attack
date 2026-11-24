package app.jeongsan.server.receipt

import org.springframework.stereotype.Service
import java.io.InputStream

/** 현재 단계는 사진 수신 확인뿐이다. 모델 클라이언트·저장소·후속 작업에 의존하지 않는다. */
@Service
class ReceiptReceptionService(private val access: ReceiptUnitAccess, private val photos: ReceiptPhotoReader,
                              private val admission: ReceiptAdmission) {
    fun receive(gatheringId: Long, settlementUnitId: Long, userId: Long, contentType: String?, contentLength: Long,
                source: () -> InputStream): ReceiptReceptionResponse {
        access.requireHost(gatheringId, settlementUnitId, userId)
        val mediaType = photos.mediaType(contentType, contentLength)
        admission.enter(userId).use {
            val bytes = photos.read(mediaType, source)
            access.requireHost(gatheringId, settlementUnitId, userId)
            return ReceiptReceptionResponse(bytes.size, mediaType)
        }
    }
}
