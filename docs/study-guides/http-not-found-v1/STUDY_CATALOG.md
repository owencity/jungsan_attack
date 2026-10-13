# PR 기반 Top-Down 학습 자료 Catalog

## 1. 역할

이 문서는 PR의 변경 코드와 학습 자료의 정확한 항목을 연결하기 위한 reference
metadata다. 책의 내용을 설명하거나 요약하지 않는다.

AI는 이 Catalog에 등록된 Item, Chapter, Section만 추천할 수 있다. 제목이나 번호가
등록되지 않은 자료는 모델의 일반 지식으로 보충하지 않는다.

## 2. Catalog 범위

- Java 기본기는 정확한 페이지나 Chapter를 추정하지 않고 개념 단위로 관리한다.
- Effective Java와 함수형 프로그래밍 with 자바는 사용자가 제공한 실제 목차(2026-09-29)로 전체를 등록했다 —
  Effective Java 아이템 1~90(`EJ-01`~`EJ-90`), 함수형 프로그래밍 with 자바 15장 71개 절(`FJ-01-01`~`FJ-15-04`,
  '핵심 요약' 제외). 번호·제목·장은 원문 그대로이며, `keywords`·`mapping_hint`는 diff 대조용으로 덧붙인 것이다.
- 자바의 정석은 사용자가 고른 6~16장 선별 목차(2026-09-29) 233개 소절을 장·절·소절·페이지와 함께 등록했다(`JST-06-01-01`~
  `JST-16-02-03`). Java 기본기 카테고리에서 읽을 위치(페이지)를 알려주는 데 쓴다.
- 목차에 없는 번호·제목은 등록하지 않는다. 새 자료를 추가할 때도 제공된 목차 원문의 범위까지만 등록한다.
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

출처: 사용자가 제공한 목차(Effective Java 3판, 2026-09-29). 아이템 번호·제목·장은 원문 그대로다. 결과에는 `Item {번호}, {title}`로 표시한다.

### EJ-01

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 1
- title: 생성자 대신 정적 팩터리 메서드를 고려하라
- keywords:
  - static factory
  - of
  - from
  - valueOf
  - companion object
- mapping_hint:
  생성자 대신 이름 있는 정적 팩터리(of/from, companion object 함수)로 객체를 만드는 코드가 추가·변경될 때 참고한다.

### EJ-02

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 2
- title: 생성자에 매개변수가 많다면 빌더를 고려하라
- keywords:
  - builder
  - many parameters
  - named argument
  - default parameter
- mapping_hint:
  매개변수가 많은 생성자나 빌더(코틀린은 이름 붙인 인자·기본값)로 객체 생성 방식이 바뀔 때 참고한다.

### EJ-03

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 3
- title: private 생성자나 열거 타입으로 싱글턴임을 보증하라
- keywords:
  - singleton
  - object
  - enum singleton
  - private constructor
- mapping_hint:
  싱글턴(object, enum, private 생성자)을 선언하거나 바꿀 때 참고한다.

### EJ-04

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 4
- title: 인스턴스화를 막으려거든 private 생성자를 사용하라
- keywords:
  - utility class
  - private constructor
  - object
  - top-level function
- mapping_hint:
  인스턴스가 필요 없는 유틸리티 클래스·최상위 함수 묶음이 추가될 때 참고한다.

### EJ-05

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 5
- title: 자원을 직접 명시하지 말고 의존 객체 주입을 사용하라
- keywords:
  - dependency injection
  - constructor injection
  - @Component
  - @Bean
- mapping_hint:
  의존 객체를 직접 생성하지 않고 생성자 주입(Spring Bean)으로 받도록 바뀔 때 참고한다.

### EJ-06

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 6
- title: 불필요한 객체 생성을 피하라
- keywords:
  - object creation
  - regex
  - boxing
  - cache
  - allocation
- mapping_hint:
  반복 경로에서 정규식·박싱·무거운 객체를 매번 만드는 코드가 생길 때 참고한다.

### EJ-07

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 7
- title: 다 쓴 객체 참조를 해제하라
- keywords:
  - memory leak
  - cache
  - listener
  - weak reference
  - obsolete reference
- mapping_hint:
  캐시·리스너·컬렉션에 참조를 쌓아 두어 메모리가 해제되지 않을 수 있는 코드가 추가될 때 참고한다.

### EJ-08

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 8
- title: finalizer와 cleaner 사용을 피하라
- keywords:
  - finalizer
  - cleaner
  - resource cleanup
- mapping_hint:
  finalizer/cleaner로 자원 정리를 맡기는 코드가 등장할 때 참고한다.

### EJ-09

- source: Effective Java
- chapter: 2장 객체 생성과 파괴
- item: 9
- title: try-finally보다는 try-with-resources를 사용하라
- keywords:
  - try-with-resources
  - use
  - AutoCloseable
  - Closeable
- mapping_hint:
  스트림·커넥션 등 닫아야 하는 자원을 try-with-resources(코틀린 use)로 다루는 코드가 바뀔 때 참고한다.

### EJ-10

- source: Effective Java
- chapter: 3장 모든 객체의 공통 메서드
- item: 10
- title: equals는 일반 규약을 지켜 재정의하라
- keywords:
  - equals
  - data class
  - value object
  - identity
- mapping_hint:
  equals를 재정의하거나 data class·값 객체의 동등성 기준이 바뀔 때 참고한다.

### EJ-11

- source: Effective Java
- chapter: 3장 모든 객체의 공통 메서드
- item: 11
- title: equals를 재정의하려거든 hashCode도 재정의하라
- keywords:
  - hashCode
  - equals
  - HashMap
  - HashSet
  - data class
- mapping_hint:
  equals와 함께 hashCode가 쓰이는 Map·Set 키나 data class 필드가 바뀔 때 참고한다.

### EJ-12

- source: Effective Java
- chapter: 3장 모든 객체의 공통 메서드
- item: 12
- title: toString을 항상 재정의하라
- keywords:
  - toString
  - logging
  - data class
  - masking
- mapping_hint:
  toString 출력이 로그·디버깅에 쓰이거나 민감 정보가 찍힐 수 있는 클래스가 바뀔 때 참고한다.

### EJ-13

- source: Effective Java
- chapter: 3장 모든 객체의 공통 메서드
- item: 13
- title: clone 재정의는 주의해서 진행하라
- keywords:
  - clone
  - copy
  - Cloneable
- mapping_hint:
  clone·copy로 객체를 복제하는 코드가 추가될 때 참고한다.

### EJ-14

- source: Effective Java
- chapter: 3장 모든 객체의 공통 메서드
- item: 14
- title: Comparable을 구현할지 고려하라
- keywords:
  - Comparable
  - compareTo
  - Comparator
  - sortedBy
  - compareBy
- mapping_hint:
  Comparable/Comparator로 정렬 기준을 정의·변경할 때 참고한다.

### EJ-15

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 15
- title: 클래스와 멤버의 접근 권한을 최소화하라
- keywords:
  - visibility
  - private
  - internal
  - encapsulation
- mapping_hint:
  클래스·멤버의 공개 범위가 넓어지거나 좁아질 때 참고한다.

### EJ-16

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 16
- title: public 클래스에서는 public 필드가 아닌 접근자 메서드를 사용하라
- keywords:
  - accessor
  - getter
  - public field
  - property
- mapping_hint:
  public 필드 대신 접근자(프로퍼티)로 상태를 노출하도록 바뀔 때 참고한다.

### EJ-17

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 17
- title: 변경 가능성을 최소화하라
- keywords:
  - immutable
  - val
  - final
  - defensive copy
  - data class
- mapping_hint:
  불변 객체(val, 읽기 전용 컬렉션)로 만들거나 가변 상태가 추가될 때 참고한다.

### EJ-18

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 18
- title: 상속보다는 컴포지션을 사용하라
- keywords:
  - extends
  - inheritance
  - composition
  - delegation
- mapping_hint:
  상속 관계를 추가·수정하거나 상속 대신 별도 객체에 동작을 위임하도록 구조를 변경할 때 참고한다.

### EJ-19

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 19
- title: 상속을 고려해 설계하고 문서화하라. 그러지 않았다면 상속을 금지하라
- keywords:
  - open
  - inheritance design
  - final
  - override
- mapping_hint:
  상속을 허용(open)하거나 막는 설계가 바뀌고, 하위 클래스가 깨질 수 있는 재정의가 생길 때 참고한다.

### EJ-20

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 20
- title: 추상 클래스보다는 인터페이스를 우선하라
- keywords:
  - interface
  - abstract class
  - default method
- mapping_hint:
  추상 클래스와 인터페이스 중 무엇으로 타입을 정의할지 바뀔 때 참고한다.

### EJ-21

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 21
- title: 인터페이스는 구현하는 쪽을 생각해 설계하라
- keywords:
  - interface evolution
  - default method
  - implementation
- mapping_hint:
  기존 인터페이스에 메서드(디폴트 구현)를 추가해 구현체들에 영향이 갈 때 참고한다.

### EJ-22

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 22
- title: 인터페이스는 타입을 정의하는 용도로만 사용하라
- keywords:
  - constant interface
  - interface as type
- mapping_hint:
  상수만 담은 인터페이스처럼 인터페이스를 타입 정의 외 용도로 쓸 때 참고한다.

### EJ-23

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 23
- title: 태그 달린 클래스보다는 클래스 계층구조를 활용하라
- keywords:
  - tagged class
  - sealed
  - class hierarchy
  - when
- mapping_hint:
  타입 필드로 분기하던 클래스를 클래스 계층(sealed)으로 바꾸거나 반대로 바꿀 때 참고한다.

### EJ-24

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 24
- title: 멤버 클래스는 되도록 static으로 만들라
- keywords:
  - nested class
  - inner class
  - static member class
- mapping_hint:
  멤버 클래스가 바깥 인스턴스를 참조하는지(inner/nested) 여부가 바뀔 때 참고한다.

### EJ-25

- source: Effective Java
- chapter: 4장 클래스와 인터페이스
- item: 25
- title: 톱레벨 클래스는 한 파일에 하나만 담으라
- keywords:
  - top-level class
  - one file
- mapping_hint:
  한 파일에 여러 최상위 클래스가 생길 때 참고한다.

### EJ-26

- source: Effective Java
- chapter: 5장 제네릭
- item: 26
- title: 로 타입은 사용하지 말라
- keywords:
  - raw type
  - generic
  - List<*>
- mapping_hint:
  제네릭 타입 인자 없이 쓰는 코드(로 타입, 스타 프로젝션 남용)가 나타날 때 참고한다.

### EJ-27

- source: Effective Java
- chapter: 5장 제네릭
- item: 27
- title: 비검사 경고를 제거하라
- keywords:
  - unchecked warning
  - @Suppress
  - cast
- mapping_hint:
  비검사 경고를 억제하거나 unchecked 캐스트가 추가될 때 참고한다.

### EJ-28

- source: Effective Java
- chapter: 5장 제네릭
- item: 28
- title: 배열보다는 리스트를 사용하라
- keywords:
  - array
  - list
  - generic array
- mapping_hint:
  배열과 리스트 중 무엇을 쓸지, 제네릭 배열이 필요한 코드가 바뀔 때 참고한다.

### EJ-29

- source: Effective Java
- chapter: 5장 제네릭
- item: 29
- title: 이왕이면 제네릭 타입으로 만들라
- keywords:
  - generic type
  - type parameter
- mapping_hint:
  Object·Any로 받던 타입을 제네릭 타입으로 바꾸거나 새로 정의할 때 참고한다.

### EJ-30

- source: Effective Java
- chapter: 5장 제네릭
- item: 30
- title: 이왕이면 제네릭 메서드로 만들라
- keywords:
  - generic method
  - type inference
- mapping_hint:
  제네릭 메서드(함수)를 추가하거나 타입 추론에 기대는 호출이 바뀔 때 참고한다.

### EJ-31

- source: Effective Java
- chapter: 5장 제네릭
- item: 31
- title: 한정적 와일드카드를 사용해 API 유연성을 높이라
- keywords:
  - bounded wildcard
  - ? extends
  - ? super
  - out
  - in
  - PECS
- mapping_hint:
  와일드카드·변성(out/in)으로 API가 받는 타입 범위를 넓힐 때 참고한다.

### EJ-32

- source: Effective Java
- chapter: 5장 제네릭
- item: 32
- title: 제네릭과 가변인수를 함께 쓸 때는 신중하라
- keywords:
  - varargs
  - generic varargs
  - @SafeVarargs
- mapping_hint:
  제네릭 타입과 가변인수(vararg)를 함께 쓰는 함수가 생길 때 참고한다.

### EJ-33

- source: Effective Java
- chapter: 5장 제네릭
- item: 33
- title: 타입 안전 이종 컨테이너를 고려하라
- keywords:
  - type-safe heterogeneous container
  - Class<T>
  - KClass
  - type token
- mapping_hint:
  타입을 키로 값을 담는 컨테이너(Class/KClass 키)가 추가될 때 참고한다.

### EJ-34

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 34
- title: int 상수 대신 열거 타입을 사용하라
- keywords:
  - enum
  - int constant
  - status
  - when
