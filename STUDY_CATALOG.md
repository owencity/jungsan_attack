# PR 기반 Top-Down 학습 자료 Catalog

## 1. 역할

이 문서는 PR의 변경 코드와 학습 자료의 정확한 항목을 연결하기 위한 reference
metadata다. 책의 내용을 설명하거나 요약하지 않는다.

AI는 이 Catalog에 등록된 Item, Chapter, Section만 추천할 수 있다. 제목이나 번호가
등록되지 않은 자료는 모델의 일반 지식으로 보충하지 않는다.

## 2. Catalog 범위

- Java 기본기는 정확한 페이지나 Chapter를 추정하지 않고 개념 단위로 관리한다.
- Effective Java와 함수형 프로그래밍 with 자바는 입력에 번호와 제목이 명시된 항목만
  등록한다.
- 현재 입력에는 두 책의 전체 목차가 자리표시자로만 제공됐다. 따라서 요청문에서 번호와
  제목이 실제로 확인되는 항목만 등록했다.
- 이후 실제 목차가 제공되면 확인된 범위까지만 항목을 추가한다.
- Kotlin 기본·고급은 사용자가 제공한 강의 목차(2026-10-04) 전체를 강의 단위로 등록했다(`KB-01`~`KB-20`,
  `KA-01`~`KA-26`). 강의 번호와 제목은 원문 그대로이며, `keywords`·`mapping_hint`는 diff 대조용으로 덧붙인 것이다.
  이 서버(`server`·`core`)와 앱이 Kotlin이라 PR의 문법·언어 기능은 대부분 이 두 카테고리로 연결된다.

---

## 3. Java 기본기

### JAVA-OBJECT-CLASS

- source: Java 기본기
- title: 객체 / 클래스
- keywords:
  - class
  - object
  - instance
  - field
  - method
- mapping_hint:
  새 클래스나 객체의 상태와 동작이 추가되거나, 객체 간 책임이 변경되는 diff에서 선수 지식으로 추천한다.

### JAVA-CONSTRUCTOR

- source: Java 기본기
- title: 생성자
- keywords:
  - constructor
  - new
  - this
  - initialization
- mapping_hint:
  생성자, 객체 초기화 순서 또는 생성 시 필수 값이 추가·변경되는 diff에서 참고한다.

### JAVA-INHERITANCE

- source: Java 기본기
- title: 상속
- keywords:
  - extends
  - super
  - inheritance
  - override
- mapping_hint:
  클래스 상속 관계를 추가하거나 부모 클래스의 동작을 재사용·변경하는 diff에서 참고한다.

### JAVA-POLYMORPHISM

- source: Java 기본기
- title: 다형성
- keywords:
  - extends
  - implements
  - override
  - interface
  - abstract
- mapping_hint:
  부모 타입 참조, 오버라이딩, 인터페이스 구현을 통해 구현체를 교체하는 코드가 등장할 때 선수 지식으로 추천한다.

### JAVA-ABSTRACT-CLASS

- source: Java 기본기
- title: 추상 클래스
- keywords:
  - abstract class
  - abstract method
  - extends
  - template method
- mapping_hint:
  추상 클래스 또는 추상 메서드가 추가·변경되고 하위 클래스가 이를 구현하는 diff에서 참고한다.

### JAVA-INTERFACE

- source: Java 기본기
- title: 인터페이스
- keywords:
  - interface
  - implements
  - default method
  - contract
- mapping_hint:
  인터페이스 계약을 새로 정의하거나 구현체·기본 메서드를 변경하는 diff에서 선수 지식으로 추천한다.

### JAVA-INNER-CLASS

- source: Java 기본기
- title: 내부 클래스
- keywords:
  - inner class
  - nested class
  - static nested class
  - anonymous class
- mapping_hint:
  멤버 클래스, 정적 중첩 클래스 또는 익명 클래스가 실제로 추가·변경되는 diff에서 참고한다.

### JAVA-ACCESS-MODIFIER

- source: Java 기본기
- title: 접근 제어자
- keywords:
  - public
  - protected
  - private
  - package-private
- mapping_hint:
  타입이나 멤버의 공개 범위를 변경해 호출 가능 범위와 캡슐화가 달라지는 diff에서 참고한다.

