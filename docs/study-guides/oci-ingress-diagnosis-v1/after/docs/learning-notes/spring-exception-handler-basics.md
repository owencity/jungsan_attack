# Spring 예외 처리: 누가 함수를 호출하고 왜 500이 반환되는가

작성: 2026-10-11. 사용자가 다시 읽기 위해 요청한 설명 노트다.
코드 기준은 운영에 배포된 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`이다.
이 문서는 해당 코드의 동작을 설명하며 새로운 예외 처리 구현이나 수정 완료를 뜻하지 않는다.

## 먼저 볼 코드

[`GlobalExceptionHandler.handleUnexpected`](https://github.com/owencity/jungsan_attack/blob/3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420/server/src/main/kotlin/app/jeongsan/server/common/GlobalExceptionHandler.kt)을 먼저 읽는다.
같은 파일의 `handleApi`와 비교하면 로그인 실패가 왜 401이고 없는 경로가 왜 현재 500인지 볼 수 있다.

```kotlin
@ExceptionHandler(Exception::class)
fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("처리되지 않은 예외", e)
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다."))
}
```

## 1. 트리거는 어디에 있는가

`@ExceptionHandler(Exception::class)`가 Spring에 예외 처리 함수를 등록하는 표시다.
클래스의 `@RestControllerAdvice`는 여러 컨트롤러의 요청 처리에서 이 예외 처리를 공통으로 사용하게 한다.
Spring이 HTTP 요청을 처리하다 예외를 만나면 예외 타입에 맞는 처리 함수를 찾아 호출한다.
애너테이션 자체가 코틀린의 `try/catch`를 실행하는 것은 아니다. 애너테이션 정보를 읽고 실행하는 주체는 Spring이다.

실제 예외 처리에는 컨트롤러 내부의 처리 함수, 다른 Advice, 예외 타입의 구체성, Advice 우선순위가 관여한다.
이 저장소에서는 같은 Advice 안에서 `ApiException` 처리와 넓은 `Exception` 처리를 비교하면 된다.
HTTP 요청 처리와 관계없는 기동 실패나 별도 배치 스레드의 예외가 이 함수로 모두 들어오는 것은 아니다.

## 2. 함수 선언을 읽는 방법

| 표현 | 뜻 | 코드에서 확인할 것 |
|---|---|---|
| `fun` | 함수 선언 | 호출될 동작을 정의한다 |
| `handleUnexpected` | 함수 이름 | 이름 때문에 자동 호출되는 것이 아니라 애너테이션으로 등록된다 |
| `e: Exception` | `e`라는 매개변수의 타입이 Exception | 실제 발생한 하위 예외 객체도 전달될 수 있다 |
| `Exception::class` | Exception 클래스의 타입 정보를 가리킨다 | Exception 객체를 새로 만드는 표현이 아니다 |
| `: ResponseEntity<ErrorResponse>` | 함수 반환 타입 | HTTP 상태와 ErrorResponse 본문을 담은 응답 객체를 반환한다 |
| `return` | 호출한 곳에 결과를 돌려준다 | 함수가 만든 응답 객체를 Spring에 반환한다 |

`e`에는 실제 예외 객체가 들어간다. `NoResourceFoundException`도 Exception의 하위 타입이므로 이 매개변수로 받을 수 있다.

## 3. 로그 기록과 응답 만들기는 다른 단계다

```kotlin
log.error("처리되지 않은 예외", e)
```

이 줄은 예외 내용과 발생 위치를 서버 로그에 기록한다. HTTP 응답 코드를 정하는 줄은 아래다.

```kotlin
ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    .body(ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다."))
```

1. `status(...)`로 HTTP 상태 500을 지정한다.
2. `ErrorResponse(...)`로 응답에 담을 데이터를 만든다. 코틀린 생성자 호출에는 Java의 `new`가 없다.
3. `body(...)`로 해당 데이터를 본문에 담는다.
4. 함수가 객체를 반환하면 Spring이 본문을 JSON으로 직렬화하고 클라이언트에게 응답한다.

점으로 이어진 호출은 앞 호출에서 반환한 객체의 다음 함수를 부르는 표현이다.
`HttpStatus.INTERNAL_SERVER_ERROR`라는 상수는 HTTP 500을 나타낸다.

## 4. 이번 운영 요청이 실제로 지나간 순서

```text
GET /api/v1/me
    ↓
등록된 GET 핸들러가 없음
    ↓
Spring의 정적 리소스 처리에서도 해당 경로를 찾지 못함
    ↓
NoResourceFoundException 발생
    ↓
해당 예외를 전용으로 처리하는 함수가 이 Advice에 없음
    ↓
Exception 범위의 handleUnexpected(e)가 선택됨
    ↓
함수가 명시적으로 HTTP 500 + INTERNAL_ERROR를 반환
    ↓
Spring이 그 응답을 클라이언트에게 보냄
```

운영 로그는 `NoResourceFoundException: No static resource api/v1/me.`였다.
본래 없는 리소스의 상태는 404다. 현재 구현은 이 예외를 별도로 분류하지 않아 500으로 바꾸고 있다.
Spring 기본 처리의 해당 예외 상태도 [404](https://docs.spring.io/spring-framework/docs/6.2.5/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html)다.

## 5. 왜 로그인 실패는 401이 유지되는가

같은 파일에 `@ExceptionHandler(ApiException::class)`로 등록된 `handleApi`가 있다.
로그인 실패는 이 타입 계층의 예외를 사용하므로 그 처리 함수가 선택되고 예외에 지정된 401 상태를 반환한다.
넓은 Exception 함수가 모든 구체적인 예외 처리보다 먼저 선택되는 것은 아니다.

현재 사용자 정보 계약과 코드는 `GET /api/v1/auth/me`다.
[`AuthController.me`](https://github.com/owencity/jungsan_attack/blob/3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420/server/src/main/kotlin/app/jeongsan/server/user/AuthController.kt)와
[`LoginUserArgumentResolver.resolveArgument`](https://github.com/owencity/jungsan_attack/blob/3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420/server/src/main/kotlin/app/jeongsan/server/common/LoginUser.kt)을 이어 읽는다.
토큰 없이 이 경로를 호출하면 401이다. `/api/v1/me`는 `/auth/me`의 별칭으로 등록되어 있지 않다.

## 6. 직접 확인할 질문

- [ ] 이 함수를 직접 호출하는 내 코드가 없어도 실행되는 이유를 설명할 수 있는가?
- [ ] 로그 기록과 HTTP 500 결정 중 어느 줄이 각각 담당하는가?
- [ ] `e: Exception`에 하위 타입 객체를 넣을 수 있는 이유를 설명할 수 있는가?
- [ ] `handleApi`가 반환하는 상태와 `handleUnexpected`가 반환하는 상태의 차이를 찾았는가?
- [ ] 없는 경로 404를 보완할 때, 실제 서버 오류의 500까지 바꾸면 안 되는 이유를 설명할 수 있는가?

## 7. 원격 main 카탈로그에서 확인한 읽을 위치

다음은 사용자가 질문한 기존 코드를 읽기 위한 위치다. 이 문서 PR에 새 실행 코드가 추가됐다는 뜻은 아니다.
자료와 번호는 위 기준 커밋의 `STUDY_CATALOG.md`에 등록된 것을 사용했다.

- Kotlin 기본 **7강 「코틀린에서 예외를 다루는 방법」** (`KB-07`): 예외의 발생과 전파를 읽는다. Spring의 함수 선택은 별도의 프레임워크 동작이다.
- Kotlin 기본 **8강 「코틀린에서 함수를 다루는 방법」** (`KB-08`): `fun`, 매개변수 타입, 반환 타입과 `return`을 본다.
- 자바의 정석 **8장 1.2 「예외 클래스의 계층구조」 p.445** (`JST-08-01-02`): Exception 타입으로 하위 예외 객체를 받을 수 있는 관계를 본다.
- 자바의 정석 **8장 1.3 「예외 처리하기 - try-catch문」 p.446**, **1.4 「try-catch문에서의 흐름」 p.449** (`JST-08-01-03`, `JST-08-01-04`): 예외가 발생한 뒤 정상 흐름에서 예외 처리로 이동하는 것을 본다.
- Spring 기술: [`@ExceptionHandler`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)에서 예외 타입 매칭과 Advice 적용 범위를 본다.

이 문서를 저장한 것은 학습 완료를 뜻하지 않는다. 위 체크는 사용자가 직접 읽고 기록한다.