- mapping_hint:
  상태·종류를 int/String 상수 대신 열거 타입으로 표현하거나 enum 값이 바뀔 때 참고한다.

### EJ-35

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 35
- title: ordinal 메서드 대신 인스턴스 필드를 사용하라
- keywords:
  - ordinal
  - enum field
- mapping_hint:
  enum의 ordinal에 의존하는 저장·계산 코드가 나타날 때 참고한다.

### EJ-36

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 36
- title: 비트 필드 대신 EnumSet을 사용하라
- keywords:
  - EnumSet
  - bit field
  - flags
- mapping_hint:
  여러 플래그 조합을 비트 필드나 EnumSet으로 다룰 때 참고한다.

### EJ-37

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 37
- title: ordinal 인덱싱 대신 EnumMap을 사용하라
- keywords:
  - EnumMap
  - ordinal index
- mapping_hint:
  enum을 키로 하는 맵·배열 인덱싱 코드가 추가될 때 참고한다.

### EJ-38

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 38
- title: 확장할 수 있는 열거 타입이 필요하면 인터페이스를 사용하라
- keywords:
  - extensible enum
  - interface
- mapping_hint:
  열거 타입을 인터페이스로 확장하려는 구조가 생길 때 참고한다.

### EJ-39

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 39
- title: 명명 패턴보다 애너테이션을 사용하라
- keywords:
  - annotation
  - naming pattern
  - custom annotation
- mapping_hint:
  이름 규칙 대신 애너테이션으로 표시·처리하는 코드(커스텀 애너테이션)가 생길 때 참고한다.

### EJ-40

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 40
- title: @Override 애너테이션을 일관되게 사용하라
- keywords:
  - @Override
  - override
- mapping_hint:
  재정의 표시가 빠지거나 바뀌어 의도치 않은 오버로딩이 생길 수 있을 때 참고한다.

### EJ-41

- source: Effective Java
- chapter: 6장 열거 타입과 애너테이션
- item: 41
- title: 정의하려는 것이 타입이라면 마커 인터페이스를 사용하라
- keywords:
  - marker interface
  - marker annotation
- mapping_hint:
  표시용 인터페이스·애너테이션으로 타입을 구분할 때 참고한다.

### EJ-42

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 42
- title: 익명 클래스보다는 람다를 사용하라
- keywords:
  - lambda
  - anonymous class
  - object :
- mapping_hint:
  익명 클래스(object :)를 람다로 바꾸거나 새로 람다를 쓸 때 참고한다.

### EJ-43

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 43
- title: 람다보다는 메서드 참조를 사용하라
- keywords:
  - method reference
  - ::
- mapping_hint:
  람다 대신 메서드 참조(::)를 쓰는 코드가 추가될 때 참고한다.

### EJ-44

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 44
- title: 표준 함수형 인터페이스를 사용하라
- keywords:
  - functional interface
  - Function
  - Supplier
  - Consumer
  - Predicate
  - fun interface
- mapping_hint:
  표준 함수형 인터페이스나 직접 만든 함수형 인터페이스(fun interface)를 정의·사용할 때 참고한다.

### EJ-45

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 45
- title: 스트림은 주의해서 사용하라
- keywords:
  - stream
  - collection chain
  - readability
- mapping_hint:
  스트림·컬렉션 함수 체인이 길어져 가독성·디버깅이 쟁점일 때 참고한다.

### EJ-46

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 46
- title: 스트림에서는 부작용 없는 함수를 사용하라
- keywords:
  - side effect
  - forEach
  - collect
  - pure function
- mapping_hint:
  스트림·컬렉션 연산 안에서 외부 상태를 바꾸는 부작용이 생길 때 참고한다.

### EJ-47

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 47
- title: 반환 타입으로는 스트림보다 컬렉션이 낫다
- keywords:
  - return type
  - Stream
  - Collection
  - Sequence
- mapping_hint:
  메서드가 스트림·Sequence와 컬렉션 중 무엇을 반환할지 바뀔 때 참고한다.

### EJ-48

- source: Effective Java
- chapter: 7장 람다와 스트림
- item: 48
- title: 스트림 병렬화는 주의해서 적용하라
- keywords:
  - parallel stream
  - parallel
  - thread
- mapping_hint:
  병렬 스트림·병렬 처리를 도입할 때 참고한다.

### EJ-49

- source: Effective Java
- chapter: 8장 메서드
- item: 49
- title: 매개변수가 유효한지 검사하라
- keywords:
  - validation
  - require
  - check
  - IllegalArgumentException
- mapping_hint:
  메서드 입력값 검증(require, 예외 던지기)이 추가·변경될 때 참고한다.

### EJ-50

- source: Effective Java
- chapter: 8장 메서드
- item: 50
- title: 적시에 방어적 복사본을 만들라
- keywords:
  - defensive copy
  - mutable parameter
  - toList
- mapping_hint:
  외부에서 받은 가변 객체·컬렉션을 그대로 보관하거나 그대로 내보낼 때 참고한다.

### EJ-51

- source: Effective Java
- chapter: 8장 메서드
- item: 51
- title: 메서드 시그니처를 신중히 설계하라
- keywords:
  - method signature
  - parameter type
  - boolean parameter
- mapping_hint:
  메서드 이름·매개변수 개수·타입(불리언 인자 등) 설계가 바뀔 때 참고한다.

### EJ-52

- source: Effective Java
- chapter: 8장 메서드
- item: 52
- title: 다중정의는 신중히 사용하라
- keywords:
  - overloading
  - overload resolution
- mapping_hint:
  같은 이름의 메서드를 다중정의해 호출 대상이 헷갈릴 수 있을 때 참고한다.

### EJ-53

- source: Effective Java
- chapter: 8장 메서드
- item: 53
- title: 가변인수는 신중히 사용하라
- keywords:
  - varargs
  - vararg
- mapping_hint:
  가변인수(vararg) 함수가 추가될 때 참고한다.

### EJ-54

- source: Effective Java
- chapter: 8장 메서드
- item: 54
- title: null이 아닌, 빈 컬렉션이나 배열을 반환하라
- keywords:
  - empty collection
  - null return
  - emptyList
- mapping_hint:
  컬렉션·배열을 반환하는 메서드가 null을 돌려줄 수 있을 때 참고한다.

### EJ-55

- source: Effective Java
- chapter: 8장 메서드
- item: 55
- title: 옵셔널 반환은 신중히 하라
- keywords:
  - Optional
  - nullable return
  - ?
- mapping_hint:
  반환 타입을 Optional·nullable로 바꾸거나 새로 쓸 때 참고한다.

### EJ-56

- source: Effective Java
- chapter: 8장 메서드
- item: 56
- title: 공개된 API 요소에는 항상 문서화 주석을 작성하라
- keywords:
  - Javadoc
  - KDoc
  - API documentation
- mapping_hint:
  공개 API에 문서화 주석이 추가·변경될 때 참고한다.

### EJ-57

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 57
- title: 지역변수의 범위를 최소화하라
- keywords:
  - local variable scope
  - var
- mapping_hint:
  지역 변수의 선언 위치·범위가 넓어지는 코드가 생길 때 참고한다.

### EJ-58

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 58
- title: 전통적인 for 문보다는 for-each 문을 사용하라
- keywords:
  - for-each
  - for loop
  - index
- mapping_hint:
  인덱스 기반 반복을 for-each로 바꾸거나 반대로 바꿀 때 참고한다.

### EJ-59

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 59
- title: 라이브러리를 익히고 사용하라
- keywords:
  - standard library
  - library
- mapping_hint:
  표준 라이브러리로 해결되는 기능을 직접 구현한 코드가 생길 때 참고한다.

### EJ-60

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 60
- title: 정확한 답이 필요하다면 float와 double은 피하라
- keywords:
  - float
  - double
  - money
  - BigDecimal
  - Long
  - rounding
- mapping_hint:
  금액·정산처럼 정확한 값이 필요한 계산에 float/double·반올림이 쓰일 때 참고한다.

### EJ-61

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 61
- title: 박싱된 기본 타입보다는 기본 타입을 사용하라
- keywords:
  - boxing
  - Integer
  - Long
  - nullable primitive
- mapping_hint:
  박싱된 타입(Long?, Integer)과 기본 타입 비교·연산이 섞일 때 참고한다.

### EJ-62

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 62
- title: 다른 타입이 적절하다면 문자열 사용을 피하라
- keywords:
  - stringly typed
  - String key
  - enum
- mapping_hint:
  문자열로 상태·키·타입을 표현하는 코드가 추가될 때 참고한다.

### EJ-63

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 63
- title: 문자열 연결은 느리니 주의하라
- keywords:
  - string concatenation
  - StringBuilder
  - buildString
- mapping_hint:
  반복문 안에서 문자열을 이어 붙이는 코드가 생길 때 참고한다.

### EJ-64

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 64
- title: 객체는 인터페이스를 사용해 참조하라
- keywords:
  - interface reference
  - List
  - Map
  - declared type
- mapping_hint:
  변수·필드·매개변수 타입을 구현 클래스 대신 인터페이스로 선언하는지가 바뀔 때 참고한다.

### EJ-65

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 65
- title: 리플렉션보다는 인터페이스를 사용하라
- keywords:
  - reflection
  - interface
- mapping_hint:
  리플렉션으로 객체를 다루는 코드가 추가될 때 참고한다.

### EJ-66

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 66
- title: 네이티브 메서드는 신중히 사용하라
- keywords:
  - native method
  - JNI
- mapping_hint:
  네이티브 메서드 호출이 추가될 때 참고한다.

### EJ-67

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 67
- title: 최적화는 신중히 하라
- keywords:
  - optimization
  - performance
  - premature optimization
- mapping_hint:
  성능을 이유로 구조를 복잡하게 바꾸는 최적화가 들어갈 때 참고한다.

### EJ-68

- source: Effective Java
- chapter: 9장 일반적인 프로그래밍 원칙
- item: 68
- title: 일반적으로 통용되는 명명 규칙을 따르라
- keywords:
  - naming convention
  - naming
- mapping_hint:
  클래스·메서드·패키지 이름 규칙이 어긋나거나 새로 정해질 때 참고한다.

### EJ-69

- source: Effective Java
- chapter: 10장 예외
- item: 69
- title: 예외는 진짜 예외 상황에만 사용하라
- keywords:
  - exception for control flow
  - try
  - catch
- mapping_hint:
  예외를 일반 흐름 제어에 쓰는 코드가 생길 때 참고한다.

### EJ-70

- source: Effective Java
- chapter: 10장 예외
- item: 70
- title: 복구할 수 있는 상황에는 검사 예외를, 프로그래밍 오류에는 런타임 예외를 사용하라
- keywords:
  - checked exception
  - runtime exception
  - error type
- mapping_hint:
  복구 가능한 상황과 프로그래밍 오류를 어떤 예외로 구분할지 바뀔 때 참고한다.

### EJ-71

- source: Effective Java
- chapter: 10장 예외
- item: 71
- title: 필요 없는 검사 예외 사용은 피하라
- keywords:
  - checked exception
  - throws
- mapping_hint:
  호출자에게 처리를 강제하는 검사 예외가 추가될 때 참고한다.

### EJ-72

- source: Effective Java
- chapter: 10장 예외
- item: 72
- title: 표준 예외를 사용하라
- keywords:
  - standard exception
  - IllegalArgumentException
  - IllegalStateException
- mapping_hint:
  커스텀 예외 대신 표준 예외를 쓸지, 새 예외 타입을 만들지 바뀔 때 참고한다.

### EJ-73

- source: Effective Java
- chapter: 10장 예외
- item: 73
- title: 추상화 수준에 맞는 예외를 던지라
- keywords:
  - exception translation
  - abstraction level
  - ApiException
- mapping_hint:
  하위 계층 예외(DB·외부 API)를 상위 계층 예외(API 오류)로 바꿔 던질 때 참고한다.

### EJ-74

- source: Effective Java
- chapter: 10장 예외
- item: 74
- title: 메서드가 던지는 모든 예외를 문서화하라
- keywords:
  - exception documentation
  - @throws
- mapping_hint:
  메서드가 던지는 예외 목록·문서가 바뀔 때 참고한다.

### EJ-75

- source: Effective Java
- chapter: 10장 예외
- item: 75
- title: 예외의 상세 메시지에 실패 관련 정보를 담으라
- keywords:
  - exception message
  - error message
  - failure info
- mapping_hint:
  예외·오류 응답 메시지에 담을 실패 정보가 바뀔 때 참고한다.

### EJ-76

- source: Effective Java
- chapter: 10장 예외
- item: 76
- title: 가능한 한 실패 원자적으로 만들라
- keywords:
  - failure atomicity
  - transaction
  - rollback
  - state change
- mapping_hint:
  실패했을 때 객체·DB 상태가 중간에 반쯤 바뀐 채 남을 수 있는 코드(트랜잭션 경계 포함)가 바뀔 때 참고한다.

### EJ-77

- source: Effective Java
- chapter: 10장 예외
- item: 77
- title: 예외를 무시하지 말라
- keywords:
  - ignored exception
  - empty catch
  - runCatching
