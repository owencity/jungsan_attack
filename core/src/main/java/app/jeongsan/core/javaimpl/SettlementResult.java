package app.jeongsan.core.javaimpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import app.jeongsan.core.javaimpl.Validation.ValidationError;

/** 근거와 송금 지시까지 비교하기 위해 Kotlin 결과의 모든 필드를 유지한다. */
public record SettlementResult(Map<Long, Long> amounts, Map<Long, ParticipantBreakdown> breakdown,
                               Map<Long, RecipientSettlement> recipients, List<Transfer> transfers,
                               long grandTotal) {
    public SettlementResult {
        amounts = freeze(amounts);
        breakdown = freeze(breakdown);
        recipients = freeze(recipients);
        transfers = List.copyOf(transfers);
    }

    public sealed interface SettlementOutcome permits Success, Failure {}
    public record Success(SettlementResult result) implements SettlementOutcome {}
    public record Failure(List<ValidationError> errors) implements SettlementOutcome {
        public Failure { errors = List.copyOf(errors); }
    }

    public record ParticipantBreakdown(long participantId, String name, List<RoundBreakdown> rounds,
                                       Rational rawTotal, long finalAmount, long paidTotal) {
        public ParticipantBreakdown { rounds = List.copyOf(rounds); }
    }

    public record RecipientSettlement(long recipientId, long paidTotal, Map<Long, Long> participantAmounts,
                                      long ownShare, long incomingTotal, long adjustmentParticipantId) {
        public RecipientSettlement { participantAmounts = freeze(participantAmounts); }
    }

    public record RoundBreakdown(long roundId, int seq, String label, boolean attended, boolean drank,
                                 boolean exempt, long foodTotal, int attendeeCount, Rational foodShare,
                                 long alcoholTotal, int drinkerCount, Rational alcoholShare) {
        public Rational amount() { return foodShare.plus(alcoholShare); }
    }

    public record Transfer(long fromId, long toId, long amount) {}

    private static <K, V> Map<K, V> freeze(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
