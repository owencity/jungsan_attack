package app.jeongsan.server.gathering.javaimpl;

import java.util.Locale;

public final class NotificationText {
    private NotificationText() {}
    public static String money(long amount) { return String.format(Locale.US, "%,d", amount); }
    public static String title(String type) {
        return switch (type) {
            case "JOINED", "RESPONDED" -> "참여 응답이 도착했어요";
            case "SETTLED" -> "정산이 나왔어요! 입금액을 확인해주세요";
            case "SETTLED_HOST" -> "계산이 끝났어요";
            case "AUTO_RESPONDED" -> "자동응답으로 계산된 사람이 있어요";
            case "SENT" -> "입금을 확인해주세요";
            case "CONFIRMED" -> "입금이 확인됐어요";
            case "NOT_RECEIVED" -> "아직 입금이 확인되지 않았어요";
            case "COMPLETED" -> "정산 완료! 🎉";
            case "SETTLEMENT_REVERTED" -> "정산이 되돌려졌어요";
            case "PAYOUT_REGISTERED" -> "계좌가 등록됐어요";
            case "PAYOUT_MISSING" -> "받을 계좌를 등록해주세요";
            case "HEADCOUNT_EXCEEDED" -> "참여 인원을 확인해주세요";
            case "MEMBER_EXCLUDED", "REMOVED" -> "이번 정산에서 빠졌어요";
            default -> "술자리 소식이 있어요";
        };
    }
}