- mapping_hint:
  예외를 잡고 아무것도 하지 않는(빈 catch, 결과 무시) 코드가 생길 때 참고한다.

### EJ-78

- source: Effective Java
- chapter: 11장 동시성
- item: 78
- title: 공유 중인 가변 데이터는 동기화해 사용하라
- keywords:
  - synchronization
  - shared mutable state
  - volatile
  - concurrency
- mapping_hint:
  여러 스레드·요청이 공유하는 가변 데이터에 접근하는 코드가 생길 때 참고한다.

### EJ-79

- source: Effective Java
- chapter: 11장 동시성
- item: 79
- title: 과도한 동기화는 피하라
- keywords:
  - excessive synchronization
  - lock
  - deadlock
- mapping_hint:
  동기화 블록·락 범위가 넓어지거나 락 안에서 외부 코드를 호출할 때 참고한다.

### EJ-80

- source: Effective Java
- chapter: 11장 동시성
- item: 80
- title: 스레드보다는 실행자, 태스크, 스트림을 애용하라
- keywords:
  - executor
  - thread pool
  - task
  - @Async
- mapping_hint:
  스레드를 직접 만들지 않고 실행자·스레드 풀·태스크로 작업을 실행할 때 참고한다.

### EJ-81

- source: Effective Java
- chapter: 11장 동시성
- item: 81
- title: wait와 notify보다는 동시성 유틸리티를 애용하라
- keywords:
  - wait
  - notify
  - concurrent utilities
  - CountDownLatch
  - ConcurrentHashMap
- mapping_hint:
  wait/notify 대신 동시성 유틸리티(동시성 컬렉션, 래치)를 쓰는 코드가 바뀔 때 참고한다.

### EJ-82

- source: Effective Java
- chapter: 11장 동시성
- item: 82
- title: 스레드 안전성 수준을 문서화하라
- keywords:
  - thread safety
  - documentation
  - @ThreadSafe
- mapping_hint:
  클래스의 스레드 안전성 수준이 바뀌거나 동시 사용을 전제로 할 때 참고한다.

### EJ-83

- source: Effective Java
- chapter: 11장 동시성
- item: 83
- title: 지연 초기화는 신중히 사용하라
- keywords:
  - lazy initialization
  - lazy
  - double-checked locking
- mapping_hint:
  지연 초기화(by lazy 등)를 도입해 초기화 시점·스레드 안전성이 바뀔 때 참고한다.

### EJ-84

- source: Effective Java
- chapter: 11장 동시성
- item: 84
- title: 프로그램의 동작을 스레드 스케줄러에 기대지 말라
- keywords:
  - thread scheduler
  - Thread.sleep
  - yield
  - busy wait
- mapping_hint:
  sleep·스레드 우선순위·바쁜 대기에 동작을 기대는 코드가 생길 때 참고한다.

### EJ-85

- source: Effective Java
- chapter: 12장 직렬화
- item: 85
- title: 자바 직렬화의 대안을 찾으라
- keywords:
  - serialization
  - Serializable
  - JSON
- mapping_hint:
  자바 직렬화 대신 JSON 등 다른 직렬화 방식을 쓸지 정할 때 참고한다.

### EJ-86

- source: Effective Java
- chapter: 12장 직렬화
- item: 86
- title: Serializable을 구현할지는 신중히 결정하라
- keywords:
  - Serializable
  - serialVersionUID
- mapping_hint:
  클래스에 Serializable을 구현할 때 참고한다.

### EJ-87

- source: Effective Java
- chapter: 12장 직렬화
- item: 87
- title: 커스텀 직렬화 형태를 고려해보라
- keywords:
  - custom serialized form
  - serialization format
- mapping_hint:
  직렬화 형태(필드·포맷)를 직접 정의할 때 참고한다.

### EJ-88

- source: Effective Java
- chapter: 12장 직렬화
- item: 88
- title: readObject 메서드는 방어적으로 작성하라
- keywords:
  - readObject
  - deserialization
  - validation
- mapping_hint:
  역직렬화 시 입력을 검증해야 하는 코드가 생길 때 참고한다.

### EJ-89

- source: Effective Java
- chapter: 12장 직렬화
- item: 89
- title: 인스턴스 수를 통제해야 한다면 readResolve보다는 열거 타입을 사용하라
- keywords:
  - readResolve
  - singleton serialization
  - enum
- mapping_hint:
  직렬화와 싱글턴·인스턴스 통제가 함께 쓰일 때 참고한다.

### EJ-90

- source: Effective Java
- chapter: 12장 직렬화
- item: 90
- title: 직렬화된 인스턴스 대신 직렬화 프록시 사용을 검토하라
- keywords:
  - serialization proxy
- mapping_hint:
  직렬화 프록시 패턴을 쓰는 코드가 생길 때 참고한다.

---

## 5. 함수형 프로그래밍 with 자바

출처: 사용자가 제공한 목차(2026-09-29). 장·절 번호와 제목은 원문 그대로이며 '핵심 요약'은 항목에서 뺐다. 결과에는 `Chapter {장} / Section {절}, {title}`로 표시한다.

### FJ-01-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 1
- chapter_title: 함수형 프로그래밍 소개
- section: 1.1
- title: 어떤 것이 언어를 ‘함수형’으로 만드는가?
- keywords:
  - functional language
  - first-class function
- mapping_hint:
  함수를 값처럼 다루는 코드가 처음 들어와 함수형의 정의부터 짚어야 할 때 참고한다.

### FJ-01-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 1
- chapter_title: 함수형 프로그래밍 소개
- section: 1.2
- title: 함수형 프로그래밍의 개념
- keywords:
  - pure function
  - immutability
  - referential transparency
- mapping_hint:
  순수 함수·불변성·참조 투명성이 설계 판단에 쓰일 때 참고한다.

### FJ-01-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 1
- chapter_title: 함수형 프로그래밍 소개
- section: 1.3
- title: 함수형 프로그래밍의 장점
- keywords:
  - functional benefits
  - testability
  - concurrency
- mapping_hint:
  함수형으로 바꾼 이유(테스트·동시성 이점)를 따져야 할 때 참고한다.

### FJ-01-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 1
- chapter_title: 함수형 프로그래밍 소개
- section: 1.4
- title: 함수형 프로그래밍의 단점
- keywords:
  - functional drawbacks
  - performance
  - learning curve
- mapping_hint:
  함수형 스타일의 비용(성능·가독성)이 쟁점일 때 참고한다.

### FJ-02-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 2
- chapter_title: 함수형 자바
- section: 2.1
- title: 자바 람다란?
- keywords:
  - lambda
  - lambda syntax
- mapping_hint:
  람다 문법이 새로 쓰일 때 참고한다.

### FJ-02-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 2
- chapter_title: 함수형 자바
- section: 2.2
- title: 람다의 실전 활용
- keywords:
  - lambda usage
  - method reference
  - closure
- mapping_hint:
  람다·메서드 참조를 실제 로직(정렬·필터·콜백)에 적용할 때 참고한다.

### FJ-02-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 2
- chapter_title: 함수형 자바
- section: 2.3
- title: 자바의 함수형 프로그래밍 개념
- keywords:
  - function composition
  - higher-order function
  - pure function
- mapping_hint:
  자바(JVM)에서 함수형 개념(고차 함수·합성)을 쓰는 코드가 추가될 때 참고한다.

### FJ-03-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 3
- chapter_title: JDK의 함수형 인터페이스
- section: 3.1
- title: 네 가지 함수형 인터페이스
- keywords:
  - Function
  - Supplier
  - Consumer
  - Predicate
- mapping_hint:
  Function/Supplier/Consumer/Predicate(코틀린 함수 타입)가 매개변수·필드로 쓰일 때 참고한다.

### FJ-03-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 3
- chapter_title: JDK의 함수형 인터페이스
- section: 3.2
- title: 함수형 인터페이스 변형이 많은 이유
- keywords:
  - primitive functional interface
  - IntFunction
  - BiFunction
- mapping_hint:
  기본형·인자 개수별 함수형 인터페이스 변형을 고를 때 참고한다.

### FJ-03-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 3
- chapter_title: JDK의 함수형 인터페이스
- section: 3.3
- title: 함수 합성
- keywords:
  - compose
  - andThen
  - function composition
- mapping_hint:
  함수를 이어 붙여(andThen/compose) 처리 흐름을 만들 때 참고한다.

### FJ-03-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅰ 함수형 기초
- chapter: 3
- chapter_title: JDK의 함수형 인터페이스
- section: 3.4
- title: 함수형 지원 확장
- keywords:
  - functional support
  - default method
  - custom functional interface
- mapping_hint:
  함수형 인터페이스를 직접 정의하거나 확장할 때 참고한다.

### FJ-04-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 4
- chapter_title: 불변성
- section: 4.1
- title: 객체 지향 프로그래밍의 가변성과 자료 구조
- keywords:
  - mutability
  - data structure
  - OOP state
- mapping_hint:
  가변 필드·가변 컬렉션으로 상태를 바꾸는 객체 지향 코드가 쟁점일 때 참고한다.

### FJ-04-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 4
- chapter_title: 불변성
- section: 4.2
- title: 함수형 프로그래밍의 불변성
- keywords:
  - immutability
  - val
  - read-only
- mapping_hint:
  불변 값으로 상태를 다루도록 바꿀 때 참고한다.

### FJ-04-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 4
- chapter_title: 불변성
- section: 4.3
- title: 자바 불변성 상태
- keywords:
  - unmodifiable collection
  - final
  - immutable Java
- mapping_hint:
  읽기 전용·수정 불가 컬렉션과 진짜 불변의 차이가 문제 될 때 참고한다.

### FJ-04-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 4
- chapter_title: 불변성
- section: 4.4
- title: 불변성 만들기
- keywords:
  - defensive copy
  - copy
  - immutable object
- mapping_hint:
  불변 객체를 만들거나 copy로 새 값을 만드는 코드가 추가될 때 참고한다.

### FJ-05-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 5
- chapter_title: 레코드
- section: 5.1
- title: 데이터 집계 유형
- keywords:
  - data aggregate
  - tuple
  - DTO
- mapping_hint:
  값을 묶어 전달하는 데이터 집계 타입(DTO 등)을 만들 때 참고한다.

### FJ-05-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 5
- chapter_title: 레코드
- section: 5.2
- title: 도움을 주기 위한 레코드
- keywords:
  - record
  - data class
- mapping_hint:
  record(코틀린 data class)로 데이터 타입을 정의할 때 참고한다.

### FJ-05-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 5
- chapter_title: 레코드
- section: 5.3
- title: 사용 사례와 일반적인 관행
- keywords:
  - record usage
  - validation
  - compact constructor
- mapping_hint:
  record·data class에 검증·파생 값·관행을 적용할 때 참고한다.

### FJ-05-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 5
- chapter_title: 레코드
- section: 5.4
- title: 레코드를 마무리하며
- keywords:
  - record limitations
- mapping_hint:
  record·data class로 표현하기 어려운 경우(상속·가변 상태)를 판단할 때 참고한다.

### FJ-06-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 6
- chapter_title: 스트림을 이용한 데이터 처리
- section: 6.1
- title: 반복을 통한 데이터 처리
- keywords:
  - loop
  - iteration
  - for
- mapping_hint:
  반복문으로 데이터를 처리하던 코드를 바꾸거나 새로 쓸 때 참고한다.

### FJ-06-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 6
- chapter_title: 스트림을 이용한 데이터 처리
- section: 6.2
- title: 함수형 데이터 파이프라인으로써의 스트림
- keywords:
  - stream
  - pipeline
  - lazy
- mapping_hint:
  스트림·컬렉션 체인을 데이터 파이프라인으로 구성할 때 참고한다.

### FJ-06-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 6
- chapter_title: 스트림을 이용한 데이터 처리
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
  Stream 파이프라인이 추가되거나 중간·최종 연산의 연결 구조가 변경될 때 참고한다.

### FJ-06-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 6
- chapter_title: 스트림을 이용한 데이터 처리
- section: 6.4
- title: 스트림 사용 여부 선택
- keywords:
  - stream vs loop
  - readability
- mapping_hint:
  스트림과 반복문 중 무엇을 쓸지가 쟁점일 때 참고한다.

### FJ-07-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.1
- title: 원시 스트림
- keywords:
  - IntStream
  - LongStream
  - primitive stream
  - sumOf
- mapping_hint:
  기본형 스트림·합계 연산(sum, sumOf)이 쓰일 때 참고한다.

### FJ-07-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.2
- title: 반복 스트림
- keywords:
  - iterate
  - generate
  - iterative stream
- mapping_hint:
  iterate로 반복 스트림을 만들 때 참고한다.

### FJ-07-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.3
- title: 무한 스트림
- keywords:
  - infinite stream
  - limit
  - takeWhile
  - generateSequence
- mapping_hint:
  무한 스트림·generateSequence와 종료 조건(limit, takeWhile)이 쓰일 때 참고한다.

### FJ-07-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.4
- title: 배열에서 스트림으로, 그리고 다시 배열로
- keywords:
  - Arrays.stream
  - toArray
