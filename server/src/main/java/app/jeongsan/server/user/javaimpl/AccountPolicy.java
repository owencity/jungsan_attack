package app.jeongsan.server.user.javaimpl;

import app.jeongsan.server.common.ApiException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Set;

public final class AccountPolicy {
    private AccountPolicy() {}
    public static void deletable(List<String> states) {
        var active=Set.of("OPEN","SETTLING","COLLECTING","CONFIRMED");
        if(states.stream().anyMatch(active::contains))
            throw new ApiException("ACTIVE_GATHERING_EXISTS",HttpStatus.CONFLICT,"진행 중인 정산이 끝난 뒤 탈퇴할 수 있어요");
        if(states.stream().anyMatch(s -> !"COMPLETED".equals(s)))
            throw new ApiException("ACCOUNT_STATE_CHANGED",HttpStatus.CONFLICT,"계정 상태를 확인한 뒤 다시 시도해 주세요.");
    }
    public static void unchanged(List<Long> before, List<Long> after) {
        if(!before.equals(after)) throw new ApiException("ACCOUNT_STATE_CHANGED",HttpStatus.CONFLICT,"계정 상태가 바뀌었어요. 다시 시도해 주세요.");
    }
}
