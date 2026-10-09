package app.jeongsan.server.gathering.javaimpl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Kotlin을 호출하지 않는 인원 선택·응답 완료·결제자 보존 학습 구현. */
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
        if (!"OPEN".equals(state) || headcount == null) return null;
        List<Long> ordered = seats.stream().filter(seat -> activeIds.contains(seat.participantId()))
                .sorted(Comparator.comparing((Seat seat) -> seat.participantId() != hostId)
                        .thenComparing(Seat::joinedAt).thenComparingLong(Seat::participantId))
                .map(Seat::participantId).toList();
        List<Round> selectedRounds = rounds.stream().filter(round -> round.unitId() == unitId).toList();
        if (ordered.size() < headcount || selectedRounds.isEmpty() || !ordered.contains(hostId)) return null;
        List<Long> included = List.copyOf(ordered.subList(0, headcount));
        Set<Key> answered = answers.stream().filter(answer -> "SELF".equals(answer.source()) || "HOST".equals(answer.source()))
                .map(answer -> new Key(answer.participantId(), answer.roundId())).collect(Collectors.toSet());
        for (long participantId : included)
            for (Round round : selectedRounds)
                if (!answered.contains(new Key(participantId, round.id()))) return null;
        List<Long> excluded = List.copyOf(ordered.subList(headcount, ordered.size()));
        String error = selectedRounds.stream().anyMatch(round -> excluded.contains(round.payerId())) ? "REMOVE_PAYER" : null;
        return new Plan(included, excluded, error);
    }
}