- mapping_hint:
  배열과 스트림을 오가는 코드가 추가될 때 참고한다.

### FJ-07-05

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.5
- title: 저수준 스트림 생성
- keywords:
  - Spliterator
  - StreamSupport
  - low-level stream
- mapping_hint:
  Spliterator 등 저수준으로 스트림을 만드는 코드가 생길 때 참고한다.

### FJ-07-06

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.6
- title: 파일 I/O 사용하기
- keywords:
  - Files.lines
  - file I/O
  - stream close
- mapping_hint:
  파일을 스트림으로 읽거나 쓰며 자원 닫기가 쟁점일 때 참고한다.

### FJ-07-07

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.7
- title: 날짜와 시간 처리하기
- keywords:
  - date time
  - LocalDate
  - Instant
  - datesUntil
- mapping_hint:
  날짜·시간을 스트림으로 다루는 코드가 추가될 때 참고한다.

### FJ-07-08

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.8
- title: JMH를 활용하여 스트림 성능 측정하기
- keywords:
  - JMH
  - benchmark
  - stream performance
- mapping_hint:
  스트림 성능을 측정하거나 성능 비교가 근거로 쓰일 때 참고한다.

### FJ-07-09

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.9
- title: 컬렉터 알아보기
- keywords:
  - Collectors
  - groupingBy
  - toMap
  - partitioningBy
  - associate
  - groupBy
- mapping_hint:
  groupingBy/toMap 같은 컬렉터(코틀린 groupBy/associate)로 결과를 모을 때 참고한다.

### FJ-07-10

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 7
- chapter_title: 스트림 사용하기
- section: 7.10
- title: (순차적인) 스트림에 대한 고찰
- keywords:
  - sequential stream
  - ordering
- mapping_hint:
  순차 스트림의 순서·동작을 전제로 하는 코드가 바뀔 때 참고한다.

### FJ-08-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 8
- chapter_title: 스트림을 활용한 병렬 데이터 처리
- section: 8.1
- title: 동시성 vs 병렬성
- keywords:
  - concurrency
  - parallelism
- mapping_hint:
  동시성과 병렬성을 구분해야 하는 처리 코드가 추가될 때 참고한다.

### FJ-08-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 8
- chapter_title: 스트림을 활용한 병렬 데이터 처리
- section: 8.2
- title: 병렬 함수 파이프라인으로써의 스트림
- keywords:
  - parallel pipeline
  - parallel stream
- mapping_hint:
  병렬 파이프라인으로 스트림을 처리할 때 참고한다.

### FJ-08-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 8
- chapter_title: 스트림을 활용한 병렬 데이터 처리
- section: 8.3
- title: 병렬 스트림 활용
- keywords:
  - parallel
  - ForkJoinPool
  - parallelStream
- mapping_hint:
  parallelStream 등 병렬 스트림을 실제로 쓸 때 참고한다.

### FJ-08-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 8
- chapter_title: 스트림을 활용한 병렬 데이터 처리
- section: 8.4
- title: 병렬 스트림 활용 시기와 주의할 점
- keywords:
  - parallel pitfalls
  - shared state
  - ordering
- mapping_hint:
  병렬 처리에서 공유 상태·순서·풀 고갈 위험이 있을 때 참고한다.

### FJ-09-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.1
- title: null 참조의 문제점
- keywords:
  - null
  - NullPointerException
- mapping_hint:
  null 참조로 인한 오류 가능성이 있는 코드가 추가될 때 참고한다.

### FJ-09-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.2
- title: 자바에서 null을 다루는 방법 (Optional 도입 전)
- keywords:
  - null check
  - default value
- mapping_hint:
  null 검사·기본값 처리 방식이 바뀔 때 참고한다.

### FJ-09-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.3
- title: Optional 알아보기
- keywords:
  - Optional
  - map
  - orElse
  - ifPresent
- mapping_hint:
  Optional(코틀린 nullable + ?.let/?:)로 값 유무를 다룰 때 참고한다.

### FJ-09-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.4
- title: Optional과 스트림
- keywords:
  - Optional stream
  - flatMap
  - mapNotNull
- mapping_hint:
  Optional·nullable 값을 스트림·컬렉션 체인 안에서 걸러 낼 때 참고한다.

### FJ-09-05

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.5
- title: 원시 타입용 Optional
- keywords:
  - OptionalInt
  - primitive Optional
- mapping_hint:
  기본형 Optional을 쓸 때 참고한다.

### FJ-09-06

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.6
- title: 주의 사항
- keywords:
  - Optional pitfalls
  - Optional field
  - get
- mapping_hint:
  Optional을 필드·매개변수로 쓰거나 get()을 바로 부르는 코드가 생길 때 참고한다.

### FJ-09-07

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 9
- chapter_title: Optional을 사용한 null 처리
- section: 9.7
- title: null 참조에 대한 생각
- keywords:
  - null design
  - nullable type
- mapping_hint:
  null을 허용할지 말지 타입 설계가 바뀔 때 참고한다.

### FJ-10-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 10
- chapter_title: 함수형 예외 처리
- section: 10.1
- title: 자바 예외 처리
- keywords:
  - exception
  - checked
  - unchecked
- mapping_hint:
  예외 처리 구조가 바뀌어 자바 예외 체계를 다시 짚어야 할 때 참고한다.

### FJ-10-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 10
- chapter_title: 함수형 예외 처리
- section: 10.2
- title: try-catch 블록
- keywords:
  - try-catch
  - finally
- mapping_hint:
  try-catch 블록이 추가·변경될 때 참고한다.

### FJ-10-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 10
- chapter_title: 함수형 예외 처리
- section: 10.3
- title: 람다에서의 체크 예외
- keywords:
  - checked exception in lambda
  - wrapping exception
- mapping_hint:
  람다 안에서 검사 예외를 처리하거나 감싸 던질 때 참고한다.

### FJ-10-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 10
- chapter_title: 함수형 예외 처리
- section: 10.4
- title: 함수형으로 예외 다루기
- keywords:
  - Either
  - Result
  - runCatching
  - Try
- mapping_hint:
  Result/runCatching 등 예외를 값으로 다루는 코드가 추가될 때 참고한다.

### FJ-10-05

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 10
- chapter_title: 함수형 예외 처리
- section: 10.5
- title: 함수형 예외 처리에 대한 고찰
- keywords:
  - functional error handling
  - trade-off
- mapping_hint:
  함수형 예외 처리의 장단점이 설계 판단에 쓰일 때 참고한다.

### FJ-11-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 11
- chapter_title: 느긋한 계산법 (지연 평가)
- section: 11.1
- title: 느긋함 vs 엄격함
- keywords:
  - lazy
  - strict
  - evaluation
- mapping_hint:
  지연 평가와 즉시 평가의 차이가 동작에 영향을 줄 때 참고한다.

### FJ-11-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 11
- chapter_title: 느긋한 계산법 (지연 평가)
- section: 11.2
- title: 자바는 얼마나 엄격한가?
- keywords:
  - strict evaluation
  - short-circuit
- mapping_hint:
  자바(JVM)의 즉시 평가·단락 평가에 기대는 코드가 바뀔 때 참고한다.

### FJ-11-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 11
- chapter_title: 느긋한 계산법 (지연 평가)
- section: 11.3
- title: 람다와 고차 함수
- keywords:
  - higher-order function
  - lambda
  - deferred
- mapping_hint:
  람다로 계산을 미루는 고차 함수가 추가될 때 참고한다.

### FJ-11-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 11
- chapter_title: 느긋한 계산법 (지연 평가)
- section: 11.4
- title: 썽크를 사용한 지연 실행
- keywords:
  - thunk
  - Supplier
  - lazy
  - memoization
- mapping_hint:
  Supplier·lazy로 계산을 지연·캐시하는 코드가 생길 때 참고한다.

### FJ-11-05

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 11
- chapter_title: 느긋한 계산법 (지연 평가)
- section: 11.5
- title: 느긋함에 대한 고찰
- keywords:
  - laziness trade-off
- mapping_hint:
  지연 평가 도입의 득실을 따져야 할 때 참고한다.

### FJ-12-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 12
- chapter_title: 재귀
- section: 12.1
- title: 재귀란 무엇인가?
- keywords:
  - recursion
  - base case
- mapping_hint:
  재귀 함수가 추가될 때 참고한다.

### FJ-12-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 12
- chapter_title: 재귀
- section: 12.2
- title: 더 복잡한 예시
- keywords:
  - recursion example
  - tree
  - stack overflow
- mapping_hint:
  트리·중첩 구조를 재귀로 처리하거나 스택 깊이가 쟁점일 때 참고한다.

### FJ-12-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 12
- chapter_title: 재귀
- section: 12.3
- title: 재귀와 유사한 스트림
- keywords:
  - recursive stream
  - iterate
- mapping_hint:
  재귀 대신 스트림으로 반복 구조를 표현할 때 참고한다.

### FJ-12-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 12
- chapter_title: 재귀
- section: 12.4
- title: 재귀에 대한 고찰
- keywords:
  - recursion trade-off
  - tailrec
- mapping_hint:
  재귀를 쓸지(꼬리 재귀 포함) 판단해야 할 때 참고한다.

### FJ-13-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.1
- title: 동기 vs 비동기
- keywords:
  - synchronous
  - asynchronous
  - blocking
- mapping_hint:
  동기 호출을 비동기로 바꾸거나 블로킹 여부가 쟁점일 때 참고한다.

### FJ-13-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.2
- title: 자바의 Future
- keywords:
  - Future
  - get
  - blocking
- mapping_hint:
  Future와 블로킹 get()이 쓰일 때 참고한다.

### FJ-13-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.3
- title: CompletableFutures로 비동기 파이프라인 구축
- keywords:
  - CompletableFuture
  - thenApply
  - thenCompose
  - async pipeline
- mapping_hint:
  CompletableFuture로 비동기 작업을 이어 붙일 때 참고한다.

### FJ-13-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.4
- title: 수동 생성 및 수동 완료
- keywords:
  - complete
  - manual completion
- mapping_hint:
  CompletableFuture를 직접 만들고 완료시키는 코드가 생길 때 참고한다.

### FJ-13-05

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.5
- title: 스레드 풀과 타임 아웃의 중요성
- keywords:
  - thread pool
  - timeout
  - executor
- mapping_hint:
  비동기 작업의 스레드 풀·타임아웃 설정이 바뀔 때 참고한다.

### FJ-13-06

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 13
- chapter_title: 비동기 작업
- section: 13.6
- title: 비동기 작업에 대한 고찰
- keywords:
  - async trade-off
- mapping_hint:
  비동기 처리의 복잡도·오류 처리 비용을 따져야 할 때 참고한다.

### FJ-14-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 14
- chapter_title: 비함수형 디자인 패턴
- section: 14.1
- title: 디자인 패턴이란?
- keywords:
  - design pattern
- mapping_hint:
  디자인 패턴을 적용한 구조가 추가될 때 참고한다.

### FJ-14-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 14
- chapter_title: 비함수형 디자인 패턴
- section: 14.2
- title: (함수형) 디자인 패턴
- keywords:
  - strategy
  - factory
  - builder
  - decorator
  - functional pattern
- mapping_hint:
  전략·팩토리·빌더·데코레이터를 함수(람다)로 구현하는 코드가 생길 때 참고한다.

### FJ-14-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 14
- chapter_title: 비함수형 디자인 패턴
- section: 14.3
- title: 함수형 디자인 패턴에 대한 고찰
- keywords:
  - functional pattern trade-off
- mapping_hint:
  패턴을 함수형으로 바꾼 득실을 판단할 때 참고한다.

### FJ-15-01

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 15
- chapter_title: 자바를 위한 함수형 접근 방식
- section: 15.1
- title: 객체 지향 프로그래밍과 함수형 프로그래밍 원칙 비교
- keywords:
  - OOP vs FP
  - principles
- mapping_hint:
  객체 지향과 함수형 원칙이 한 설계 안에서 부딪힐 때 참고한다.

### FJ-15-02

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 15
- chapter_title: 자바를 위한 함수형 접근 방식
- section: 15.2
- title: 함수형 사고방식
- keywords:
  - functional thinking
  - pure core
- mapping_hint:
  부작용을 경계로 밀어내는 사고방식이 설계에 쓰일 때 참고한다.

### FJ-15-03

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 15
- chapter_title: 자바를 위한 함수형 접근 방식
- section: 15.3
- title: 명령형 세계의 함수형 아키텍처
- keywords:
  - functional core imperative shell
  - architecture
- mapping_hint:
  계산(순수)과 입출력(부작용)을 나누는 구조로 코드를 바꿀 때 — 예: core 계산 엔진과 server 분리 참고한다.

### FJ-15-04

- source: 함수형 프로그래밍 with 자바
- part: PART Ⅱ 함수형 접근 방식
- chapter: 15
- chapter_title: 자바를 위한 함수형 접근 방식
- section: 15.4
- title: 자바에서 함수형 접근에 대한 고찰
- keywords:
  - functional approach in Java
