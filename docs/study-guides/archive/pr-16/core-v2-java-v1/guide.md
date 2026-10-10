# Core v2 Java 구현과 Kotlin 비교 — 스터디 가이드 v1

- 코드 기준: `7cd7ff1c8e8442733d11590df17eea119fc63a27` → `36526853b4532df1a6926e13a640fbb6169fbd61` (Java 추가 구현 diff).
- 목차·정책 기준: 최신 `origin/main` 확인 커밋 `d5feb4677f71c9fe973e31118fd3b82d27eb2788`. 로컬의 오래된 목차는 사용하지 않았다.
- 아래 추천은 새 Java 파일과 새 `JavaParitySpec.kt`에서 확인한 코드만 근거로 삼는다.
- Kotlin 운영 파일은 이번 diff에서 수정하지 않았다. 아래 대응표의 Kotlin 위치는 비교용 기존 코드다.
- 읽을 순서: 등록된 목차 위치 → 연결된 클래스·함수의 추가 diff → 한 줄 관점.

## Java 기본기

- 자바의 정석 6장 3.8 기본형 매개변수와 참조형 매개변수 (p.288) — [Model.java:17 · SettlementInput compact constructor](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L17): 생성자에 받은 리스트와 원본 참조를 끊는 지점을 확인한다.
- 자바의 정석 6장 5.4 생성자에서 다른 생성자 호출하기 - this(), this (p.319) — [Model.java:25 · SettlementInput overloaded constructor](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L25): 세 생성자의 기본 컬렉션이 어느 생성자에서 공통 처리되는지 확인한다.
- 자바의 정석 7장 4.2 static - 클래스의, 공통적인 (p.368) — [Settlement.java:19 · settle](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L19): 인스턴스 상태 없이 호출하는 계산 진입점과 Kotlin object 진입점을 비교한다.
- 자바의 정석 7장 7.1 인터페이스란? (p.411) — [SettlementResult.java:20 · SettlementOutcome](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L20): 성공·실패 타입이 같은 반환 계약을 구현하는 관계를 확인한다.
- 자바의 정석 7장 8.2 내부 클래스의 종류와 특징 (p.435) — [Model.java:55 · Model.Participant](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L55): 같은 기능 파일에 묶인 중첩 타입의 접근 이름을 확인한다.
- 자바의 정석 9장 1.1 Object클래스 (p.480) — [Rational.java:73 · equals / hashCode](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L73): 약분된 유리수가 객체 동등성과 해시 키에서 같은 값으로 취급되는지 확인한다.
- 자바의 정석 9장 2.6 java.math.BigInteger클래스 (p.548) — [Model.java:44 · effectiveRounds](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L44): 병 수 곱셈과 누적을 long으로 축소하기 전 계산하는 위치를 확인한다.
- 자바의 정석 11장 1.7 Comparator와 Comparable (p.658) — [Settlement.java:76 · adjustment participant comparator](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L76): 원부담 내림차순 뒤 숫자 id 오름차순을 적용하는지 확인한다.
- 자바의 정석 11장 1.10 HashMap과 Hashtable (p.674) — [Validation.java:40 · duplicatesOf](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Validation.java#L40): 중복 오류가 첫 등장 순서를 유지하는 이유를 맵 구현과 연결한다.
- 자바의 정석 11장 1.11 TreeMap (p.684) — [Settlement.java:64 · recipient grouping](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L64): 수취인 정렬이 송금 목록의 순서까지 결정하는지 확인한다.
- 자바의 정석 11장 1.13 Collections (p.694) — [SettlementResult.java:45 · freeze](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L45): 읽기 전용 래퍼와 원본 복사를 각각 수행하는 위치를 확인한다.
- 자바의 정석 12장 1.6 지네릭 메서드 (p.717) — [SettlementResult.java:44 · freeze](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L44): 입력·결과의 서로 다른 맵 타입을 같은 제네릭 메서드로 복사하는 경로를 확인한다.
- 자바의 정석 12장 2.3 열거형에 멤버 추가하기 (p.728) — [Model.java:68 · Attendance](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L68): 네 enum 상수의 참석·음주·면제 필드가 배분 분모를 결정하는지 확인한다.
- 자바의 정석 14장 2.3 스트림의 중간연산 (p.958) — [Settlement.java:111 · buildTransfers](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L111): 자기 송금과 0원 송금을 걸러낸 뒤 송금자 순으로 정렬하는지 확인한다.
- 자바의 정석 14장 2.5 스트림의 최종연산 (p.976) — [Model.java:46 · effectiveRounds](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L46): 합산의 초기값과 연산이 오버플로 없이 최종 금액을 만드는지 확인한다.
- 자바의 정석 14장 2.6 collect() (p.980) — [Settlement.java:64 · recipient grouping](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L64): 차수를 결제자별로 묶은 후에만 원부담을 올리는 실행 순서를 확인한다.

## Effective Java

- Item 1, 생성자 대신 정적 팩터리 메서드를 고려하라 — [Rational.java:22 · of](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L22): 모든 연산 결과가 정규화 팩터리를 거치는지 확인한다.
- Item 10, equals는 일반 규약을 지켜 재정의하라 — [Rational.java:73 · equals](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L73): 통분 가능한 다른 표현이 정규화 후 같은 값으로 비교되는지 확인한다.
- Item 11, equals를 재정의하려거든 hashCode도 재정의하라 — [Rational.java:77 · hashCode](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L77): equals에서 비교한 두 필드가 hashCode에도 쓰이는지 확인한다.
- Item 17, 변경 가능성을 최소화하라 — [Rational.java:10 · Rational](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L10): 연산이 기존 값을 변경하지 않고 새 유리수를 반환하는지 확인한다.
- Item 50, 적시에 방어적 복사본을 만들라 — [Model.java:17 · SettlementInput](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L17): 입력 컬렉션의 사후 변경으로 계산이 바뀌지 않도록 복사한 지점을 확인한다.

## 함수형 Java

- Chapter 4 / Section 4.4, 불변성 만들기 — [SettlementResult.java:45 · freeze](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L45): 결과 record 내부 맵까지 변경 불가능한지 확인한다.
- Chapter 5 / Section 5.2, 도움을 주기 위한 레코드 — [Model.java:14 · SettlementInput](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L14): record의 값 비교와 compact constructor의 복사 책임을 구분해 확인한다.
- Chapter 6 / Section 6.3, 스트림 파이프라인 구축하기 — [Settlement.java:71 · raw amount pipeline](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L71): 차수 근거에서 해당 결제자의 원부담만 추출해 정확히 합산하는지 확인한다.
- Chapter 7 / Section 7.1, 원시 스트림 — [Settlement.java:89 · incomingTotal](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L89): 박싱된 맵 값을 원시형 스트림으로 합산하는 반환 타입을 확인한다.
- Chapter 7 / Section 7.9, 컬렉터 알아보기 — [Settlement.java:64 · recipient grouping](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L64): 그룹 맵 공급자를 TreeMap으로 지정한 효과를 확인한다.

## Kotlin 기본

- 3강, 코틀린에서 Type을 다루는 방법 — [JavaParitySpec.kt:187 · assertParity](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L187): Java 결과의 타입 검사 뒤 접근 가능한 멤버와 Kotlin 결과 캐스팅을 확인한다.
- 16강, 코틀린에서 다양한 함수를 다루는 방법 — [JavaParitySpec.kt:159 · SettlementInput.toJava](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L159): 확장 함수가 입력 어댑터 역할을 하고 실제 Java 엔진 호출과 구분되는지 확인한다.
- 18강, 코틀린에서 컬렉션을 함수형으로 다루는 방법 — [JavaParitySpec.kt:171 · JResult.toKotlin](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L171): 중첩된 Java 결과를 Kotlin 값 객체로 바꾸면서 빠뜨린 필드가 없는지 확인한다.

## Kotlin 고급

- 17강, 연산자 오버로딩 — [JavaParitySpec.kt:27 · Rational parity assertions](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L27): Java plus 메서드와 Kotlin + 연산자 호출이 같은 결과를 내는지 확인한다.

## 기술별 학습 포인트

- Java 21 record / sealed permits — [SettlementResult.java:20 · Java 21 record / sealed permits](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L20): record의 얕은 불변성과 sealed 반환 타입의 하위 타입 제한을 확인한다.
- differential test — [JavaParitySpec.kt:180 · differential test](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L180): 금액만이 아니라 정확한 근거·오류 위치·출력 순서를 비교하는 경계를 확인한다.
- seeded generated cases — [JavaParitySpec.kt:107 · seeded generated cases](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/test/kotlin/app/jeongsan/core/JavaParitySpec.kt#L107): 같은 입력 2,000건을 재현하고 성공과 음수 조정 실패가 모두 포함되는지 확인한다.

## 같은 기능에서 함께 볼 위치

Java는 이번 추가 diff, Kotlin은 기준 커밋에 이미 있는 구현이다.

| 기능 | Java 추가 코드 | Kotlin 비교 코드 | 관점 |
|---|---|---|---|
| effectiveRounds | [Model.java:37](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Model.java#L37) | [Model.kt:28](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Model.kt#L28) | 술병 합산을 BigInteger로 처리한 뒤 Long 범위로 줄이는 위치 |
| ceilTo | [Rational.java:52](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Rational.java#L52) | [Rational.kt:47](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Rational.kt#L47) | 음수 나머지는 보정하지 않고 양수 나머지만 올리는 조건 |
| validateOnConfirm | [Validation.java:116](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Validation.java#L116) | [Validation.kt:193](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Validation.kt#L193) | 미응답과 불참을 구분하고 전체 총액 상한을 검사하는 단계 |
| compute | [Settlement.java:36](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/Settlement.java#L36) | [Settlement.kt:38](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L38) | 결제자별 원부담 합산·조정자 선택·송금 순서 |
| SettlementOutcome | [SettlementResult.java:20](https://github.com/owencity/jungsan_attack/blob/36526853b4532df1a6926e13a640fbb6169fbd61/core/src/main/java/app/jeongsan/core/javaimpl/SettlementResult.java#L20) | [SettlementResult.kt:7](https://github.com/owencity/jungsan_attack/blob/7cd7ff1c8e8442733d11590df17eea119fc63a27/core/src/main/kotlin/app/jeongsan/core/SettlementResult.kt#L7) | 성공·실패·근거 필드의 대응 |

## 검증 기록과 남은 확인

Core 54건 통과(기존 48 + 비교 6, 실패·건너뛰기 0), 서버 컴파일 통과. 고정 seed 비교 입력 2,000건과 오류 코드 15개 전체를 확인했다.
전체 총액 상한이 CONFIRM에서만 검사되는 기존 Kotlin 동작은 Java에도 유지했다. 명세 §5.1과의 차이는 별도 리뷰 확인 사항이다.
이 가이드의 생성은 학습 완료를 뜻하지 않는다. [학습 기록](progress.md)에 본인이 진행 상황을 남긴다.

이후 문서 커밋이 PR head에 추가되어도 코드 링크는 위 구현 커밋에 고정된다. 원본 [코드 diff](changes.patch), [목차 스냅샷](catalog.md), [정책 스냅샷](policy.md), [메타데이터](manifest.json)를 함께 보존한다.

