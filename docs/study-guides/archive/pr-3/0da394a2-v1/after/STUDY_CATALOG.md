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