- mapping_hint:
  자바(JVM)에서 함수형 접근을 어디까지 쓸지 판단할 때 참고한다.

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

---

## 8. 자바의 정석

출처: 사용자가 제공한 선별 목차(2026-09-29). 1~5장(변수·연산자·조건문·배열 등 기초)은 사용자가 제외했다. 장·절·소절 번호, 제목, 페이지는 원문 그대로이며 형식만 정리했다(`6 변수의 초기화` → 6절, `Future903` → `Future` p.903, `Optional〈T〉` → `Optional<T>`). 결과에는 `자바의 정석 {장}장 {절}.{소절} {title} (p.{page})`로 표시한다.

### JST-06-01-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 1. 객체지향언어
- subsection: 1.1
- title: 객체지향 언어의 역사
- page: 254
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '객체지향 언어의 역사'(p.254)를 참고한다.

### JST-06-01-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 1. 객체지향언어
- subsection: 1.2
- title: 객체지향언어
- page: 254
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '객체지향언어'(p.254)를 참고한다.

### JST-06-02-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.1
- title: 클래스와 객체의 정의와 용도
- page: 255
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '클래스와 객체의 정의와 용도'(p.255)를 참고한다.

### JST-06-02-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.2
- title: 객체와 인스턴스
- page: 256
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '객체와 인스턴스'(p.256)를 참고한다.

### JST-06-02-03

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.3
- title: 객체의 구성요소 - 속성과 기능
- page: 257
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '객체의 구성요소 - 속성과 기능'(p.257)를 참고한다.

### JST-06-02-04

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.4
- title: 인스턴스의 생성과 사용
- page: 258
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '인스턴스의 생성과 사용'(p.258)를 참고한다.

### JST-06-02-05

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.5
- title: 객체 배열
- page: 264
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '객체 배열'(p.264)를 참고한다.

### JST-06-02-06

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 2. 클래스와 객체
- subsection: 2.6
- title: 클래스의 또 다른 정의
- page: 266
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '클래스의 또 다른 정의'(p.266)를 참고한다.

### JST-06-03-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.1
- title: 선언위치에 따른 변수의 종류
- page: 270
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '선언위치에 따른 변수의 종류'(p.270)를 참고한다.

### JST-06-03-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.2
- title: 클래스 변수와 인스턴스 변수
- page: 271
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '클래스 변수와 인스턴스 변수'(p.271)를 참고한다.

### JST-06-03-03

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.3
- title: 메서드
- page: 273
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '메서드'(p.273)를 참고한다.

### JST-06-03-04

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.4
- title: 메서드의 선언과 구현
- page: 276
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '메서드의 선언과 구현'(p.276)를 참고한다.

### JST-06-03-05

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.5
- title: 메서드의 호출
- page: 278
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '메서드의 호출'(p.278)를 참고한다.

### JST-06-03-06

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.6
- title: return문
- page: 282
- keywords:
  - return
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 'return문'(p.282)를 참고한다.

### JST-06-03-07

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.7
- title: JVM의 메모리 구조
- page: 285
- keywords:
  - JVM
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 'JVM의 메모리 구조'(p.285)를 참고한다.

### JST-06-03-08

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.8
- title: 기본형 매개변수와 참조형 매개변수
- page: 288
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '기본형 매개변수와 참조형 매개변수'(p.288)를 참고한다.

### JST-06-03-09

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.9
- title: 참조형 반환타입
- page: 292
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '참조형 반환타입'(p.292)를 참고한다.

### JST-06-03-10

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.10
- title: 재귀 호출(recursive call)
- page: 294
- keywords:
  - recursive call
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '재귀 호출(recursive call)'(p.294)를 참고한다.

### JST-06-03-11

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.11
- title: 클래스 메서드와 인스턴스 메서드
- page: 301
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '클래스 메서드와 인스턴스 메서드'(p.301)를 참고한다.

### JST-06-03-12

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 3. 변수와 메서드
- subsection: 3.12
- title: 클래스 멤버와 인스턴스 멤버 간의 참조와 호출
- page: 304
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '클래스 멤버와 인스턴스 멤버 간의 참조와 호출'(p.304)를 참고한다.

### JST-06-04-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 4. 오버로딩(overloading)
- subsection: 4.1
- title: 오버로딩이란?
- page: 307
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '오버로딩이란?'(p.307)를 참고한다.

### JST-06-04-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 4. 오버로딩(overloading)
- subsection: 4.2
- title: 오버로딩의 조건
- page: 307
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '오버로딩의 조건'(p.307)를 참고한다.

### JST-06-04-03

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 4. 오버로딩(overloading)
- subsection: 4.3
- title: 오버로딩의 예
- page: 307
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '오버로딩의 예'(p.307)를 참고한다.

### JST-06-04-04

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 4. 오버로딩(overloading)
- subsection: 4.4
- title: 오버로딩의 장점
- page: 309
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '오버로딩의 장점'(p.309)를 참고한다.

### JST-06-04-05

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 4. 오버로딩(overloading)
- subsection: 4.5
- title: 가변인자(varargs)와 오버로딩
- page: 311
- keywords:
  - varargs
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '가변인자(varargs)와 오버로딩'(p.311)를 참고한다.

### JST-06-05-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 5. 생성자(Constructor)
- subsection: 5.1
- title: 생성자란?
- page: 315
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '생성자란?'(p.315)를 참고한다.

### JST-06-05-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 5. 생성자(Constructor)
- subsection: 5.2
- title: 기본 생성자(default constructor)
- page: 316
- keywords:
  - default constructor
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '기본 생성자(default constructor)'(p.316)를 참고한다.

### JST-06-05-03

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 5. 생성자(Constructor)
- subsection: 5.3
- title: 매개변수가 있는 생성자
- page: 318
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '매개변수가 있는 생성자'(p.318)를 참고한다.

### JST-06-05-04

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 5. 생성자(Constructor)
- subsection: 5.4
- title: 생성자에서 다른 생성자 호출하기 - this(), this
- page: 319
- keywords:
  - this
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '생성자에서 다른 생성자 호출하기 - this(), this'(p.319)를 참고한다.

### JST-06-05-05

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 5. 생성자(Constructor)
- subsection: 5.5
- title: 생성자를 이용한 인스턴스의 복사
- page: 322
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '생성자를 이용한 인스턴스의 복사'(p.322)를 참고한다.

### JST-06-06-01

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 6. 변수의 초기화
- subsection: 6.1
- title: 변수의 초기화
- page: 324
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '변수의 초기화'(p.324)를 참고한다.

### JST-06-06-02

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 6. 변수의 초기화
- subsection: 6.2
- title: 명시적 초기화(explicit initialization)
- page: 325
- keywords:
  - explicit initialization
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '명시적 초기화(explicit initialization)'(p.325)를 참고한다.

### JST-06-06-03

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 6. 변수의 초기화
- subsection: 6.3
- title: 초기화 블럭(initialization block)
- page: 326
- keywords:
  - initialization block
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '초기화 블럭(initialization block)'(p.326)를 참고한다.

### JST-06-06-04

- source: 자바의 정석
- chapter: 6장 객체지향 프로그래밍 I
- section: 6. 변수의 초기화
- subsection: 6.4
- title: 멤버변수의 초기화 시기와 순서
- page: 328
- keywords:
  - class
  - object
  - instance
  - method
  - constructor
  - JVM memory
  - overloading
  - initialization
- mapping_hint:
  클래스·객체·메서드·생성자·초기화 순서처럼 객체의 기본 구조가 diff에서 추가·변경될 때 '멤버변수의 초기화 시기와 순서'(p.328)를 참고한다.

### JST-07-01-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 1. 상속(inheritance)
- subsection: 1.1
- title: 상속의 정의와 장점
- page: 334
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '상속의 정의와 장점'(p.334)를 참고한다.

### JST-07-01-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 1. 상속(inheritance)
- subsection: 1.2
- title: 클래스간의 관계 - 포함 관계
- page: 340
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '클래스간의 관계 - 포함 관계'(p.340)를 참고한다.

### JST-07-01-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 1. 상속(inheritance)
- subsection: 1.3
- title: 클래스간의 관계 결정하기
- page: 341
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '클래스간의 관계 결정하기'(p.341)를 참고한다.

### JST-07-01-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 1. 상속(inheritance)
- subsection: 1.4
- title: 단일 상속(single inheritance)
- page: 347
- keywords:
  - single inheritance
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '단일 상속(single inheritance)'(p.347)를 참고한다.

### JST-07-01-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 1. 상속(inheritance)
- subsection: 1.5
- title: Object클래스 - 모든 클래스의 조상
- page: 349
- keywords:
  - Object
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'Object클래스 - 모든 클래스의 조상'(p.349)를 참고한다.

### JST-07-02-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 2. 오버라이딩(overriding)
- subsection: 2.1
- title: 오버라이딩이란?
- page: 351
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '오버라이딩이란?'(p.351)를 참고한다.

### JST-07-02-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 2. 오버라이딩(overriding)
- subsection: 2.2
- title: 오버라이딩의 조건
- page: 352
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '오버라이딩의 조건'(p.352)를 참고한다.

### JST-07-02-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 2. 오버라이딩(overriding)
- subsection: 2.3
- title: 오버로딩 vs. 오버라이딩
- page: 353
- keywords:
  - vs.
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '오버로딩 vs. 오버라이딩'(p.353)를 참고한다.

### JST-07-02-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 2. 오버라이딩(overriding)
- subsection: 2.4
- title: super
- page: 354
- keywords:
  - super
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'super'(p.354)를 참고한다.

### JST-07-02-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 2. 오버라이딩(overriding)
- subsection: 2.5
- title: super() - 조상 클래스의 생성자
- page: 356
- keywords:
  - super
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'super() - 조상 클래스의 생성자'(p.356)를 참고한다.

### JST-07-03-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 3. package와 import
- subsection: 3.1
- title: 패키지(package)
- page: 360
- keywords:
  - package
  - inheritance
  - override
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '패키지(package)'(p.360)를 참고한다.

### JST-07-03-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 3. package와 import
- subsection: 3.2
- title: 패키지의 선언
- page: 361
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '패키지의 선언'(p.361)를 참고한다.

### JST-07-03-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 3. package와 import
- subsection: 3.3
- title: import문
- page: 364
- keywords:
  - import
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'import문'(p.364)를 참고한다.

### JST-07-03-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 3. package와 import
- subsection: 3.4
- title: import문의 선언
- page: 364
- keywords:
  - import
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'import문의 선언'(p.364)를 참고한다.

### JST-07-03-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 3. package와 import
- subsection: 3.5
- title: static import문
- page: 366
- keywords:
  - static
  - import
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'static import문'(p.366)를 참고한다.

### JST-07-04-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.1
- title: 제어자란?
- page: 368
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '제어자란?'(p.368)를 참고한다.

### JST-07-04-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.2
- title: static - 클래스의, 공통적인
- page: 368
- keywords:
  - static
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'static - 클래스의, 공통적인'(p.368)를 참고한다.

### JST-07-04-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.3
- title: final - 마지막의, 변경될 수 없는
- page: 369
- keywords:
  - final
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'final - 마지막의, 변경될 수 없는'(p.369)를 참고한다.

### JST-07-04-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.4
- title: abstract - 추상의, 미완성의
- page: 371
- keywords:
  - abstract
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'abstract - 추상의, 미완성의'(p.371)를 참고한다.

### JST-07-04-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.5
- title: 접근 제어자(access modifier)
- page: 372
- keywords:
  - access modifier
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '접근 제어자(access modifier)'(p.372)를 참고한다.

### JST-07-04-06

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 4. 제어자(modifier)
- subsection: 4.6
- title: 제어자(modifier)의 조합
- page: 377
- keywords:
  - modifier
  - inheritance
  - override
  - package
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '제어자(modifier)의 조합'(p.377)를 참고한다.

### JST-07-05-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.1
- title: 다형성이란?
- page: 378
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '다형성이란?'(p.378)를 참고한다.

### JST-07-05-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.2
- title: 참조변수의 형변환
- page: 380
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '참조변수의 형변환'(p.380)를 참고한다.

### JST-07-05-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.3
- title: instanceof 연산자
- page: 386
- keywords:
  - instanceof
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 'instanceof 연산자'(p.386)를 참고한다.

### JST-07-05-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.4
- title: 참조변수와 인스턴스의 연결
- page: 394
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '참조변수와 인스턴스의 연결'(p.394)를 참고한다.

### JST-07-05-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.5
- title: 매개변수의 다형성
- page: 397
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '매개변수의 다형성'(p.397)를 참고한다.

### JST-07-05-06

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 5. 다형성(polymorphism)
- subsection: 5.6
- title: 여러 종류의 객체를 배열로 다루기
- page: 400
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '여러 종류의 객체를 배열로 다루기'(p.400)를 참고한다.

### JST-07-06-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 6. 추상클래스(abstract class)
- subsection: 6.1
- title: 추상 클래스란?
- page: 405
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '추상 클래스란?'(p.405)를 참고한다.

### JST-07-06-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 6. 추상클래스(abstract class)
- subsection: 6.2
- title: 추상 메서드(abstract method)
- page: 405
- keywords:
  - abstract method
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '추상 메서드(abstract method)'(p.405)를 참고한다.

