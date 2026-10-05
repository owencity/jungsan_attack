package app.jeongsan.core.javaimpl;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 같은 파일명의 Kotlin 타입과 JVM 이름이 충돌하지 않도록 Java 입력 타입을 묶는다. */
public final class Model {
    private Model() {}

    public record SettlementInput(List<Participant> participants, List<Round> rounds,
                                  Map<AttendanceKey, Attendance> attendance, List<DrinkItem> drinkItems) {
        public SettlementInput {
            participants = List.copyOf(participants);
            rounds = List.copyOf(rounds);
            // 오류 순서도 비교 대상이므로 Map.copyOf의 미정 순서 대신 삽입 순서를 보존한다.
            attendance = Collections.unmodifiableMap(new LinkedHashMap<>(attendance));
            drinkItems = List.copyOf(drinkItems);
        }

        public SettlementInput(List<Participant> participants, List<Round> rounds) {
            this(participants, rounds, Map.of(), List.of());
        }

        public SettlementInput(List<Participant> participants, List<Round> rounds,
                               Map<AttendanceKey, Attendance> attendance) {
            this(participants, rounds, attendance, List.of());
        }

        public Attendance attendanceOf(long participantId, long roundId) {
            return attendance.getOrDefault(new AttendanceKey(participantId, roundId), Attendance.ABSENT);
        }

        public List<Round> effectiveRounds() {
            if (drinkItems.isEmpty()) return rounds;
            Map<Long, List<DrinkItem>> byRound = drinkItems.stream()
                    .collect(Collectors.groupingBy(DrinkItem::roundId));
            return rounds.stream().map(round -> {
                List<DrinkItem> items = byRound.get(round.id());
                if (items == null) return round;
                BigInteger sum = items.stream()
                        .map(item -> BigInteger.valueOf(item.bottleCount()).multiply(BigInteger.valueOf(item.unitPrice())))
                        .reduce(BigInteger.ZERO, BigInteger::add);
                // Kotlin과 동일하게 포화시켜 이후 검증이 사용자 오류를 반환하게 한다.
                long alcohol = sum.max(BigInteger.valueOf(Long.MIN_VALUE))
                        .min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
                return new Round(round.id(), round.seq(), round.label(), round.total(), alcohol, round.payerId());
            }).toList();
        }
    }

    public record Participant(long id, String name) {}
    public record Round(long id, int seq, String label, long total, long alcohol, long payerId) {}
    public record DrinkItem(long roundId, String name, int bottleCount, long unitPrice) {}
    public record AttendanceKey(long participantId, long roundId) {}

    public enum Attendance {
        ABSENT(false, false, false), EXEMPT(true, false, true),
        SOBER(true, false, false), DRANK(true, true, false);

        private final boolean attended;
        private final boolean drank;
        private final boolean exempt;

        Attendance(boolean attended, boolean drank, boolean exempt) {
            this.attended = attended;
            this.drank = drank;
            this.exempt = exempt;
        }

        public boolean attended() { return attended; }
        public boolean drank() { return drank; }
        public boolean exempt() { return exempt; }
    }
}