### JAVA-OBJECT

- source: Java 기본기
- title: Object
- keywords:
  - Object
  - toString
  - getClass
  - clone
- mapping_hint:
  `Object`의 공통 메서드를 재정의하거나 런타임 타입을 직접 다루는 코드가 변경될 때 참고한다.

### JAVA-EQUALS-HASHCODE

- source: Java 기본기
- title: equals / hashCode
- keywords:
  - equals
  - hashCode
  - equality
  - HashMap
  - HashSet
- mapping_hint:
  동등성 구현, 값 객체, 복합키 또는 해시 기반 컬렉션의 키 동작이 추가·변경되는 diff에서 참고한다.

### JAVA-EXCEPTION

- source: Java 기본기
- title: 예외
- keywords:
  - try
  - catch
  - throw
  - throws
  - exception
- mapping_hint:
  예외를 발생·전파·변환하거나 자원 정리 흐름을 변경하는 diff에서 선수 지식으로 추천한다.

### JAVA-COLLECTION

- source: Java 기본기
- title: 컬렉션
- keywords:
  - List
  - Set
  - Map
  - Queue
  - Collection
- mapping_hint:
  컬렉션 종류의 선택, 원소 추가·조회·정렬 또는 중복 처리 방식이 핵심인 diff에서 참고한다.

### JAVA-GENERICS

- source: Java 기본기
- title: 제네릭
- keywords:
  - generic
  - type parameter
  - wildcard
  - extends bound
  - super bound
- mapping_hint:
  타입 매개변수, 제네릭 메서드, 제한된 타입 또는 와일드카드가 추가·변경되는 diff에서 참고한다.

### JAVA-ENUM

- source: Java 기본기
- title: enum
- keywords:
  - enum
  - values
  - valueOf
  - switch
- mapping_hint:
  닫힌 상태 집합을 enum으로 추가하거나 enum 멤버·상태 분기를 변경하는 diff에서 참고한다.

### JAVA-ANNOTATION

- source: Java 기본기
- title: annotation
- keywords:
  - annotation
  - @interface
  - retention
  - target
  - reflection
- mapping_hint:
  사용자 정의 애너테이션이나 메타 애너테이션의 정의·처리 방식이 변경되는 diff에서 참고한다.

### JAVA-LAMBDA

- source: Java 기본기
- title: lambda
- keywords:
  - lambda
  - functional interface
  - Predicate
  - Function
  - Consumer
- mapping_hint:
  람다식이나 함수형 인터페이스를 사용한 동작 전달이 실제로 추가·변경되는 diff에서 참고한다.

### JAVA-METHOD-REFERENCE

- source: Java 기본기
- title: method reference
- keywords:
  - method reference
  - ::
  - constructor reference
  - functional interface
- mapping_hint:
  메서드 참조나 생성자 참조가 추가되어 함수형 인터페이스의 구현으로 사용되는 diff에서 참고한다.

### JAVA-STREAM

- source: Java 기본기
- title: stream
- keywords:
  - stream
  - filter
  - map
  - flatMap
  - collect
  - toList
- mapping_hint:
  Stream 파이프라인이 추가되거나 중간·최종 연산의 구성과 데이터 흐름이 변경되는 diff에서 참고한다.

### JAVA-OPTIONAL

- source: Java 기본기
- title: Optional
- keywords:
  - Optional
  - ofNullable
  - orElse
  - orElseGet
  - ifPresent
- mapping_hint:
  값의 부재를 `Optional`로 표현하거나 Optional 체인의 처리 방식이 추가·변경되는 diff에서 참고한다.

### JAVA-THREAD

- source: Java 기본기
- title: Thread
- keywords:
  - Thread
  - Runnable
  - start
  - run
  - interrupt
- mapping_hint:
  스레드 생성·실행·중단 또는 생명주기를 직접 다루는 코드가 변경되는 diff에서 참고한다.

### JAVA-CONCURRENCY

- source: Java 기본기
- title: 동시성
- keywords:
  - synchronized
  - volatile
  - Lock
  - ExecutorService
  - CompletableFuture