### JST-07-06-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 6. 추상클래스(abstract class)
- subsection: 6.3
- title: 추상 클래스의 작성
- page: 407
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '추상 클래스의 작성'(p.407)를 참고한다.

### JST-07-07-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.1
- title: 인터페이스란?
- page: 411
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스란?'(p.411)를 참고한다.

### JST-07-07-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.2
- title: 인터페이스의 작성
- page: 411
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스의 작성'(p.411)를 참고한다.

### JST-07-07-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.3
- title: 인터페이스의 상속
- page: 412
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스의 상속'(p.412)를 참고한다.

### JST-07-07-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.4
- title: 인터페이스의 구현
- page: 412
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스의 구현'(p.412)를 참고한다.

### JST-07-07-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.5
- title: 인터페이스를 이용한 다중 상속
- page: 415
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스를 이용한 다중 상속'(p.415)를 참고한다.

### JST-07-07-06

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.6
- title: 인터페이스를 이용한 다형성
- page: 417
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스를 이용한 다형성'(p.417)를 참고한다.

### JST-07-07-07

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.7
- title: 인터페이스의 장점
- page: 420
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스의 장점'(p.420)를 참고한다.

### JST-07-07-08

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.8
- title: 인터페이스의 이해
- page: 426
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '인터페이스의 이해'(p.426)를 참고한다.

### JST-07-07-09

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 7. 인터페이스(interface)
- subsection: 7.9
- title: 디폴트 메서드, static메서드, private메서드
- page: 430
- keywords:
  - static
  - private
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '디폴트 메서드, static메서드, private메서드'(p.430)를 참고한다.

### JST-07-08-01

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 8. 내부 클래스(inner class)
- subsection: 8.1
- title: 내부 클래스란?
- page: 434
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '내부 클래스란?'(p.434)를 참고한다.

### JST-07-08-02

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 8. 내부 클래스(inner class)
- subsection: 8.2
- title: 내부 클래스의 종류와 특징
- page: 435
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '내부 클래스의 종류와 특징'(p.435)를 참고한다.

### JST-07-08-03

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 8. 내부 클래스(inner class)
- subsection: 8.3
- title: 내부 클래스의 선언
- page: 435
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '내부 클래스의 선언'(p.435)를 참고한다.

### JST-07-08-04

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 8. 내부 클래스(inner class)
- subsection: 8.4
- title: 내부 클래스의 제어자와 접근성
- page: 436
- keywords:
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
  - inner class
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '내부 클래스의 제어자와 접근성'(p.436)를 참고한다.

### JST-07-08-05

- source: 자바의 정석
- chapter: 7장 객체지향 프로그래밍 II
- section: 8. 내부 클래스(inner class)
- subsection: 8.5
- title: 익명 클래스(anonymous class)
- page: 441
- keywords:
  - anonymous class
  - inheritance
  - override
  - package
  - modifier
  - polymorphism
  - abstract
  - interface
- mapping_hint:
  상속·재정의·가시성 제어자·다형성·추상 클래스·인터페이스·내부 클래스 구조가 diff에서 바뀔 때 '익명 클래스(anonymous class)'(p.441)를 참고한다.

### JST-08-01-01

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.1
- title: 프로그램 오류
- page: 444
- keywords:
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '프로그램 오류'(p.444)를 참고한다.

### JST-08-01-02

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.2
- title: 예외 클래스의 계층구조
- page: 445
- keywords:
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '예외 클래스의 계층구조'(p.445)를 참고한다.

### JST-08-01-03

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.3
- title: 예외 처리하기 - try-catch문
- page: 446
- keywords:
  - try
  - catch
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '예외 처리하기 - try-catch문'(p.446)를 참고한다.

### JST-08-01-04

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.4
- title: try-catch문에서의 흐름
- page: 449
- keywords:
  - try
  - catch
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 'try-catch문에서의 흐름'(p.449)를 참고한다.

### JST-08-01-05

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.5
- title: 예외의 발생과 catch블럭
- page: 450
- keywords:
  - catch
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '예외의 발생과 catch블럭'(p.450)를 참고한다.

### JST-08-01-06

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.6
- title: 예외 발생시키기
- page: 454
- keywords:
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '예외 발생시키기'(p.454)를 참고한다.

### JST-08-01-07

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.7
- title: 메서드에 예외 선언하기
- page: 457
- keywords:
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '메서드에 예외 선언하기'(p.457)를 참고한다.

### JST-08-01-08

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.8
- title: finally블럭
- page: 464
- keywords:
  - finally
  - exception
  - try-catch
  - throw
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 'finally블럭'(p.464)를 참고한다.

### JST-08-01-09

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.9
- title: 자동 자원 반환 - try-with-resources문
- page: 466
- keywords:
  - try
  - with
  - resources
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '자동 자원 반환 - try-with-resources문'(p.466)를 참고한다.

### JST-08-01-10

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.10
- title: 사용자정의 예외 만들기
- page: 469
- keywords:
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '사용자정의 예외 만들기'(p.469)를 참고한다.

### JST-08-01-11

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.11
- title: 예외 되던지기(exception re-throwing)
- page: 472
- keywords:
  - exception re-throwing
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
  - chained exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '예외 되던지기(exception re-throwing)'(p.472)를 참고한다.

### JST-08-01-12

- source: 자바의 정석
- chapter: 8장 예외 처리(exception handling)
- section: 1. 예외 처리(exception handling)
- subsection: 1.12
- title: 연결된 예외(chained exception)
- page: 474
- keywords:
  - chained exception
  - exception
  - try-catch
  - throw
  - finally
  - try-with-resources
  - custom exception
- mapping_hint:
  예외 발생·전파·변환, 자원 정리, 사용자 정의 예외가 diff에서 추가·변경될 때 '연결된 예외(chained exception)'(p.474)를 참고한다.

### JST-09-01-01

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 1. java.lang패키지
- subsection: 1.1
- title: Object클래스
- page: 480
- keywords:
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
  - BigDecimal
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'Object클래스'(p.480)를 참고한다.

### JST-09-01-02

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 1. java.lang패키지
- subsection: 1.2
- title: String클래스
- page: 494
- keywords:
  - String
  - Object
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
  - BigDecimal
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'String클래스'(p.494)를 참고한다.

### JST-09-01-03

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 1. java.lang패키지
- subsection: 1.3
- title: StringBuffer와 StringBuilder
- page: 508
- keywords:
  - StringBuffer
  - StringBuilder
  - Object
  - String
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'StringBuffer와 StringBuilder'(p.508)를 참고한다.

### JST-09-01-04

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 1. java.lang패키지
- subsection: 1.4
- title: Math클래스
- page: 514
- keywords:
  - Math
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'Math클래스'(p.514)를 참고한다.

### JST-09-01-05

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 1. java.lang패키지
- subsection: 1.5
- title: 래퍼(wrapper) 클래스
- page: 521
- keywords:
  - wrapper
  - Object
  - String
  - StringBuilder
  - Objects
  - regex
  - BigInteger
  - BigDecimal
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 '래퍼(wrapper) 클래스'(p.521)를 참고한다.

### JST-09-02-01

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.1
- title: java.util.Objects클래스
- page: 526
- keywords:
  - java.util.Objects
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.util.Objects클래스'(p.526)를 참고한다.

### JST-09-02-02

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.2
- title: java.util.Random클래스
- page: 530
- keywords:
  - java.util.Random
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.util.Random클래스'(p.530)를 참고한다.

### JST-09-02-03

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.3
- title: 정규식(Regular Expression) - java.util.regex
- page: 535
- keywords:
  - Regular Expression
  - java.util.regex
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 '정규식(Regular Expression) - java.util.regex'(p.535)를 참고한다.

### JST-09-02-04

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.4
- title: java.util.Scanner클래스
- page: 540
- keywords:
  - java.util.Scanner
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.util.Scanner클래스'(p.540)를 참고한다.

### JST-09-02-05

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.5
- title: java.util.StringTokenizer클래스
- page: 543
- keywords:
  - java.util.StringTokenizer
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.util.StringTokenizer클래스'(p.543)를 참고한다.

### JST-09-02-06

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.6
- title: java.math.BigInteger클래스
- page: 548
- keywords:
  - java.math.BigInteger
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.math.BigInteger클래스'(p.548)를 참고한다.

### JST-09-02-07

- source: 자바의 정석
- chapter: 9장 java.lang패키지와 유용한 클래스
- section: 2. 유용한 클래스
- subsection: 2.7
- title: java.math.BigDecimal클래스
- page: 551
- keywords:
  - java.math.BigDecimal
  - Object
  - String
  - StringBuilder
  - wrapper
  - Objects
  - regex
  - BigInteger
- mapping_hint:
  Object 메서드·문자열·래퍼 타입·정규식·큰 수(BigInteger/BigDecimal) 연산이 diff에 쓰일 때 'java.math.BigDecimal클래스'(p.551)를 참고한다.

### JST-10-01-01

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 1. 날짜와 시간
- subsection: 1.1
- title: Calendar와 Date
- page: 558
- keywords:
  - Calendar
  - Date
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'Calendar와 Date'(p.558)를 참고한다.

### JST-10-02-01

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 2. 형식화 클래스
- subsection: 2.1
- title: DecimalFormat
- page: 570
- keywords:
  - DecimalFormat
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'DecimalFormat'(p.570)를 참고한다.

### JST-10-02-02

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 2. 형식화 클래스
- subsection: 2.2
- title: SimpleDateFormat
- page: 574
- keywords:
  - SimpleDateFormat
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'SimpleDateFormat'(p.574)를 참고한다.

### JST-10-02-03

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 2. 형식화 클래스
- subsection: 2.3
- title: ChoiceFormat
- page: 578
- keywords:
  - ChoiceFormat
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'ChoiceFormat'(p.578)를 참고한다.

### JST-10-02-04

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 2. 형식화 클래스
- subsection: 2.4
- title: MessageFormat
- page: 579
- keywords:
  - MessageFormat
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'MessageFormat'(p.579)를 참고한다.

### JST-10-03-01

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.1
- title: java.time패키지의 핵심 클래스
- page: 582
- keywords:
  - java.time
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'java.time패키지의 핵심 클래스'(p.582)를 참고한다.

### JST-10-03-02

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.2
- title: LocalDate와 LocalTime
- page: 585
- keywords:
  - LocalDate
  - LocalTime
  - date
  - time
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'LocalDate와 LocalTime'(p.585)를 참고한다.

### JST-10-03-03

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.3
- title: Instant
- page: 590
- keywords:
  - Instant
  - date
  - time
  - LocalDate
  - ZonedDateTime
  - Duration
  - format
  - parse
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'Instant'(p.590)를 참고한다.

### JST-10-03-04

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.4
- title: LocalDateTime과 ZonedDateTime
- page: 591
- keywords:
  - LocalDateTime
  - ZonedDateTime
  - date
  - time
  - LocalDate
  - Instant
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'LocalDateTime과 ZonedDateTime'(p.591)를 참고한다.

### JST-10-03-05

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.5
- title: TemporalAdjusters
- page: 595
- keywords:
  - TemporalAdjusters
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'TemporalAdjusters'(p.595)를 참고한다.

### JST-10-03-06

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.6
- title: Period와 Duration
- page: 597
- keywords:
  - Period
  - Duration
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - format
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 'Period와 Duration'(p.597)를 참고한다.

### JST-10-03-07

- source: 자바의 정석
- chapter: 10장 날짜와 시간 & 형식
- section: 3. java.time패키지
- subsection: 3.7
- title: 파싱과 포맷
- page: 602
- keywords:
  - date
  - time
  - LocalDate
  - Instant
  - ZonedDateTime
  - Duration
  - format
  - parse
- mapping_hint:
  날짜·시각·시간대(UTC/KST)·기간 계산과 형식화가 diff에 쓰일 때 '파싱과 포맷'(p.602)를 참고한다.

### JST-11-01-01

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.1
- title: 컬렉션 프레임웍의 핵심 인터페이스
- page: 608
- keywords:
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
  - Iterator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 '컬렉션 프레임웍의 핵심 인터페이스'(p.608)를 참고한다.

### JST-11-01-02

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.2
- title: ArrayList와 Vector
- page: 615
- keywords:
  - ArrayList
  - Vector
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'ArrayList와 Vector'(p.615)를 참고한다.

### JST-11-01-03

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.3
- title: LinkedList
- page: 626
- keywords:
  - LinkedList
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'LinkedList'(p.626)를 참고한다.

### JST-11-01-04

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.4
- title: Stack과 Queue
- page: 634
- keywords:
  - Stack
  - Queue
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Stack과 Queue'(p.634)를 참고한다.

### JST-11-01-05

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.5
- title: Iterator, ListIterator, Enumeration
- page: 644
- keywords:
  - Iterator
  - ListIterator
  - Enumeration
  - collection
  - List
  - Set
  - Map
  - HashMap
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Iterator, ListIterator, Enumeration'(p.644)를 참고한다.

