package app.jeongsan.core.javaimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

/** Java에서도 정확한 Core 결과로부터 표시 근거를 독립 변환한다. */
public final class SettlementPresentation {
    private SettlementPresentation() {}
    public record Basis(long roundId, String type, long amount) {}
    public static List<Basis> basis(Model.SettlementInput input, SettlementResult result, SettlementResult.Transfer transfer) {
        var ids = new HashSet<Long>();
        for (var round : input.rounds()) if (round.payerId() == transfer.toId()) ids.add(round.id());
        var values = new ArrayList<Basis>();
        long sum = 0;
        for (var round : result.breakdown().get(transfer.fromId()).rounds()) {
            if (!ids.contains(round.roundId())) continue;
            String type = round.exempt() ? "EXEMPT" : !round.attended() ? "ABSENT" : round.drank() ? "DRANK" : "SOBER";
            long amount = round.amount().numerator().divide(round.amount().denominator()).longValueExact();
            values.add(new Basis(round.roundId(), type, amount)); sum += amount;
        }
        if (!values.isEmpty()) {
            int index = values.size() - 1;
            var last = values.get(index);
            values.set(index, new Basis(last.roundId(), last.type(), last.amount() + transfer.amount() - sum));
        }
        return List.copyOf(values);
    }
}