- mapping_hint:
  공유 상태, 동기화, 실행기 또는 비동기 작업의 순서와 가시성이 변경되는 diff에서 참고한다.

---

## 4. Effective Java

### EJ-18

- source: Effective Java
- item: 18
- title: 상속보다는 컴포지션을 사용하라
- keywords:
  - extends
  - inheritance
  - composition
  - delegation
- mapping_hint:
  상속 관계를 추가·수정하거나 상속 대신 별도 객체에 동작을 위임하도록 구조를 변경하는 diff에서 참고한다.

---

## 5. 함수형 프로그래밍 with 자바

### FJ-06-03

- source: 함수형 프로그래밍 with 자바
- chapter: 6
- section: 6.3
- title: 스트림 파이프라인 구축하기
- keywords:
  - stream
  - filter
  - map
  - flatMap
  - reduce
  - collect
  - toList
- mapping_hint:
  Stream 파이프라인이 추가되거나 중간·최종 연산의 연결 구조가 변경되는 diff에서 참고한다.

---

## 6. Kotlin 기본

출처: 사용자가 제공한 강의 목차(Kotlin 기본편). 강의 소개·강의 자료·마무리 영상과 재생 시간은 학습 항목에서 뺐다.
결과에는 `{N}강, {title}`로 표시한다. 등록된 강의 번호와 제목만 쓴다.

### KB-01

- source: Kotlin 기본
- lecture: 1강
- section: 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법
- title: 코틀린에서 변수를 다루는 방법
- keywords:
  - val
  - var
  - lateinit
  - 타입 추론
  - primitive type
- mapping_hint:
  val/var 선택, 타입 추론, 가변성이 바뀌는 선언이 diff에 추가·변경될 때 참고한다.

### KB-02

- source: Kotlin 기본
- lecture: 2강
- section: 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법
- title: 코틀린에서 null을 다루는 방법
- keywords:
  - nullable
  - ?.
  - ?:
  - !!
  - safe call
  - elvis
  - platform type
- mapping_hint:
  nullable 타입, 안전 호출, 엘비스 연산자, `!!`, Java 호출 결과의 플랫폼 타입이 diff에 나타날 때 참고한다.

### KB-03

- source: Kotlin 기본
- lecture: 3강
- section: 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법
- title: 코틀린에서 Type을 다루는 방법
- keywords:
  - is
  - as
  - as?
  - smart cast
  - Any
  - Unit
  - Nothing
  - string template
- mapping_hint:
  타입 검사·캐스팅, 스마트 캐스트, Any/Unit/Nothing 반환 타입이 diff에 쓰일 때 참고한다.

### KB-04

- source: Kotlin 기본
- lecture: 4강
- section: 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법
- title: 코틀린에서 연산자를 다루는 방법
- keywords:
  - ==
  - ===
  - compareTo
  - in
  - ..
  - operator
- mapping_hint:
  동등성(==/===), 비교, 범위(in, ..) 연산이 로직 판단에 쓰이는 diff에서 참고한다.

### KB-05

- source: Kotlin 기본
- lecture: 5강
- section: 섹션 3. 코틀린에서 코드를 제어하는 방법
- title: 코틀린에서 제어문을 다루는 방법
- keywords:
  - if expression
  - when
  - sealed when
  - exhaustive
- mapping_hint:
  if/when을 식으로 쓰거나 when 분기가 상태·enum·sealed 타입을 다루는 diff에서 참고한다.

### KB-06

- source: Kotlin 기본
- lecture: 6강
- section: 섹션 3. 코틀린에서 코드를 제어하는 방법
- title: 코틀린에서 반복문을 다루는 방법
- keywords:
  - for
  - in
  - until
  - downTo
  - step
  - withIndex
  - forEach
- mapping_hint:
  for/while, 범위·진행(until, downTo, step) 반복이 추가·변경된 diff에서 참고한다.

### KB-07

- source: Kotlin 기본
- lecture: 7강
- section: 섹션 3. 코틀린에서 코드를 제어하는 방법
- title: 코틀린에서 예외를 다루는 방법
- keywords:
  - try
  - catch
  - finally
  - throw
  - checked exception
  - use
  - runCatching
