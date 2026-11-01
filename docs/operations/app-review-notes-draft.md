# App Review Notes 초안 · FC-027/030/032/033 · 2026-11-01

**FC-033 빌드17용 초안이며 아직 제출하지 않았다.** 앱 PR6의 샘플 이미지 메뉴를 포함한 빌드17 업로드와 Android·iPhone·iPad CI가 완료됐다. Apple 처리 상태를 확인한 뒤 제출 빌드를 선택한다. 기능 정상 전제로 다음 개발을 진행하라는 CTO 결정은 항목별 실기기 시험 결과를 뜻하지 않는다. 실제 로그인·외부 데모 링크·계정 삭제의 확인 여부는 별도로 기록한다.

## App Review Information · Notes

The app provides an interactive demo without requiring social account credentials.

In normal use, the organizer uses the app. Participants open the organizer's shared mobile-web link, enter their name, and select attendance/drinking choices without creating an account. The organizer reviews the roster, confirms the settlement, and saves an image showing who should pay whom and how much. Drinks other than alcohol are included in the food amount.

Demo entry link (install the updated review build first):
https://jungsan-api.devkdk.com/api/v1/review/demo

Tap **Open** if iOS asks whether to open the link in Jeongsan.

You can also enter directly from the app:

1. On the login screen, tap **로그인 없이 데모 체험** (Try demo without signing in).
2. Enter a fictional display name of 2–10 characters, select the terms checkbox, and tap **이 이름으로 시작**. No real name or social account is required for the demo.
3. Use the top bar to switch between the organizer and participants A and B. These roles share the same fictional scenario state on the device.
4. In **체험 · 응답 중**, switch to participant B to submit an attendance/drinking response. Switch back to the organizer to review the roster and amount breakdown, then explicitly confirm the settlement. Participant responses alone do not confirm it.
5. In **체험 · 입금 확인 중**, participants can mark their fictional payment as sent; the organizer can confirm receipt. The app does not execute bank transfers, and demo account details are fictional.
6. You can also create a gathering, edit rounds, post messages, report or block another participant's message, and unblock a user from **내 계정**.
7. Tap **처음부터** to reset the demo or **체험 종료** to exit. Demo changes do not modify a real account or server data.
8. From the demo **내 술자리** screen, tap **샘플 정산표 보기·저장**. This opens a separate, fixed example with 25 fictional participants and a total of KRW 25,000. Tap **전체 정산표 이미지 저장** to open the actual iOS save/share interface. Both the screen and exported image are marked as a demo with no real payment. This example is independent of the gathering edited in the interactive demo.

The regular Kakao and Apple sign-in flows use **ASWebAuthenticationSession**, a system authentication interface presented within the app. The app exchanges the returned one-time ticket using PKCE.

For a real signed-in account, account deletion is available in **내 계정 → 회원 탈퇴**, below the bank account settings. A second tap confirms deletion. The existing product policy requires active settlements to be completed before deleting that account. **체험 데이터 삭제** in demo mode deletes only the fictional local demo state and is labeled accordingly.

The demo does not require a username/password. Its participant role switcher simulates multiple people on one device for review; normal participants use the mobile-web link. Social sign-in and real account deletion remain separate from the fictional demo.

## 제출 기록

- 최신 업로드 완료: 0.1.0 (17), 앱 a475264, 운영 API 연결. Release 41299620103 및 CI 41298532501 성공. [FC-033 실행 기록](fc033-release-2026-11-01.md).
- 앱 PR6 / FC-033 공개 샘플 이미지 메뉴는 빌드17에 포함됐다. 빌드16에는 없다. Apple 처리 상태를 확인하고 빌드17을 선택한 뒤 이 안내를 제출한다.
- 제출 빌드 번호: 아직 선택하지 않았다 — 실제 App Store Connect에서 선택한 번호를 기록한다.
- 운영 링크: 2026-11-01 HTTP 200·고정 데모 링크·no-store·CSP 존재 재확인 완료.
- 앱 데모: iOS 시뮬레이터 화면 테스트 통과. TestFlight 설치 빌드의 실제 iPhone 진입은 확인 전.
- iPhone 카카오/Apple 로그인·취소·재시도: 아직 실기기 확인 전.
- 실제 회원 탈퇴: 별도 실계정 확인 필요. 진행 중409 정책의 심사 수용 여부는 기존 후속이며 이 작업에서 임의 변경하지 않았다.
- App Review 재제출·승인: 미실행.
