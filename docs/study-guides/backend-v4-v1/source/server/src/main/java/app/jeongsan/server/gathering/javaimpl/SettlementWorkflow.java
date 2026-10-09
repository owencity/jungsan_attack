package app.jeongsan.server.gathering.javaimpl;

import app.jeongsan.core.javaimpl.*;
import app.jeongsan.server.common.ApiException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 운영 Bean과 분리한 Java 학습 구현. 입력·인가·hash·상태 전이는 Kotlin으로 위임하지 않는다. */
public final class SettlementWorkflow {
    private SettlementWorkflow() {}
    public record UnitInput(long id, long hostId, String state, long revision, List<Long> participantIds) {
        public UnitInput { participantIds = List.copyOf(participantIds); }
    }
    public record PersonInput(long id, long userId, String name) {}
    public record DrinkInput(String name, long unitPrice, int quantity) {}
    public record RoundInput(long id, long unitId, int seq, long total, long payerId, List<DrinkInput> drinks) {
        public RoundInput { drinks = List.copyOf(drinks); }
    }
    public record ResponseInput(long participantId, long roundId, String type, String source) {}
    public record PreviewLine(long participantId, long total, boolean auto, List<SettlementPresentation.Basis> rounds) {}
    public record PreviewTransfer(long from, long to, long amount, List<SettlementPresentation.Basis> basis) {}
    public record Preview(long settlementUnitId, long inputRevision, String inputHash, List<PreviewLine> lines,
                          List<PreviewTransfer> transfers, long grandTotal) {}
    private static ApiException error(String code, HttpStatus status) { return new ApiException(code,status,code); }
    public static void host(UnitInput unit, PersonInput person) {
        if (unit.hostId()!=person.id()) throw error("NOT_SETTLEMENT_UNIT_HOST",HttpStatus.FORBIDDEN);
    }
    public static void open(UnitInput unit) { if (!unit.state().equals("OPEN")) throw error("SETTLEMENT_UNIT_NOT_OPEN",HttpStatus.CONFLICT); }
    public static void member(UnitInput unit, long id) {
        if (!unit.participantIds().contains(id)) throw error("NOT_SETTLEMENT_UNIT_MEMBER",HttpStatus.FORBIDDEN);
    }
    public static void remove(UnitInput unit, long id, List<RoundInput> rounds) {
        open(unit);
        if (id==unit.hostId()) throw error("REMOVE_HOST",HttpStatus.CONFLICT);
        if (rounds.stream().anyMatch(r->r.unitId()==unit.id()&&r.payerId()==id)) throw error("REMOVE_PAYER",HttpStatus.CONFLICT);
    }
    public static void response(UnitInput unit,long personId,long roundId,String type,List<RoundInput> rounds) {
        open(unit); member(unit,personId);
        if (rounds.stream().noneMatch(r->r.id()==roundId&&r.unitId()==unit.id())) throw error("ROUND_NOT_FOUND",HttpStatus.NOT_FOUND);
        if (!Set.of("ABSENT","SOBER","DRANK").contains(type)) throw error("MALFORMED_REQUEST",HttpStatus.BAD_REQUEST);
    }
    public static Model.SettlementInput input(UnitInput unit,List<PersonInput> people,List<RoundInput> rounds,List<ResponseInput> responses) {
        var persons=people.stream().filter(p->unit.participantIds().contains(p.id())).sorted(Comparator.comparingLong(PersonInput::id)).toList();
        var selected=rounds.stream().filter(r->r.unitId()==unit.id()).sorted(Comparator.comparingInt(RoundInput::seq)).toList();
        if (selected.isEmpty()) throw error("NO_ROUNDS",HttpStatus.CONFLICT);
        var answers=new LinkedHashMap<Model.AttendanceKey,Model.Attendance>();
        for(var p:persons) for(var r:selected) {
            String type=responses.stream().filter(a->a.participantId()==p.id()&&a.roundId()==r.id()).map(ResponseInput::type).findFirst().orElse("DRANK");
            answers.put(new Model.AttendanceKey(p.id(),r.id()),Model.Attendance.valueOf(type));
        }
        var drinks=new ArrayList<Model.DrinkItem>();
        for(var r:selected) for(var d:r.drinks()) drinks.add(new Model.DrinkItem(r.id(),d.name(),d.quantity(),d.unitPrice()));
        return new Model.SettlementInput(persons.stream().map(p->new Model.Participant(p.id(),p.name())).toList(),
            selected.stream().map(r->new Model.Round(r.id(),r.seq(),r.seq()+"차",r.total(),0,r.payerId())).toList(),answers,drinks);
    }
    private static void add(StringBuilder builder,Object value) { String text=value.toString(); builder.append(text.length()).append(':').append(text); }
    public static String hash(UnitInput unit,Model.SettlementInput input) {
        var b=new StringBuilder(); add(b,"v4");add(b,unit.id());add(b,"participants");add(b,input.participants().size());
        input.participants().stream().sorted(Comparator.comparingLong(Model.Participant::id)).forEach(p->add(b,p.id()));
        add(b,"rounds");add(b,input.rounds().size());
        input.rounds().stream().sorted(Comparator.comparingLong(Model.Round::id)).forEach(r->{add(b,r.id());add(b,r.seq());add(b,r.total());add(b,r.alcohol());add(b,r.payerId());});
        add(b,"attendance");add(b,input.attendance().size());
        input.attendance().entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<Model.AttendanceKey,Model.Attendance> e)->e.getKey().roundId())
            .thenComparingLong(e->e.getKey().participantId())).forEach(e->{add(b,e.getKey().roundId());add(b,e.getKey().participantId());add(b,e.getValue().name());});
        add(b,"drinks");add(b,input.drinkItems().size());
        input.drinkItems().stream().sorted(Comparator.comparingLong(Model.DrinkItem::roundId).thenComparing(Model.DrinkItem::name)
            .thenComparingInt(Model.DrinkItem::bottleCount).thenComparingLong(Model.DrinkItem::unitPrice))
            .forEach(d->{add(b,d.roundId());add(b,d.name());add(b,d.bottleCount());add(b,d.unitPrice());});
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static Preview preview(UnitInput unit,List<PersonInput> people,List<RoundInput> rounds,List<ResponseInput> responses) {
        var input=input(unit,people,rounds,responses);
        var outcome=Settlement.settle(input);
        if(outcome instanceof SettlementResult.Failure f) {
            var errors=f.errors().stream().map(e->new app.jeongsan.core.ValidationError(app.jeongsan.core.ErrorCode.valueOf(e.code().name()),e.message(),e.roundId(),e.participantId())).toList();
            throw new app.jeongsan.server.gathering.SettlementValidationException(errors);
        }
        var result=((SettlementResult.Success)outcome).result();
        var lines=new ArrayList<PreviewLine>();
        for(var breakdown:result.breakdown().values()) {
            boolean auto=rounds.stream().filter(r->r.unitId()==unit.id()).anyMatch(r->responses.stream().noneMatch(a->a.participantId()==breakdown.participantId()&&a.roundId()==r.id()));
            var items=breakdown.rounds().stream().map(r->new SettlementPresentation.Basis(r.roundId(),
                input.attendance().get(new Model.AttendanceKey(breakdown.participantId(),r.roundId())).name(),
                r.amount().numerator().divide(r.amount().denominator()).longValueExact())).toList();
            lines.add(new PreviewLine(breakdown.participantId(),breakdown.finalAmount(),auto,items));
        }
        var transfers=result.transfers().stream().map(t->new PreviewTransfer(t.fromId(),t.toId(),t.amount(),SettlementPresentation.basis(input,result,t))).toList();
        return new Preview(unit.id(),unit.revision(),hash(unit,input),lines,transfers,result.grandTotal());
    }
    public static List<Long> creationRoster(List<Long> ids,long host) {
        var sorted=ids.stream().distinct().sorted().toList();
        if(sorted.size()!=ids.size() || !sorted.contains(host)) throw error("MALFORMED_REQUEST",HttpStatus.BAD_REQUEST);
        return sorted;
    }
    private static String trim(String text) {
        int start=0,end=text.length();
        while(start<end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++;
        while(end>start && (Character.isWhitespace(text.charAt(end-1)) || Character.isSpaceChar(text.charAt(end-1)))) end--;
        return text.substring(start,end);
    }
    public static Map<String,String> payout(String bank,String account,String holder) {
        var banks=Set.of("카카오뱅크","토스뱅크","국민","신한","우리","하나","농협","기업","케이뱅크","SC제일","새마을금고","우체국","수협","신협","부산","대구");
        String digits=account.replace("-","");String name=trim(holder);
        if(!banks.contains(bank) || !account.matches("[0-9-]+") || !digits.matches("[0-9]{8,16}") || name.codePointCount(0,name.length())<1 || name.codePointCount(0,name.length())>20)
            throw error("MALFORMED_REQUEST",HttpStatus.BAD_REQUEST);
        var result=new LinkedHashMap<String,String>();result.put("bank",bank);result.put("accountNo",digits);result.put("holder",name);return Collections.unmodifiableMap(result);
    }
    public static boolean expired(List<String> states,java.time.Instant due,java.time.Instant last,java.time.Instant now) {
        return (states.stream().allMatch(s->s.equals("COMPLETED")) && due!=null && !due.isAfter(now)) || !last.isAfter(now.minusSeconds(30*86400L));
    }
    public static String summary(List<String> states) { return states.contains("OPEN")?"OPEN":states.contains("SETTLING")?"SETTLING":"COMPLETED"; }
    public static String transfer(String state,String action,long actorId,long sender,long recipient,String unitState,boolean hasPayout) {
        if(actorId!=(action.equals("sent")?sender:recipient)) throw error("NOT_TRANSFER_OWNER",HttpStatus.FORBIDDEN);
        String target=switch(action) {case "sent"->"SENT";case "confirm"->"CONFIRMED";case "not-received"->"WAITING";default->throw error("MALFORMED_REQUEST",HttpStatus.CONFLICT);};
        if(state.equals(target)||(state.equals("CONFIRMED")&&action.equals("sent"))) return state;
        if(!unitState.equals("SETTLING")) throw error("SETTLEMENT_UNIT_NOT_SETTLING",HttpStatus.CONFLICT);
        if(action.equals("sent")&&!hasPayout) throw error("PAYOUT_MISSING",HttpStatus.CONFLICT);
        if((action.equals("not-received")&&!state.equals("SENT"))||state.equals("CONFIRMED")) throw error("INVALID_TRANSFER_TRANSITION",HttpStatus.CONFLICT);
        return target;
    }
}