- mapping_hint:
  try를 식으로 쓰거나 예외 처리·자원 정리(use)가 바뀌는 diff에서 참고한다. 코틀린에 checked exception이 없다는 점과 연결된다.

### KB-08

- source: Kotlin 기본
- lecture: 8강
- section: 섹션 3. 코틀린에서 코드를 제어하는 방법
- title: 코틀린에서 함수를 다루는 방법
- keywords:
  - fun
  - default parameter
  - named argument
  - expression body
  - vararg
- mapping_hint:
  함수 선언 형태, 기본값·이름 붙인 인자, 식 본문 함수가 diff에 추가·변경될 때 참고한다.

### KB-09

- source: Kotlin 기본
- lecture: 9강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 클래스를 다루는 방법
- keywords:
  - class
  - constructor
  - init
  - property
  - getter
  - setter
  - backing field
- mapping_hint:
  클래스·주 생성자·init 블록·프로퍼티(커스텀 getter/setter, backing field)가 추가·변경될 때 참고한다.

### KB-10

- source: Kotlin 기본
- lecture: 10강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 상속을 다루는 방법
- keywords:
  - open
  - override
  - abstract
  - interface
  - final
- mapping_hint:
  open/override/abstract로 상속 관계가 바뀌거나, 기본이 final이라 프록시(Spring·JPA)와 부딪히는 diff에서 참고한다.

### KB-11

- source: Kotlin 기본
- lecture: 11강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 접근 제어를 다루는 방법
- keywords:
  - private
  - protected
  - internal
  - public
  - visibility
- mapping_hint:
  internal/private 등 가시성이 바뀌어 모듈·패키지 경계가 달라지는 diff에서 참고한다.

### KB-12

- source: Kotlin 기본
- lecture: 12강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 object 키워드를 다루는 방법
- keywords:
  - object
  - companion object
  - singleton
  - anonymous object
  - @JvmStatic
- mapping_hint:
  object 싱글턴, companion object의 팩토리·상수, 익명 객체가 diff에 쓰일 때 참고한다.

### KB-13

- source: Kotlin 기본
- lecture: 13강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 중첩 클래스를 다루는 방법
- keywords:
  - nested class
  - inner class
  - outer reference
- mapping_hint:
  중첩(nested)과 내부(inner) 클래스가 추가되어 바깥 객체 참조 여부가 달라지는 diff에서 참고한다.

### KB-14

- source: Kotlin 기본
- lecture: 14강
- section: 섹션 4. 코틀린에서의 OOP
- title: 코틀린에서 다양한 클래스를 다루는 방법
- keywords:
  - data class
  - enum class
  - sealed class
  - sealed interface
  - copy
  - equals
- mapping_hint:
  data class(equals·copy), enum class, sealed class/interface로 상태·결과를 모델링하는 diff에서 참고한다.

### KB-15

- source: Kotlin 기본
- lecture: 15강
- section: 섹션 5. 코틀린에서의 FP
- title: 코틀린에서 배열과 컬렉션을 다루는 방법
- keywords:
  - Array
  - List
  - MutableList
  - Map
  - Set
  - listOf
  - mutableListOf
  - immutable
- mapping_hint:
  불변/가변 컬렉션 선택, List·Map·Set 생성과 변경이 diff에 나타날 때 참고한다.

### KB-16

- source: Kotlin 기본
- lecture: 16강
- section: 섹션 5. 코틀린에서의 FP
- title: 코틀린에서 다양한 함수를 다루는 방법
- keywords:
  - extension function
  - infix
  - inline
  - local function
  - top-level function
- mapping_hint:
  확장 함수, infix·지역·최상위 함수가 추가되어 호출 위치와 책임이 달라지는 diff에서 참고한다.

### KB-17

- source: Kotlin 기본
- lecture: 17강
- section: 섹션 5. 코틀린에서의 FP
- title: 코틀린에서 람다를 다루는 방법
- keywords:
  - lambda
  - closure
  - it
  - function type
  - trailing lambda
- mapping_hint:
  람다·함수 타입 파라미터·클로저가 상태를 캡처하는 코드가 diff에 추가될 때 참고한다.

### KB-18

