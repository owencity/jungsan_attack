package app.jeongsan.core.javaimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kotlin을 호출하지 않는 표시 변환. 정수 셀과 수취인별 조정을 독립적으로 만든다. */
public final class SettlementTable {
    private SettlementTable() {}
    public record Line(long participantId, String name, long total, long paidTotal) {}
    public record Cell(long participantId, String type, long food, long alcohol, long amount) {}
    public record RoundRow(long roundId, int seq, long payerParticipantId, long total, long foodTotal,
                           long alcoholTotal, long displayTotal, List<Cell> cells) {
        public RoundRow { cells=List.copyOf(cells); }
    }
    public record Adjustment(long recipientParticipantId, Map<Long,Long> cells) {
        public Adjustment { cells=Collections.unmodifiableMap(new LinkedHashMap<>(cells)); }
    }
    public record View(List<Line> lines, List<RoundRow> rounds, List<Adjustment> adjustments, long grandTotal) {
        public View { lines=List.copyOf(lines);rounds=List.copyOf(rounds);adjustments=List.copyOf(adjustments); }
    }

    public static View present(Model.SettlementInput input, SettlementResult result) {
        var participants=new ArrayList<>(input.participants());
        participants.sort(Comparator.comparingLong(Model.Participant::id));
        var rounds=new ArrayList<>(input.rounds());
        rounds.sort(Comparator.comparingInt(Model.Round::seq));
        var rows=new ArrayList<RoundRow>();
        for(var round:rounds) {
            var cells=new ArrayList<Cell>();
            long foodTotal=0, alcoholTotal=0, displayTotal=0;
            for(var participant:participants) {
                var breakdown=result.breakdown().get(participant.id()).rounds().stream().filter(item->item.roundId()==round.id()).findFirst().orElseThrow();
                foodTotal=breakdown.foodTotal();alcoholTotal=breakdown.alcoholTotal();
                long food=breakdown.foodShare().numerator().divide(breakdown.foodShare().denominator()).longValueExact();
                long alcohol=breakdown.alcoholShare().numerator().divide(breakdown.alcoholShare().denominator()).longValueExact();
                String type=breakdown.exempt()?"EXEMPT":!breakdown.attended()?"ABSENT":breakdown.drank()?"DRANK":"SOBER";
                long amount=Math.addExact(food,alcohol);
                cells.add(new Cell(participant.id(),type,food,alcohol,amount));displayTotal=Math.addExact(displayTotal,amount);
            }
            rows.add(new RoundRow(round.id(),round.seq(),round.payerId(),round.total(),foodTotal,alcoholTotal,displayTotal,cells));
        }
        var adjustments=new ArrayList<Adjustment>();
        var recipientIds=new ArrayList<>(result.recipients().keySet());
        recipientIds.sort(Long::compare);
        for(long recipientId:recipientIds) {
            var amounts=new LinkedHashMap<Long,Long>();
            for(var participant:participants) {
                long displayed=0;
                for(var row:rows) if(row.payerParticipantId()==recipientId)
                    for(var cell:row.cells()) if(cell.participantId()==participant.id()) displayed=Math.addExact(displayed,cell.amount());
                amounts.put(participant.id(),result.recipients().get(recipientId).participantAmounts().get(participant.id())-displayed);
            }
            if(amounts.values().stream().anyMatch(amount->amount!=0)) adjustments.add(new Adjustment(recipientId,amounts));
        }
        var lines=new ArrayList<Line>();
        for(var participant:participants) {
            var breakdown=result.breakdown().get(participant.id());
            lines.add(new Line(participant.id(),participant.name(),breakdown.finalAmount(),breakdown.paidTotal()));
        }
        return new View(lines,rows,adjustments,result.grandTotal());
    }

    public static long transferTotal(List<SettlementResult.Transfer> transfers) {
        long total=0;
        for(var transfer:transfers) total=Math.addExact(total,transfer.amount());
        return total;
    }

    public static boolean matches(List<SettlementResult.Transfer> transfers, List<SettlementResult.Transfer> stored) {
        var ordering=Comparator.comparingLong(SettlementResult.Transfer::toId).thenComparingLong(SettlementResult.Transfer::fromId);
        return transfers.stream().sorted(ordering).toList().equals(stored.stream().sorted(ordering).toList());
    }
}
