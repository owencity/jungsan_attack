package app.jeongsan.server.gathering.javaimpl;

import java.util.Locale;

public final class NotificationText {
    private NotificationText() {}
    public static String money(long amount) { return String.format(Locale.US, "%,d", amount); }
    public static String transfer(String type, String actorName) {
        return switch (type) {
            case "SENT" -> actorName + "님이 보냈대요. 입금을 확인해주세요";
            case "CONFIRMED" -> actorName + "님이 입금을 확인했어요";
            case "NOT_RECEIVED" -> actorName + "님이 아직 입금을 확인 못 했대요";
            default -> throw new IllegalArgumentException("송금 알림 종류가 아닙니다.");
        };
    }
    public static String payoutRegistered(String payerName) {
        return payerName + "님이 계좌를 등록했어요. 이제 보낼 수 있어요";
    }
    public static String completed(boolean gatheringCompleted) {
        return gatheringCompleted ? "정산 완료! 7일 뒤 사라져요" : "담당 차수의 정산이 완료됐어요";
    }
    public static String removed(String gatheringTitle) {
        return gatheringTitle + "의 담당 정산에서 빠졌어요";
    }
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
