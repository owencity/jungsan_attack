package app.jeongsan.core.javaimpl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import app.jeongsan.core.javaimpl.Model.*;

/** 저장 중인 미완성 입력과 확정 가능한 입력을 구분한다(CALC_RULES_V2 §5). */
public final class Validation {
    public static final long MAX_AMOUNT = 1_000_000_000_000L;
    private static final BigInteger MAX_AMOUNT_BIG = BigInteger.valueOf(MAX_AMOUNT);

    private Validation() {}

    public enum ValidationPhase { SAVE, CONFIRM }
    public enum ErrorCode {
        TOTAL_NOT_POSITIVE, ALCOHOL_NEGATIVE, ALCOHOL_EXCEEDS_TOTAL, PAYER_NOT_FOUND,
        INVALID_DRINK_ITEM, DRINK_ITEM_ROUND_NOT_FOUND, ATTENDANCE_REFERENCE_NOT_FOUND,
        MISSING_ATTENDANCE, DUPLICATE_ID, DUPLICATE_ROUND_SEQ, AMOUNT_TOO_LARGE,
        TOO_FEW_PARTICIPANTS, NO_ATTENDEE, NO_DRINKER_WITH_ALCOHOL, NEGATIVE_ADJUSTED_AMOUNT
    }

    public record ValidationError(ErrorCode code, String message, Long roundId, Long participantId) {}

    public static List<ValidationError> validate(SettlementInput input, ValidationPhase phase) {
        List<ValidationError> errors = new ArrayList<>();
        List<Round> rounds = input.effectiveRounds();
        Set<Long> ids = new LinkedHashSet<>();
        input.participants().forEach(p -> ids.add(p.id()));
        validateAlways(input, rounds, ids, errors);
        if (phase == ValidationPhase.CONFIRM) validateOnConfirm(input, rounds, errors);
        return List.copyOf(errors);
    }

    private static <T> Set<T> duplicatesOf(List<T> values) {
        Map<T, Integer> counts = new LinkedHashMap<>();
        values.forEach(value -> counts.merge(value, 1, Integer::sum));
        Set<T> duplicates = new LinkedHashSet<>();
        counts.forEach((value, count) -> { if (count > 1) duplicates.add(value); });
        return duplicates;
    }

