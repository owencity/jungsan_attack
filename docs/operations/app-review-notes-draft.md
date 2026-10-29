# FC-027 App Review Notes 초안

**제출 전용 초안이다.** 백엔드 PR48 배포, 앱 PR1의 새 TestFlight 빌드 설치, 실제 iPhone 로그인·데모·링크·탈퇴 확인 후 해당 빌드를 선택하고 아래 내용을 App Store Connect에 입력한다. 아직 배포되지 않은 빌드나 동작하지 않는 링크를 준비됐다고 제출하지 않는다. 개발·검증 현황은 각 PR의 최신 기록을 따른다.

## App Review Information · Notes

The app provides an interactive demo without requiring social account credentials.

Demo entry link (install the updated review build first):
https://jungsan-api.devkdk.com/api/v1/review/demo

Tap **Open** if iOS asks whether to open the link in Jeongsan.

You can also enter directly from the app:

1. On the login screen, tap **로그인 없이 데모 체험** (Try demo without signing in).
2. Enter a fictional display name of 2–10 characters, select the terms checkbox, and tap **이 이름으로 시작**. No real name or social account is required for the demo.
3. Use the top bar to switch between the organizer and participants A and B. These roles share the same fictional scenario state on the device.
4. In **체험 · 응답 중**, switch to participant B to submit an attendance/drinking response. Switch back to the organizer to inspect the settlement and amount breakdown.
5. In **체험 · 입금 확인 중**, participants can mark their fictional payment as sent; the organizer can confirm receipt. The app does not execute bank transfers, and demo account details are fictional.
6. You can also create a gathering, edit rounds, post messages, report or block another participant's message, and unblock a user from **내 계정**.
7. Tap **처음부터** to reset the demo or **체험 종료** to exit. Demo changes do not modify a real account or server data.

The regular Kakao and Apple sign-in flows use **ASWebAuthenticationSession**, a system authentication interface presented within the app. The app exchanges the returned one-time ticket using PKCE.

For a real signed-in account, account deletion is available in **내 계정 → 회원 탈퇴**, below the bank account settings. A second tap confirms deletion. The existing product policy requires active settlements to be completed before deleting that account. **체험 데이터 삭제** in demo mode deletes only the fictional local demo state and is labeled accordingly.

The demo does not require a username/password. It exposes the organizer and participant workflows through explicit role switching. Social sign-in and real account deletion remain separate from the fictional demo.

## 제출 기록

- 업로드 빌드: 0.1.0 (9), 앱2ed2523, 운영 API 연결. 약관 동의 후 attempt2에서 Apple 업로드 성공. Apple 처리 완료·TestFlight 설치 확인은 남아 있다.
- 제출 빌드 번호: 아직 선택하지 않았다 — 실제 App Store Connect에서 선택한 번호를 기록한다.
- 운영 링크: 서버0d0df31에서200·고정 데모 링크·보안 헤더 확인 완료.
- 앱 데모: iOS 시뮬레이터 화면 테스트 통과. TestFlight 설치 빌드의 실제 iPhone 진입은 확인 전.
- iPhone 카카오/Apple 로그인·취소·재시도: 아직 실기기 확인 전.
- 실제 회원 탈퇴: 별도 실계정 확인 필요. 진행 중409 정책의 심사 수용 여부는 기존 후속이며 이 작업에서 임의 변경하지 않았다.
- App Review 재제출·승인: 미실행.
