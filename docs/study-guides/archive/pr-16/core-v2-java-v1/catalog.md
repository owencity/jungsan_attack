# 이 가이드가 참조한 목차 원문

전체 목차: [d5feb4677f71c9fe973e31118fd3b82d27eb2788](https://github.com/owencity/jungsan_attack/blob/d5feb4677f71c9fe973e31118fd3b82d27eb2788/STUDY_CATALOG.md).

아래 항목은 해당 커밋의 원문에서 그대로 발췌했다.

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

