package app.jeongsan.server.quicksettlement.javaimpl;

import app.jeongsan.core.*;
import app.jeongsan.server.common.MalformedRequestException;
import app.jeongsan.server.quicksettlement.QuickSettlementValidationException;
import app.jeongsan.server.quicksettlement.QuickSettlementResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 학습용 입력·응답 어댑터. 금액 계산은 양 언어 모두 기존 core 진입점을 사용한다. */
public final class QuickSettlementCalculator {
    private QuickSettlementCalculator() {}

    public static QuickSettlementResponse calculate(long total, int headcount) {
        if (total < 1 || total > 10_000_000) {
            throw new MalformedRequestException("총액은 1원부터 1천만 원까지입니다.");
        }
        if (headcount < 2 || headcount > 50) {
            throw new MalformedRequestException("총 정산인원은 결제자를 포함해 2명부터 50명까지입니다.");
        }
        var people = new ArrayList<Participant>();
        var attendance = new LinkedHashMap<AttendanceKey, Attendance>();
        for (long id = 1; id <= headcount; id++) {
            people.add(new Participant(id, id == 1 ? "결제자" : "참여자"));
            attendance.put(new AttendanceKey(id, 1), Attendance.SOBER);
        }
        var input = new SettlementInput(people, List.of(new Round(1, 1, "간단 정산", total, 0, 1)), attendance, List.of());
        var outcome = Settlement.INSTANCE.settle(input);
        if (outcome instanceof SettlementOutcome.Failure failure) {
            throw new QuickSettlementValidationException(failure.getErrors());
        }
        var result = ((SettlementOutcome.Success) outcome).getResult();
        var recipient = result.getRecipients().get(1L);
        return new QuickSettlementResponse(result.getGrandTotal(), headcount, recipient.getParticipantAmounts().get(2L),
                recipient.getOwnShare(), recipient.getIncomingTotal());
    }
}
