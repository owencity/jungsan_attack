package app.jeongsan.core.javaimpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import app.jeongsan.core.javaimpl.Model.*;
import app.jeongsan.core.javaimpl.SettlementResult.*;
import app.jeongsan.core.javaimpl.Validation.*;

/** Kotlin 구현을 호출하지 않는 독립적인 Core v2 계산기다. */
public final class Settlement {
    private Settlement() {}

    public static SettlementOutcome settle(SettlementInput input) {
        List<ValidationError> errors = Validation.validate(input, ValidationPhase.CONFIRM);
        if (!errors.isEmpty()) return new Failure(errors);
        SettlementResult result = compute(input);
        for (RecipientSettlement recipient : result.recipients().values()) {
            for (Map.Entry<Long, Long> entry : recipient.participantAmounts().entrySet()) {
                if (entry.getValue() < 0) {
                    return new Failure(List.of(new ValidationError(ErrorCode.NEGATIVE_ADJUSTED_AMOUNT,
                            "결제 금액이 참여 인원에 비해 너무 작아 잔액 조정자의 부담이 음수가 됩니다. ("
                                    + entry.getKey() + ": " + entry.getValue()
                                    + "원) 인원을 줄이거나 금액을 확인해주세요.", null, entry.getKey())));
                }
            }
        }
        return new Success(result);
    }

    private static SettlementResult compute(SettlementInput input) {
        List<Round> rounds = input.effectiveRounds();
        Map<Long, List<RoundBreakdown>> lines = new LinkedHashMap<>();
        input.participants().forEach(p -> lines.put(p.id(), new ArrayList<>()));
        for (Round round : rounds.stream().sorted(Comparator.comparingInt(Round::seq)).toList()) {
            Set<Long> attendees = input.participants().stream().filter(p -> {
                Attendance a = input.attendanceOf(p.id(), round.id());
                return a.attended() && !a.exempt();
            }).map(Participant::id).collect(Collectors.toSet());
            Set<Long> drinkers = input.participants().stream()
                    .filter(p -> input.attendanceOf(p.id(), round.id()).drank())
                    .map(Participant::id).collect(Collectors.toSet());
            long food = round.total() - round.alcohol();
            Rational foodShare = attendees.isEmpty() ? Rational.ZERO : Rational.of(food).divide(attendees.size());
            Rational alcoholShare = drinkers.isEmpty() ? Rational.ZERO : Rational.of(round.alcohol()).divide(drinkers.size());
            for (Participant p : input.participants()) {
                Attendance a = input.attendanceOf(p.id(), round.id());
                lines.get(p.id()).add(new RoundBreakdown(round.id(), round.seq(), round.label(),
                        a.attended(), a.drank(), a.exempt(), food, attendees.size(),
                        attendees.contains(p.id()) ? foodShare : Rational.ZERO, round.alcohol(), drinkers.size(),
                        drinkers.contains(p.id()) ? alcoholShare : Rational.ZERO));
            }
        }

        Map<Long, Long> paid = new LinkedHashMap<>();
        input.participants().forEach(p -> paid.put(p.id(), 0L));
        rounds.forEach(round -> paid.merge(round.payerId(), round.total(), Long::sum));
        Map<Long, List<Round>> byRecipient = rounds.stream()
                .collect(Collectors.groupingBy(Round::payerId, TreeMap::new, Collectors.toList()));
        Map<Long, RecipientSettlement> recipients = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Round>> group : byRecipient.entrySet()) {
            long recipientId = group.getKey();
            Set<Long> roundIds = group.getValue().stream().map(Round::id).collect(Collectors.toSet());
            Map<Long, Rational> raw = new LinkedHashMap<>();
            input.participants().forEach(p -> raw.put(p.id(), lines.get(p.id()).stream()
                    .filter(line -> roundIds.contains(line.roundId())).map(RoundBreakdown::amount)
                    .reduce(Rational.ZERO, Rational::plus)));
            long adjustmentId = recipientId;
            if (raw.get(recipientId).signum() <= 0) {
                adjustmentId = raw.entrySet().stream().filter(entry -> entry.getValue().signum() > 0)
                        .sorted(Map.Entry.<Long, Rational>comparingByValue().reversed()
                                .thenComparing(Map.Entry.comparingByKey()))
                        .findFirst().orElseThrow().getKey();
            }

            Map<Long, Long> amounts = new LinkedHashMap<>();
            for (Participant p : input.participants()) {
                amounts.put(p.id(), p.id() == adjustmentId ? 0L : raw.get(p.id()).ceilTo(1));
            }
            long paidTotal = group.getValue().stream().mapToLong(Round::total).sum();
            // 차수별 올림 대신 결제자별 원부담 합계를 한 번 올리고 잔액을 조정한다.
            amounts.put(adjustmentId, paidTotal - amounts.values().stream().mapToLong(Long::longValue).sum());
            long incomingTotal = amounts.entrySet().stream().filter(entry -> entry.getKey() != recipientId)
                    .mapToLong(Map.Entry::getValue).sum();
            recipients.put(recipientId, new RecipientSettlement(recipientId, paidTotal, amounts,
                    amounts.get(recipientId), incomingTotal, adjustmentId));
        }

        Map<Long, Long> amounts = new LinkedHashMap<>();
        Map<Long, ParticipantBreakdown> breakdown = new LinkedHashMap<>();
        for (Participant p : input.participants()) {
            long amount = recipients.values().stream().mapToLong(r -> r.participantAmounts().get(p.id())).sum();
            amounts.put(p.id(), amount);
            Rational rawTotal = lines.get(p.id()).stream().map(RoundBreakdown::amount).reduce(Rational.ZERO, Rational::plus);
            breakdown.put(p.id(), new ParticipantBreakdown(p.id(), p.name(), lines.get(p.id()), rawTotal, amount, paid.get(p.id())));
        }
        return new SettlementResult(amounts, breakdown, recipients, buildTransfers(recipients),
                rounds.stream().mapToLong(Round::total).sum());
    }

    /** 결제자 간 상계를 하지 않고 수취인 id, 송금자 id 순으로 보존한다. */
    private static List<Transfer> buildTransfers(Map<Long, RecipientSettlement> recipients) {
        List<Transfer> transfers = new ArrayList<>();
        for (RecipientSettlement recipient : recipients.values()) {
            recipient.participantAmounts().entrySet().stream()
                    .filter(entry -> entry.getKey() != recipient.recipientId() && entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> transfers.add(new Transfer(entry.getKey(), recipient.recipientId(), entry.getValue())));
        }
        return transfers;
    }
}