### JST-11-01-06

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.6
- title: Arrays
- page: 654
- keywords:
  - Arrays
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Arrays'(p.654)를 참고한다.

### JST-11-01-07

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.7
- title: Comparator와 Comparable
- page: 658
- keywords:
  - Comparator
  - Comparable
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Comparator와 Comparable'(p.658)를 참고한다.

### JST-11-01-08

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.8
- title: HashSet
- page: 661
- keywords:
  - HashSet
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'HashSet'(p.661)를 참고한다.

### JST-11-01-09

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.9
- title: TreeSet
- page: 668
- keywords:
  - TreeSet
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'TreeSet'(p.668)를 참고한다.

### JST-11-01-10

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.10
- title: HashMap과 Hashtable
- page: 674
- keywords:
  - HashMap
  - Hashtable
  - collection
  - List
  - Set
  - Map
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'HashMap과 Hashtable'(p.674)를 참고한다.

### JST-11-01-11

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.11
- title: TreeMap
- page: 684
- keywords:
  - TreeMap
  - collection
  - List
  - Set
  - Map
  - HashMap
  - Comparator
  - Iterator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'TreeMap'(p.684)를 참고한다.

### JST-11-01-12

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.12
- title: Properties
- page: 688
- keywords:
  - Properties
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Properties'(p.688)를 참고한다.

### JST-11-01-13

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.13
- title: Collections
- page: 694
- keywords:
  - Collections
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 'Collections'(p.694)를 참고한다.

### JST-11-01-14

- source: 자바의 정석
- chapter: 11장 컬렉션 프레임웍
- section: 1. 컬렉션 프레임웍(collections framework)
- subsection: 1.14
- title: 컬렉션 클래스 정리 & 요약
- page: 699
- keywords:
  - collection
  - List
  - Set
  - Map
  - HashMap
  - TreeMap
  - Comparator
  - Iterator
- mapping_hint:
  List/Set/Map 구현 선택, 정렬 기준, 순회 방식이 diff에서 바뀔 때 '컬렉션 클래스 정리 & 요약'(p.699)를 참고한다.

### JST-12-01-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.1
- title: 지네릭스란?
- page: 702
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭스란?'(p.702)를 참고한다.

### JST-12-01-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.2
- title: 지네릭 클래스의 선언
- page: 703
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 클래스의 선언'(p.703)를 참고한다.

### JST-12-01-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.3
- title: 지네릭 클래스의 객체 생성과 사용
- page: 706
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 클래스의 객체 생성과 사용'(p.706)를 참고한다.

### JST-12-01-04

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.4
- title: 제한된 지네릭 클래스
- page: 709
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '제한된 지네릭 클래스'(p.709)를 참고한다.

### JST-12-01-05

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.5
- title: 와일드 카드
- page: 711
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '와일드 카드'(p.711)를 참고한다.

### JST-12-01-06

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.6
- title: 지네릭 메서드
- page: 717
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 메서드'(p.717)를 참고한다.

### JST-12-01-07

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.7
- title: 지네릭 타입의 형변환
- page: 720
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 타입의 형변환'(p.720)를 참고한다.

### JST-12-01-08

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 1. 지네릭스(generics)
- subsection: 1.8
- title: 지네릭 타입의 제거
- page: 722
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 타입의 제거'(p.722)를 참고한다.

### JST-12-02-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 2. 열거형
- subsection: 2.1
- title: 열거형이란?
- page: 724
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '열거형이란?'(p.724)를 참고한다.

### JST-12-02-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 2. 열거형
- subsection: 2.2
- title: 열거형의 정의와 사용
- page: 725
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '열거형의 정의와 사용'(p.725)를 참고한다.

### JST-12-02-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 2. 열거형
- subsection: 2.3
- title: 열거형에 멤버 추가하기
- page: 728
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '열거형에 멤버 추가하기'(p.728)를 참고한다.

### JST-12-02-04

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 2. 열거형
- subsection: 2.4
- title: 열거형의 이해
- page: 731
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '열거형의 이해'(p.731)를 참고한다.

### JST-12-03-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 3. 애너테이션(annotation)
- subsection: 3.1
- title: 애너테이션이란?
- page: 735
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '애너테이션이란?'(p.735)를 참고한다.

### JST-12-03-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 3. 애너테이션(annotation)
- subsection: 3.2
- title: 표준 애너테이션
- page: 736
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '표준 애너테이션'(p.736)를 참고한다.

### JST-12-03-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 3. 애너테이션(annotation)
- subsection: 3.3
- title: 메타 애너테이션
- page: 744
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '메타 애너테이션'(p.744)를 참고한다.

### JST-12-03-04

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 3. 애너테이션(annotation)
- subsection: 3.4
- title: 애너테이션 타입 정의하기
- page: 748
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '애너테이션 타입 정의하기'(p.748)를 참고한다.

### JST-12-04-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 4. 레코드(record)
- subsection: 4.1
- title: 레코드란?
- page: 754
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '레코드란?'(p.754)를 참고한다.

### JST-12-04-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 4. 레코드(record)
- subsection: 4.2
- title: 레코드의 특징
- page: 755
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '레코드의 특징'(p.755)를 참고한다.

### JST-12-04-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 4. 레코드(record)
- subsection: 4.3
- title: 레코드의 중첩
- page: 760
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '레코드의 중첩'(p.760)를 참고한다.

### JST-12-04-04

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 4. 레코드(record)
- subsection: 4.4
- title: 지네릭 레코드
- page: 762
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '지네릭 레코드'(p.762)를 참고한다.

### JST-12-04-05

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 4. 레코드(record)
- subsection: 4.5
- title: 레코드와 애너테이션
- page: 764
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '레코드와 애너테이션'(p.764)를 참고한다.

### JST-12-05-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 5. 실드 클래스(sealed class)
- subsection: 5.1
- title: 실드 클래스란?
- page: 766
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '실드 클래스란?'(p.766)를 참고한다.

### JST-12-05-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 5. 실드 클래스(sealed class)
- subsection: 5.2
- title: 실드 클래스의 제약 조건
- page: 767
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '실드 클래스의 제약 조건'(p.767)를 참고한다.

### JST-12-05-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 5. 실드 클래스(sealed class)
- subsection: 5.3
- title: 실드 클래스와 switch식
- page: 769
- keywords:
  - switch
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '실드 클래스와 switch식'(p.769)를 참고한다.

### JST-12-06-01

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.1
- title: 모듈이란?
- page: 774
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '모듈이란?'(p.774)를 참고한다.

### JST-12-06-02

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.2
- title: 모듈 설명자 - module-info.java
- page: 776
- keywords:
  - module
  - info.java
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '모듈 설명자 - module-info.java'(p.776)를 참고한다.

### JST-12-06-03

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.3
- title: 이름없는 모듈과 java.base모듈
- page: 779
- keywords:
  - java.base
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '이름없는 모듈과 java.base모듈'(p.779)를 참고한다.

### JST-12-06-04

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.4
- title: 전이적 의존성과 순환 의존성
- page: 786
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '전이적 의존성과 순환 의존성'(p.786)를 참고한다.

### JST-12-06-05

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.5
- title: 모듈의 컴파일과 실행
- page: 788
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '모듈의 컴파일과 실행'(p.788)를 참고한다.

### JST-12-06-06

- source: 자바의 정석
- chapter: 12장 모던 자바 기능
- section: 6. 모듈(module)
- subsection: 6.6
- title: 자동 모듈
- page: 793
- keywords:
  - generics
  - wildcard
  - enum
  - annotation
  - record
  - sealed
  - module
- mapping_hint:
  제네릭·열거형·애너테이션·record·sealed class가 diff에서 추가·변경될 때 '자동 모듈'(p.793)를 참고한다.

### JST-13-01-01

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.1
- title: 프로세스와 쓰레드
- page: 796
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '프로세스와 쓰레드'(p.796)를 참고한다.

### JST-13-01-02

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.2
- title: 쓰레드의 구현과 실행
- page: 798
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '쓰레드의 구현과 실행'(p.798)를 참고한다.

### JST-13-01-03

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.3
- title: start()와 run()
- page: 802
- keywords:
  - start
  - run
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'start()와 run()'(p.802)를 참고한다.

### JST-13-01-04

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.4
- title: 싱글쓰레드와 멀티쓰레드
- page: 806
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '싱글쓰레드와 멀티쓰레드'(p.806)를 참고한다.

### JST-13-01-05

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.5
- title: 쓰레드의 우선순위
- page: 812
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '쓰레드의 우선순위'(p.812)를 참고한다.

### JST-13-01-06

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.6
- title: 쓰레드 그룹(thread group)
- page: 815
- keywords:
  - thread group
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '쓰레드 그룹(thread group)'(p.815)를 참고한다.

### JST-13-01-07

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.7
- title: 데몬 쓰레드(daemon thread)
- page: 818
- keywords:
  - daemon thread
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '데몬 쓰레드(daemon thread)'(p.818)를 참고한다.

### JST-13-01-08

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 1. 쓰레드
- subsection: 1.8
- title: 쓰레드의 실행제어
- page: 822
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '쓰레드의 실행제어'(p.822)를 참고한다.

### JST-13-02-01

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 2. 쓰레드의 동기화
- subsection: 2.1
- title: synchronized를 이용한 동기화
- page: 841
- keywords:
  - synchronized
  - thread
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'synchronized를 이용한 동기화'(p.841)를 참고한다.

### JST-13-02-02

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 2. 쓰레드의 동기화
- subsection: 2.2
- title: wait()과 notify()
- page: 845
- keywords:
  - wait
  - notify
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'wait()과 notify()'(p.845)를 참고한다.

### JST-13-02-03

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 2. 쓰레드의 동기화
- subsection: 2.3
- title: Lock과 Condition을 이용한 동기화
- page: 853
- keywords:
  - Lock
  - Condition
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'Lock과 Condition을 이용한 동기화'(p.853)를 참고한다.

### JST-13-02-04

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 2. 쓰레드의 동기화
- subsection: 2.4
- title: volatile
- page: 860
- keywords:
  - volatile
  - thread
  - synchronized
  - lock
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'volatile'(p.860)를 참고한다.

### JST-13-02-05

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 2. 쓰레드의 동기화
- subsection: 2.5
- title: fork & join 프레임웍
- page: 862
- keywords:
  - fork
  - join
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'fork & join 프레임웍'(p.862)를 참고한다.

### JST-13-03-01

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.1
- title: 가상 쓰레드란?
- page: 867
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '가상 쓰레드란?'(p.867)를 참고한다.

### JST-13-03-02

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.2
- title: 가상 쓰레드의 생성과 사용
- page: 868
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '가상 쓰레드의 생성과 사용'(p.868)를 참고한다.

### JST-13-03-03

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.3
- title: 가상 쓰레드의 특징
- page: 869
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '가상 쓰레드의 특징'(p.869)를 참고한다.

### JST-13-03-04

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.4
- title: 플랫폼 쓰레드와 가상 쓰레드
- page: 871
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '플랫폼 쓰레드와 가상 쓰레드'(p.871)를 참고한다.

### JST-13-03-05

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.5
- title: 가상 쓰레드의 상태
- page: 878
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '가상 쓰레드의 상태'(p.878)를 참고한다.

### JST-13-03-06

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.6
- title: 가상 쓰레드 작성시 주의사항
- page: 884
- keywords:
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '가상 쓰레드 작성시 주의사항'(p.884)를 참고한다.

### JST-13-03-07

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 3. 가상 쓰레드(virtual thread)
- subsection: 3.7
- title: Continuation과 StackChunk
- page: 885
- keywords:
  - Continuation
  - StackChunk
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'Continuation과 StackChunk'(p.885)를 참고한다.

### JST-13-04-01

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.1
- title: Executor
- page: 889
- keywords:
  - Executor
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'Executor'(p.889)를 참고한다.

### JST-13-04-02

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.2
- title: ThreadFactory
- page: 890
- keywords:
  - ThreadFactory
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'ThreadFactory'(p.890)를 참고한다.

### JST-13-04-03

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.3
- title: ExecutorService
- page: 892
- keywords:
  - ExecutorService
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'ExecutorService'(p.892)를 참고한다.

### JST-13-04-04

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.4
- title: 쓰레드 풀(thread pool)
- page: 898
- keywords:
  - thread pool
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - Future
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 '쓰레드 풀(thread pool)'(p.898)를 참고한다.

### JST-13-04-05

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.5
- title: Future
- page: 903
- keywords:
  - Future
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - CompletableFuture
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'Future'(p.903)를 참고한다.

### JST-13-04-06

- source: 자바의 정석
- chapter: 13장 쓰레드
- section: 4. Executor와 ExecutorService
- subsection: 4.6
- title: CompletableFuture
- page: 913
- keywords:
  - CompletableFuture
  - thread
  - synchronized
  - lock
  - volatile
  - executor
  - thread pool
  - Future
- mapping_hint:
  동시 실행·동기화·락·스레드 풀·비동기 작업이 diff에 들어오거나 동시성 문제가 쟁점일 때 'CompletableFuture'(p.913)를 참고한다.