- source: Kotlin 기본
- lecture: 18강
- section: 섹션 5. 코틀린에서의 FP
- title: 코틀린에서 컬렉션을 함수형으로 다루는 방법
- keywords:
  - filter
  - map
  - groupBy
  - associate
  - sumOf
  - fold
  - first
  - flatMap
- mapping_hint:
  filter/map/groupBy/associate/sumOf 같은 컬렉션 함수 체인이 추가·변경되는 diff에서 참고한다.

### KB-19

- source: Kotlin 기본
- lecture: 19강
- section: 섹션 6. 추가적으로 알아두어야 할 코틀린 특성
- title: 코틀린의 이모저모
- keywords:
  - typealias
  - as import
  - destructuring
  - componentN
  - label
  - takeIf
  - takeUnless
- mapping_hint:
  typealias, 구조 분해, 레이블 return, takeIf 같은 문법이 diff에 쓰일 때 참고한다.

### KB-20

- source: Kotlin 기본
- lecture: 20강
- section: 섹션 6. 추가적으로 알아두어야 할 코틀린 특성
- title: 코틀린의 scope function
- keywords:
  - let
  - run
  - apply
  - also
  - with
  - this
  - it
- mapping_hint:
  let/run/apply/also/with로 객체를 다루며 반환값이 무엇인지가 로직에 영향을 주는 diff에서 참고한다.

---

## 7. Kotlin 고급

출처: 사용자가 제공한 강의 목차(Kotlin 고급편). 강의 소개·강의 자료·마무리 영상과 재생 시간은 학습 항목에서 뺐다.
결과에는 `{N}강, {title}`로 표시한다. 등록된 강의 번호와 제목만 쓴다.

### KA-01

- source: Kotlin 고급
- lecture: 1강
- section: 섹션 2. 제네릭
- title: 제네릭과 타입 파라미터
- keywords:
  - generic
  - type parameter
  - <T>
- mapping_hint:
  타입 파라미터를 받는 클래스·함수가 새로 추가되거나 바뀌는 diff에서 참고한다.

### KA-02

- source: Kotlin 고급
- lecture: 2강
- section: 섹션 2. 제네릭
- title: 배열과 리스트, 제네릭과 무공변
- keywords:
  - invariance
  - Array
  - List
  - 무공변
- mapping_hint:
  제네릭 타입끼리 대입이 안 되거나 Array/List 변환이 들어간 diff에서 참고한다.

### KA-03

- source: Kotlin 고급
- lecture: 3강
- section: 섹션 2. 제네릭
- title: 공변과 반공변
- keywords:
  - out
  - in
  - covariance
  - contravariance
- mapping_hint:
  out/in 변성 키워드나 생산자·소비자 역할의 제네릭 타입이 diff에 나타날 때 참고한다.

### KA-04

- source: Kotlin 고급
- lecture: 4강
- section: 섹션 2. 제네릭
- title: 선언 지점 변성 / 사용 지점 변성
- keywords:
  - declaration-site variance
  - use-site variance
  - out T
  - in T
- mapping_hint:
  클래스 선언에 변성을 붙이거나 사용하는 자리에서 out/in을 지정하는 diff에서 참고한다.

### KA-05

- source: Kotlin 고급
- lecture: 5강
- section: 섹션 2. 제네릭
- title: 제네릭 제약과 제네릭 함수
- keywords:
  - upper bound
  - where
  - T : Comparable
  - generic function
- mapping_hint:
  타입 상한(T : X)·where 제약이 붙은 제네릭 함수가 추가·변경될 때 참고한다.

### KA-06

- source: Kotlin 고급
- lecture: 6강
- section: 섹션 2. 제네릭
- title: 타입 소거와 Star Projection
- keywords:
  - type erasure
  - star projection
  - *
  - reified
- mapping_hint:
  런타임에 제네릭 타입 정보가 필요하거나 `*` 프로젝션·reified가 쓰인 diff에서 참고한다.

### KA-07

- source: Kotlin 고급
- lecture: 7강
- section: 섹션 2. 제네릭
- title: 제네릭 용어 정리 및 간단한 팁
- keywords:
  - generic terminology
  - Nothing
  - variance tips
