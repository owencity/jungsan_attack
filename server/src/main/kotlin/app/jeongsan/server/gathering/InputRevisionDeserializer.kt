package app.jeongsan.server.gathering

import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/** 소수를 잘라 우연히 현재 버전과 같아지는 요청을 막는다. null은 구버전 앱 계약을 유지한다. */
class InputRevisionDeserializer : ValueDeserializer<Long?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Long? {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return context.reportInputMismatch(Long::class.javaObjectType, "expectedInputRevision은 정수 숫자여야 합니다.")
        }
        return parser.longValue
    }
}