    private static void validateAlways(SettlementInput input, List<Round> rounds, Set<Long> ids,
                                       List<ValidationError> errors) {
        duplicatesOf(input.participants().stream().map(Participant::id).toList()).forEach(id ->
                errors.add(new ValidationError(ErrorCode.DUPLICATE_ID,
                        "참여자 id가 중복되었습니다: " + id, null, id)));
        duplicatesOf(input.rounds().stream().map(Round::id).toList()).forEach(id ->
                errors.add(new ValidationError(ErrorCode.DUPLICATE_ID,
                        "차수 id가 중복되었습니다: " + id, id, null)));
        duplicatesOf(input.rounds().stream().map(Round::seq).toList()).forEach(seq ->
                errors.add(new ValidationError(ErrorCode.DUPLICATE_ROUND_SEQ,
                        "차수 순번이 중복되었습니다: " + seq + "차. 근거 화면의 차수 순서가 흔들립니다.", null, null)));

        Set<Long> roundIds = new LinkedHashSet<>();
        input.rounds().forEach(round -> roundIds.add(round.id()));
        for (DrinkItem item : input.drinkItems()) {
            if (item.bottleCount() <= 0 || item.unitPrice() <= 0) {
                errors.add(new ValidationError(ErrorCode.INVALID_DRINK_ITEM,
                        "'" + item.name() + "'의 병 수와 단가는 1 이상이어야 합니다. ("
                                + item.bottleCount() + "병 × " + item.unitPrice() + "원)", item.roundId(), null));
            } else {
                BigInteger itemAmount = BigInteger.valueOf(item.bottleCount())
                        .multiply(BigInteger.valueOf(item.unitPrice()));
                if (itemAmount.compareTo(MAX_AMOUNT_BIG) > 0) {
                    errors.add(new ValidationError(ErrorCode.AMOUNT_TOO_LARGE,
                            "'" + item.name() + "'의 술값이 너무 큽니다. 병 수와 단가를 확인해주세요.", item.roundId(), null));
                }
            }
            if (!roundIds.contains(item.roundId())) {
                errors.add(new ValidationError(ErrorCode.DRINK_ITEM_ROUND_NOT_FOUND,
                        "'" + item.name() + "'이 존재하지 않는 차수를 참조합니다.", item.roundId(), null));
            }
        }

        for (Round round : rounds) {
            if (round.total() <= 0) {
                errors.add(new ValidationError(ErrorCode.TOTAL_NOT_POSITIVE,
                        round.label() + "의 총액은 1원 이상이어야 합니다.", round.id(), null));
            }
            if (round.alcohol() < 0) {
                errors.add(new ValidationError(ErrorCode.ALCOHOL_NEGATIVE,
                        round.label() + "의 술값이 음수입니다.", round.id(), null));
            }
            if (round.alcohol() > round.total()) {
                errors.add(new ValidationError(ErrorCode.ALCOHOL_EXCEEDS_TOTAL,
                        round.label() + "의 술값(" + round.alcohol() + "원)이 총액(" + round.total()
                                + "원)보다 큽니다.", round.id(), null));
            }
            if (round.total() > MAX_AMOUNT) {
                errors.add(new ValidationError(ErrorCode.AMOUNT_TOO_LARGE,
                        round.label() + "의 총액이 너무 큽니다. 0을 하나 더 찍지 않았는지 확인해주세요.", round.id(), null));
            }
            if (round.alcohol() > MAX_AMOUNT) {
                errors.add(new ValidationError(ErrorCode.AMOUNT_TOO_LARGE,
                        round.label() + "의 술값이 너무 큽니다. 병 수와 단가를 확인해주세요.", round.id(), null));
            }
            if (!ids.contains(round.payerId())) {
                errors.add(new ValidationError(ErrorCode.PAYER_NOT_FOUND,
                        round.label() + "의 결제자가 참여자 목록에 없습니다.", round.id(), round.payerId()));
            }
        }

        for (AttendanceKey key : input.attendance().keySet()) {
            if (!ids.contains(key.participantId()) || !roundIds.contains(key.roundId())) {
                errors.add(new ValidationError(ErrorCode.ATTENDANCE_REFERENCE_NOT_FOUND,
                        "참여 응답이 존재하지 않는 참여자 또는 차수를 참조합니다.", key.roundId(), key.participantId()));
            }
        }
    }

    private static void validateOnConfirm(SettlementInput input, List<Round> rounds,
                                          List<ValidationError> errors) {
        // 기존 Kotlin과 같은 시점을 유지한다. SAVE 총합 상한의 명세 차이는 별도 검토 사항이다.
        BigInteger grandTotal = rounds.stream().map(round -> BigInteger.valueOf(round.total()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        if (grandTotal.compareTo(MAX_AMOUNT_BIG) > 0) {
            errors.add(new ValidationError(ErrorCode.AMOUNT_TOO_LARGE,
                    "전체 총액이 너무 큽니다. (" + grandTotal + "원)", null, null));
        }
        if (input.participants().size() < 2) {
            errors.add(new ValidationError(ErrorCode.TOO_FEW_PARTICIPANTS,
                    "참여자가 2명 이상이어야 정산할 수 있습니다.", null, null));
            return;
        }

        for (Round round : rounds) {
            for (Participant participant : input.participants()) {
                if (!input.attendance().containsKey(new AttendanceKey(participant.id(), round.id()))) {
                    errors.add(new ValidationError(ErrorCode.MISSING_ATTENDANCE,
                            round.label() + "에 '" + participant.name() + "'의 응답이 없습니다.", round.id(), participant.id()));
                }
            }
            boolean hasAttendee = input.participants().stream().anyMatch(participant -> {
                Attendance a = input.attendanceOf(participant.id(), round.id());
                return a.attended() && !a.exempt();
            });
            if (!hasAttendee) {
                errors.add(new ValidationError(ErrorCode.NO_ATTENDEE,
                        round.label() + "에 비용을 부담할 참석자가 없습니다.", round.id(), null));
                continue;
            }
            if (round.alcohol() > 0 && input.participants().stream()
                    .noneMatch(participant -> input.attendanceOf(participant.id(), round.id()).drank())) {
                errors.add(new ValidationError(ErrorCode.NO_DRINKER_WITH_ALCOHOL,
                        round.label() + "에 술값 " + round.alcohol() + "원이 있는데 술 마신 사람이 없습니다. "
                                + "술값을 0으로 하거나 음주자를 지정해주세요.", round.id(), null));
            }
        }
    }
}