### JST-14-01-01

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.1
- title: 람다식이란?
- page: 928
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '람다식이란?'(p.928)를 참고한다.

### JST-14-01-02

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.2
- title: 람다식 작성하기
- page: 929
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '람다식 작성하기'(p.929)를 참고한다.

### JST-14-01-03

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.3
- title: 함수형 인터페이스(Functional Interface)
- page: 931
- keywords:
  - Functional Interface
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '함수형 인터페이스(Functional Interface)'(p.931)를 참고한다.

### JST-14-01-04

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.4
- title: java.util.function패키지
- page: 936
- keywords:
  - java.util.function
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 'java.util.function패키지'(p.936)를 참고한다.

### JST-14-01-05

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.5
- title: Function의 합성과 Predicate의 결합
- page: 942
- keywords:
  - Function
  - Predicate
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 'Function의 합성과 Predicate의 결합'(p.942)를 참고한다.

### JST-14-01-06

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 1. 람다식(Lambda expression)
- subsection: 1.6
- title: 메서드 참조
- page: 946
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '메서드 참조'(p.946)를 참고한다.

### JST-14-02-01

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.1
- title: 스트림이란?
- page: 948
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '스트림이란?'(p.948)를 참고한다.

### JST-14-02-02

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.2
- title: 스트림 만들기
- page: 953
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '스트림 만들기'(p.953)를 참고한다.

### JST-14-02-03

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.3
- title: 스트림의 중간연산
- page: 958
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '스트림의 중간연산'(p.958)를 참고한다.

### JST-14-02-04

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.4
- title: Optional<T>와 OptionalInt
- page: 971
- keywords:
  - Optional<T>
  - OptionalInt
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 'Optional<T>와 OptionalInt'(p.971)를 참고한다.

### JST-14-02-05

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.5
- title: 스트림의 최종연산
- page: 976
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '스트림의 최종연산'(p.976)를 참고한다.

### JST-14-02-06

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.6
- title: collect()
- page: 980
- keywords:
  - collect
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 'collect()'(p.980)를 참고한다.

### JST-14-02-07

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.7
- title: Collector구현하기
- page: 997
- keywords:
  - Collector
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 'Collector구현하기'(p.997)를 참고한다.

### JST-14-02-08

- source: 자바의 정석
- chapter: 14장 람다와 스트림
- section: 2. 스트림(stream)
- subsection: 2.8
- title: 스트림의 변환
- page: 1000
- keywords:
  - lambda
  - functional interface
  - method reference
  - stream
  - Optional
  - collect
  - Collector
- mapping_hint:
  람다·함수형 인터페이스·스트림 연산·Optional·collect가 diff에 추가·변경될 때 '스트림의 변환'(p.1000)를 참고한다.

### JST-15-01-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 1. 자바에서의 입출력
- subsection: 1.1
- title: 입출력이란?
- page: 1004
- keywords:
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '입출력이란?'(p.1004)를 참고한다.

### JST-15-01-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 1. 자바에서의 입출력
- subsection: 1.2
- title: 스트림(stream)
- page: 1004
- keywords:
  - stream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '스트림(stream)'(p.1004)를 참고한다.

### JST-15-01-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 1. 자바에서의 입출력
- subsection: 1.3
- title: 바이트 기반 스트림 - InputStream, OutputStream
- page: 1005
- keywords:
  - InputStream
  - OutputStream
  - I/O
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '바이트 기반 스트림 - InputStream, OutputStream'(p.1005)를 참고한다.

### JST-15-01-04

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 1. 자바에서의 입출력
- subsection: 1.4
- title: 보조 스트림
- page: 1007
- keywords:
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '보조 스트림'(p.1007)를 참고한다.

### JST-15-01-05

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 1. 자바에서의 입출력
- subsection: 1.5
- title: 문자 기반 스트림 - Reader, Writer
- page: 1008
- keywords:
  - Reader
  - Writer
  - I/O
  - InputStream
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '문자 기반 스트림 - Reader, Writer'(p.1008)를 참고한다.

### JST-15-02-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 2. 바이트기반 스트림
- subsection: 2.1
- title: InputStream과 OutputStream
- page: 1010
- keywords:
  - InputStream
  - OutputStream
  - I/O
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'InputStream과 OutputStream'(p.1010)를 참고한다.

### JST-15-02-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 2. 바이트기반 스트림
- subsection: 2.2
- title: ByteArrayInputStream과 ByteArrayOutputStream
- page: 1012
- keywords:
  - ByteArrayInputStream
  - ByteArrayOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'ByteArrayInputStream과 ByteArrayOutputStream'(p.1012)를 참고한다.

### JST-15-02-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 2. 바이트기반 스트림
- subsection: 2.3
- title: FileInputStream과 FileOutputStream
- page: 1016
- keywords:
  - FileInputStream
  - FileOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'FileInputStream과 FileOutputStream'(p.1016)를 참고한다.

### JST-15-03-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 3. 바이트 기반의 보조 스트림
- subsection: 3.1
- title: FilterInputStream과 FilterOutputStream
- page: 1019
- keywords:
  - FilterInputStream
  - FilterOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'FilterInputStream과 FilterOutputStream'(p.1019)를 참고한다.

### JST-15-03-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 3. 바이트 기반의 보조 스트림
- subsection: 3.2
- title: BufferedInputStream과 BufferedOutputStream
- page: 1020
- keywords:
  - BufferedInputStream
  - BufferedOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'BufferedInputStream과 BufferedOutputStream'(p.1020)를 참고한다.

### JST-15-03-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 3. 바이트 기반의 보조 스트림
- subsection: 3.3
- title: DataInputStream과 DataOutputStream
- page: 1023
- keywords:
  - DataInputStream
  - DataOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'DataInputStream과 DataOutputStream'(p.1023)를 참고한다.

### JST-15-03-04

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 3. 바이트 기반의 보조 스트림
- subsection: 3.4
- title: SequenceInputStream
- page: 1029
- keywords:
  - SequenceInputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'SequenceInputStream'(p.1029)를 참고한다.

### JST-15-03-05

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 3. 바이트 기반의 보조 스트림
- subsection: 3.5
- title: PrintStream
- page: 1031
- keywords:
  - PrintStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'PrintStream'(p.1031)를 참고한다.

### JST-15-04-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 4. 문자기반 스트림
- subsection: 4.1
- title: Reader와 Writer
- page: 1035
- keywords:
  - Reader
  - Writer
  - I/O
  - InputStream
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'Reader와 Writer'(p.1035)를 참고한다.

### JST-15-04-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 4. 문자기반 스트림
- subsection: 4.2
- title: FileReader와 FileWriter
- page: 1037
- keywords:
  - FileReader
  - FileWriter
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'FileReader와 FileWriter'(p.1037)를 참고한다.

### JST-15-04-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 4. 문자기반 스트림
- subsection: 4.3
- title: PipedReader와 PipedWriter
- page: 1039
- keywords:
  - PipedReader
  - PipedWriter
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'PipedReader와 PipedWriter'(p.1039)를 참고한다.

### JST-15-04-04

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 4. 문자기반 스트림
- subsection: 4.4
- title: StringReader와 StringWriter
- page: 1041
- keywords:
  - StringReader
  - StringWriter
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'StringReader와 StringWriter'(p.1041)를 참고한다.

### JST-15-05-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 5. 문자기반의 보조스트림
- subsection: 5.1
- title: BufferedReader와 BufferedWriter
- page: 1042
- keywords:
  - BufferedReader
  - BufferedWriter
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'BufferedReader와 BufferedWriter'(p.1042)를 참고한다.

### JST-15-05-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 5. 문자기반의 보조스트림
- subsection: 5.2
- title: InputStreamReader와 OutputStreamWriter
- page: 1043
- keywords:
  - InputStreamReader
  - OutputStreamWriter
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'InputStreamReader와 OutputStreamWriter'(p.1043)를 참고한다.

### JST-15-06-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 6. 표준 입출력과 File
- subsection: 6.1
- title: 표준 입출력 - System.in, System.out, System.err
- page: 1045
- keywords:
  - System.in
  - System.out
  - System.err
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '표준 입출력 - System.in, System.out, System.err'(p.1045)를 참고한다.

### JST-15-06-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 6. 표준 입출력과 File
- subsection: 6.2
- title: 표준입출력의 대상변경 - setOut(), setErr(), setIn()
- page: 1047
- keywords:
  - setOut
  - setErr
  - setIn
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '표준입출력의 대상변경 - setOut(), setErr(), setIn()'(p.1047)를 참고한다.

### JST-15-06-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 6. 표준 입출력과 File
- subsection: 6.3
- title: RandomAccessFile
- page: 1049
- keywords:
  - RandomAccessFile
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'RandomAccessFile'(p.1049)를 참고한다.

### JST-15-06-04

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 6. 표준 입출력과 File
- subsection: 6.4
- title: File
- page: 1053
- keywords:
  - File
  - I/O
  - InputStream
  - Reader
  - Buffered
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'File'(p.1053)를 참고한다.

### JST-15-07-01

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 7. 직렬화(Serialization)
- subsection: 7.1
- title: 직렬화란?
- page: 1072
- keywords:
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '직렬화란?'(p.1072)를 참고한다.

### JST-15-07-02

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 7. 직렬화(Serialization)
- subsection: 7.2
- title: ObjectInputStream, ObjectOutputStream
- page: 1073
- keywords:
  - ObjectInputStream
  - ObjectOutputStream
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 'ObjectInputStream, ObjectOutputStream'(p.1073)를 참고한다.

### JST-15-07-03

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 7. 직렬화(Serialization)
- subsection: 7.3
- title: 직렬화 가능한 클래스 만들기 - Serializable, transient
- page: 1075
- keywords:
  - Serializable
  - transient
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '직렬화 가능한 클래스 만들기 - Serializable, transient'(p.1075)를 참고한다.

### JST-15-07-04

- source: 자바의 정석
- chapter: 15장 입출력(I/O)
- section: 7. 직렬화(Serialization)
- subsection: 7.4
- title: 직렬화가능한 클래스의 버전관리
- page: 1081
- keywords:
  - I/O
  - InputStream
  - Reader
  - Buffered
  - File
  - serialization
  - Serializable
- mapping_hint:
  파일·바이트/문자 스트림 입출력, 직렬화가 diff에 쓰일 때 '직렬화가능한 클래스의 버전관리'(p.1081)를 참고한다.

### JST-16-01-01

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 1. 네트워킹(networking)
- subsection: 1.1
- title: 클라이언트/서버(client/server)
- page: 1084
- keywords:
  - client/server
  - network
  - IP
  - URL
  - URI
  - socket
  - TCP
  - UDP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 '클라이언트/서버(client/server)'(p.1084)를 참고한다.

### JST-16-01-02

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 1. 네트워킹(networking)
- subsection: 1.2
- title: IP주소(IP address)
- page: 1086
- keywords:
  - IP address
  - IP
  - network
  - client/server
  - URL
  - URI
  - socket
  - TCP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'IP주소(IP address)'(p.1086)를 참고한다.

### JST-16-01-03

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 1. 네트워킹(networking)
- subsection: 1.3
- title: InetAddress
- page: 1087
- keywords:
  - InetAddress
  - network
  - client/server
  - IP
  - URL
  - URI
  - socket
  - TCP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'InetAddress'(p.1087)를 참고한다.

### JST-16-01-04

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 1. 네트워킹(networking)
- subsection: 1.4
- title: URL과 URI
- page: 1089
- keywords:
  - URL
  - URI
  - network
  - client/server
  - IP
  - socket
  - TCP
  - UDP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'URL과 URI'(p.1089)를 참고한다.

### JST-16-01-05

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 1. 네트워킹(networking)
- subsection: 1.5
- title: URLConnection
- page: 1092
- keywords:
  - URLConnection
  - network
  - client/server
  - IP
  - URL
  - URI
  - socket
  - TCP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'URLConnection'(p.1092)를 참고한다.

### JST-16-02-01

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 2. 소켓 프로그래밍
- subsection: 2.1
- title: TCP와 UDP
- page: 1097
- keywords:
  - TCP
  - UDP
  - network
  - client/server
  - IP
  - URL
  - URI
  - socket
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'TCP와 UDP'(p.1097)를 참고한다.

### JST-16-02-02

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 2. 소켓 프로그래밍
- subsection: 2.2
- title: TCP소켓 프로그래밍
- page: 1098
- keywords:
  - TCP
  - network
  - client/server
  - IP
  - URL
  - URI
  - socket
  - UDP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'TCP소켓 프로그래밍'(p.1098)를 참고한다.

### JST-16-02-03

- source: 자바의 정석
- chapter: 16장 네트워킹(networking)
- section: 2. 소켓 프로그래밍
- subsection: 2.3
- title: UDP소켓 프로그래밍
- page: 1116
- keywords:
  - UDP
  - network
  - client/server
  - IP
  - URL
  - URI
  - socket
  - TCP
- mapping_hint:
  외부 서버 호출·URL/URI 처리·소켓·TCP/UDP 같은 네트워크 동작이 diff에 쓰일 때 'UDP소켓 프로그래밍'(p.1116)를 참고한다.