- mapping_hint:
  제네릭·변성 설계가 여러 곳에 걸쳐 바뀌어 용어를 정리해 볼 필요가 있는 diff에서 참고한다.

### KA-08

- source: Kotlin 고급
- lecture: 8강
- section: 섹션 3. 지연과 위임
- title: lateinit과 lazy()
- keywords:
  - lateinit
  - lazy
  - initialization
- mapping_hint:
  lateinit 프로퍼티나 by lazy 지연 초기화가 추가되어 초기화 시점이 바뀌는 diff에서 참고한다.

### KA-09

- source: Kotlin 고급
- lecture: 9강
- section: 섹션 3. 지연과 위임
- title: by lazy의 원리와 위임 프로퍼티
- keywords:
  - by lazy
  - delegated property
  - getValue
  - setValue
  - thread safety
- mapping_hint:
  by 위임 프로퍼티의 동작·스레드 안전성이 로직이나 동시성에 영향을 주는 diff에서 참고한다.

### KA-10

- source: Kotlin 고급
- lecture: 10강
- section: 섹션 3. 지연과 위임
- title: 코틀린의 표준 위임 객체
- keywords:
  - Delegates.observable
  - Delegates.vetoable
  - notNull
  - map delegation
- mapping_hint:
  observable/vetoable/notNull 같은 표준 위임 객체가 쓰인 diff에서 참고한다.

### KA-11

- source: Kotlin 고급
- lecture: 11강
- section: 섹션 3. 지연과 위임
- title: 위임과 관련된 몇 가지 추가 기능
- keywords:
  - class delegation
  - by
  - provideDelegate
- mapping_hint:
  클래스 위임(by)으로 구현을 넘기거나 위임 제공자를 쓰는 diff에서 참고한다.

### KA-12

- source: Kotlin 고급
- lecture: 12강
- section: 섹션 3. 지연과 위임
- title: Iterable과 Sequence (feat. JMH)
- keywords:
  - Sequence
  - asSequence
  - lazy evaluation
  - Iterable
- mapping_hint:
  큰 컬렉션을 asSequence로 지연 처리하거나 컬렉션 체인 비용이 쟁점인 diff에서 참고한다.

### KA-13

- source: Kotlin 고급
- lecture: 13강
- section: 섹션 4. 복잡한 함수형 프로그래밍
- title: 고차 함수와 함수 리터럴
- keywords:
  - higher-order function
  - function literal
  - lambda
  - anonymous function
- mapping_hint:
  함수를 인자·반환값으로 다루는 고차 함수가 추가·변경될 때 참고한다.

### KA-14

- source: Kotlin 고급
- lecture: 14강
- section: 섹션 4. 복잡한 함수형 프로그래밍
- title: 복잡한 함수 타입과 고차 함수의 단점
- keywords:
  - function type
  - Function object
  - allocation
  - closure cost
- mapping_hint:
  복잡한 함수 타입이나 고차 함수 호출이 많아 객체 생성·가독성 비용이 쟁점인 diff에서 참고한다.

### KA-15

- source: Kotlin 고급
- lecture: 15강
- section: 섹션 4. 복잡한 함수형 프로그래밍
- title: inline 함수 자세히 살펴보기
- keywords:
  - inline
  - noinline
  - crossinline
  - reified
  - non-local return
- mapping_hint:
  inline/noinline/crossinline·reified가 쓰인 함수나 람다 안 return 동작이 바뀌는 diff에서 참고한다.

### KA-16

- source: Kotlin 고급
- lecture: 16강
- section: 섹션 4. 복잡한 함수형 프로그래밍
- title: SAM과 reference
- keywords:
  - SAM
  - fun interface
  - ::
  - callable reference
  - method reference
- mapping_hint:
  fun interface(SAM 변환)나 `::` 참조가 쓰여 Java 인터페이스와 맞물리는 diff에서 참고한다.

### KA-17

- source: Kotlin 고급
- lecture: 17강
- section: 섹션 5. 연산자 오버로딩과 Kotlin DSL
- title: 연산자 오버로딩
- keywords:
  - operator
  - plus
  - get
  - invoke
  - compareTo
- mapping_hint:
  operator fun으로 연산자를 정의하거나 오버로딩된 연산자를 쓰는 diff에서 참고한다.

