package app.jeongsan.server.gathering.javaimpl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** FC-029: Kotlin을 호출하지 않는 전체 명단 응답 완료 판정. 확정·자동 제외는 하지 않는다. */
public final class AutoSettlementPolicy {
    private AutoSettlementPolicy() {}

    public record Seat(long participantId, Instant joinedAt) {}
    public record Round(long id, long unitId, long payerId) {}
    public record Answer(long participantId, long roundId, String source) {}
    private record Key(long participantId, long roundId) {}
    public record Plan(List<Long> includedIds, List<Long> excludedIds, String error) {}

    public static void validateHeadcount(Integer headcount) {
        if (headcount != null && (headcount < 2 || headcount > 50))
            throw new IllegalArgumentException("MALFORMED_REQUEST");
    }

    public static Plan plan(long unitId, long hostId, String state, Integer headcount,
                            List<Long> activeIds, List<Seat> seats, List<Round> rounds, List<Answer> answers) {
        validateHeadcount(headcount);
        if (!"OPEN".equals(state)) return null;
        List<Long> ordered = seats.stream().filter(seat -> activeIds.contains(seat.participantId()))
                .sorted(Comparator.comparing((Seat seat) -> seat.participantId() != hostId)
                        .thenComparing(Seat::joinedAt).thenComparingLong(Seat::participantId))
                .map(Seat::participantId).toList();
        List<Round> selectedRounds = rounds.stream().filter(round -> round.unitId() == unitId).toList();
        if (selectedRounds.isEmpty() || !ordered.contains(hostId)) return null;
        List<Long> included = List.copyOf(ordered);
        Set<Key> answered = answers.stream().filter(answer -> "SELF".equals(answer.source()) || "HOST".equals(answer.source()))
                .map(answer -> new Key(answer.participantId(), answer.roundId())).collect(Collectors.toSet());
        for (long participantId : included)
            for (Round round : selectedRounds)
                if (!answered.contains(new Key(participantId, round.id()))) return null;
        return new Plan(included, List.of(), null);
    }
}