### KA-18

- source: Kotlin 고급
- lecture: 18강
- section: 섹션 5. 연산자 오버로딩과 Kotlin DSL
- title: Kotlin DSL 직접 만들어보기
- keywords:
  - DSL
  - lambda with receiver
  - builder
  - @DslMarker
- mapping_hint:
  수신 객체 지정 람다로 빌더·DSL을 만들거나 쓰는 diff(Gradle KTS 포함)에서 참고한다.

### KA-19

- source: Kotlin 고급
- lecture: 19강
- section: 섹션 5. 연산자 오버로딩과 Kotlin DSL
- title: DSL 활용 사례 살펴보기
- keywords:
  - DSL usage
  - Gradle Kotlin DSL
  - test DSL
  - Kotest
- mapping_hint:
  Gradle Kotlin DSL·테스트 DSL(Kotest 등)처럼 기존 DSL을 활용하는 코드가 바뀌는 diff에서 참고한다.

### KA-20

- source: Kotlin 고급
- lecture: 20강
- section: 섹션 6. 어노테이션과 리플렉션
- title: 코틀린의 어노테이션
- keywords:
  - annotation
  - use-site target
  - @field:
  - @get:
  - @param:
- mapping_hint:
  어노테이션의 적용 대상(@field:, @get: 등)이 Spring·JPA·검증 동작에 영향을 주는 diff에서 참고한다.

### KA-21

- source: Kotlin 고급
- lecture: 21강
- section: 섹션 6. 어노테이션과 리플렉션
- title: 코틀린의 리플렉션
- keywords:
  - reflection
  - KClass
  - ::class
  - KProperty
  - kotlin-reflect
- mapping_hint:
  KClass·KProperty 등 리플렉션으로 런타임에 타입·멤버를 다루는 diff에서 참고한다.

### KA-22

- source: Kotlin 고급
- lecture: 22강
- section: 섹션 6. 어노테이션과 리플렉션
- title: 리플렉션 활용 - 나만의 DI 컨테이너 만들기
- keywords:
  - DI container
  - reflection
  - constructor injection
- mapping_hint:
  리플렉션 기반으로 객체를 생성·주입하는 구조가 추가되거나 DI 동작을 이해해야 하는 diff에서 참고한다.

### KA-23

- source: Kotlin 고급
- lecture: 23강
- section: 섹션 6. 어노테이션과 리플렉션
- title: 리플렉션 활용 - 타입 안전 이종 컨테이너와 슈퍼 타입 토큰
- keywords:
  - type-safe heterogeneous container
  - super type token
  - KType
  - typeOf
- mapping_hint:
  제네릭 타입 정보를 런타임까지 보존해야 하는 컨테이너·역직렬화 코드가 diff에 나타날 때 참고한다.

### KA-24

- source: Kotlin 고급
- lecture: 24강
- section: 섹션 7. 코틀린을 더 알아보자!
- title: 유용한 코틀린 표준 라이브러리 함수들
- keywords:
  - require
  - check
  - error
  - repeat
  - TODO
  - runCatching
  - buildList
- mapping_hint:
  require/check/error 같은 표준 함수로 검증·실패를 표현하는 diff에서 참고한다.

### KA-25

- source: Kotlin 고급
- lecture: 25강
- section: 섹션 7. 코틀린을 더 알아보자!
- title: 꼬리 재귀 함수와 인라인 클래스, multiple catch
- keywords:
  - tailrec
  - value class
  - inline class
  - @JvmInline
  - multiple catch
- mapping_hint:
  tailrec 재귀, value class(@JvmInline)로 감싼 값 타입, 여러 예외 처리가 diff에 쓰일 때 참고한다.

### KA-26

- source: Kotlin 고급
- lecture: 26강
- section: 섹션 7. 코틀린을 더 알아보자!
- title: 유용한 k-도구들!!
- keywords:
  - kotlin tools
  - kapt
  - ksp
  - kotlinx
  - detekt
  - ktlint
- mapping_hint:
  코틀린 빌드·분석 도구(kapt/ksp, 린트 등) 설정이 diff에서 바뀔 때 참고한다.
