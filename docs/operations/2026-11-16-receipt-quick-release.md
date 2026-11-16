# 2026-11-16 간단 N빵·영수증 입력 배포 기록

서버와 웹 안내를 운영에 반영했다. Android 0.1.0(7) 업로드 파일은 준비됐고, iOS 0.1.0(21)은 Apple 필수 약관 오류로 TestFlight 업로드가 완료되지 않았다. 스터디용 PR은 병합하지 않았다.

## 구현한 흐름

로그인한 총무가 총금액과 본인 포함 인원을 입력하면 기존 core로 간단 N빵을 계산한다. 각자 보낼 금액, 결제자 본인 부담, 받을 합계를 구분하고 결과 이미지를 저장한다. 이 계산을 위해 회원·술자리·참석 명단을 만들거나 결과를 DB에 저장하지 않는다.

영수증은 촬영하거나 최대 5장을 선택해 순서대로 인식한다. 총무가 원본과 총액·술 항목을 대조하고 확인해야 입력에 반영된다. 상세 정산에서는 적용 전 차수를 바꿀 수 있고 같은 차수의 확인된 금액을 합산한다. 기존 값이 있으면 교체·추가를 직접 선택한다. 서버는 영수증 원본을 저장하지 않는다.

OCR로 차수 금액을 저장할 때는 `expectedInputRevision`을 함께 보내 다른 기기에서 먼저 저장한 값을 덮어쓰지 않게 한다. 생략·null 요청은 구버전 동작을 유지한다. 기존 core 계산, 총무의 직접 확정, 전체 인원과 차수별 인원, 확정된 송금 스냅샷은 유지한다.

## 실행 소스와 산출물

| 대상 | 검증·배포한 소스 |
|---|---|
| 서버 | `ca07bdc8d51027d7962a2f94798ed9949087c7a7` |
| OCR 이미지 | `2cdd5b955cb86f472a42ccdabd4e5069eb17df18` |
| 앱·iOS | `32eb060` — 직전 제품 코드 이후 변경은 iOS 테스트의 입력칸 선택 방식뿐 |
| Android 7 | `02564c0250faa00a122d68692f5033f247946c83` |
| 웹 안내 | `378889dfca514d58501b9fb4784ead31b8d2f8d7` |

- 서버 jar SHA-256: `fb754b6f14583bf821762b858d3e9ac9207b162cbc87b42d38869f479378fdd9`.
- OCR 전달 파일 SHA-256: `108d170e2ce5790e83bed03f04727bf5df9e4e7c1214a099960213eb4d1735bc`.
- Android AAB SHA-256: `e44112c40d1bd5e629430c975dc0f8b02ee412ff0e7b014d61f7afff18f9f930`.
- PR: [서버 #60](https://github.com/owencity/jungsan_attack/pull/60), [앱 #14](https://github.com/owencity/jungsan_app/pull/14), [웹 #6](https://github.com/owencity/profile/pull/6).

이 기록을 추가한 문서 커밋은 운영 실행 소스와 다르다. 다음 배포에서도 위 코드와 전체 인원·로그인 변경을 포함하는지 먼저 확인한다.

## 완료한 검증

| 검사 | 결과 |
|---|---|
| core·server | 59개·322개 통과, bootJar 성공 |
| Spring·실제 MySQL | [CI 43677297574](https://github.com/owencity/jungsan_attack/actions/runs/43677297574) 성공. 신규 31개 항목에 권한·입력·OCR 오류·동시 저장 충돌·확정 후 거절 포함. 기존 인증·비회원·스푼·알림·업그레이드 검사도 성공 |
| Python | 173개 통과 |
| ARM 실제 추론 | [CI 43676739903](https://github.com/owencity/jungsan_attack/actions/runs/43676739903) 성공. 인터넷 차단, CPU·메모리 제한, 실제 추론, 손상 사진, 동시 요청, 40초 중단·재기동 확인 |
| 앱 공통 | 287개 통과. Android 빌드·최적화 bundle 검사 성공 |
| iOS 화면 | [CI 43680090394](https://github.com/owencity/jungsan_app/actions/runs/43680090394) 성공. iPhone 8개·iPad 5개 시뮬레이터 검사. OCR 확인·취소, 이름 재입력, 10,000원·3명 계산 결과와 PNG 저장창 포함 |
| 배포 보완 | 운영 명령을 대신 실행하지 않는 로컬 검사 13개 통과. 경로·기존 Caddy 경로 보존·실패 복원·이미지 해시 연결 검사 |

앱 화면 검사는 개발용 예시로 수행했다. 실제 카메라 화질·사용자 계정·운영 영수증으로 촬영부터 적용까지 성공했다고 해석하지 않는다. Python의 기존 32장 평가에서 모든 조건이 일치한 사진은 14장이므로 원본 대조를 계속 필수로 둔다.

## 운영 적용 결과

배포 전 설정과 DB를 백업하고, 배포 잠금을 유지한 상태에서 내부 OCR을 먼저 기동했다. 새 OCR은 외부 포트가 없고 내부 네트워크에서만 접근한다. CPU 1개 상당·메모리 3GiB, 읽기 전용 파일 시스템으로 실행한다. Spring과 OCR의 내부 주소·키 일치 여부만 확인하고 비밀값은 출력하지 않았다.

- 운영 합성 영수증 추론: 총액 50,000원, 술값 15,000원·3개 확인, 4.432초. 이는 합성 사진 한 장의 결과이며 실제 영수증 전체의 정확도·처리 시간을 뜻하지 않는다.
- 서버 healthy, OCR running, OCR OOM 0·재기동 0 확인.
- 이전 Liquibase 이력·체크섬 보존, 신규 migration 없음.
- 배포 전 확정 정산·송금·근거 25개 중 변경·누락 0개. 모든 운영 방의 계산을 재실행한 것은 아니다.
- 이전 운영 이미지 `9880b58b59dc209e5ecab0e805931232cfaaf70c`를 보존했다. 실패 시 설정·이미지를 복원하며 운영 DB를 자동으로 과거 덤프로 되돌리지 않는다.
- Caddy는 영수증 두 경로의 응답 대기만 55초로 설정했다. 기존 공개 경로와 다른 서비스 연결은 유지했다.

2026-11-16 22:46 KST 공개 HTTP 검사 10개가 통과했다. health 200/UP, 기존 계정·술자리와 신규 N빵·OCR의 비로그인 요청 401, 운영 웹 CORS 허용, 임의 Origin 403을 확인했다. 실제 사용자 데이터나 계정으로 정산 저장을 수행하지 않았다. 인증된 추출·저장 성공과 동시 요청은 격리된 CI DB에서 검증했다.

운영 준비 중 이미지 식별자가 다르게 보여 적용을 중단한 적이 있다. CI의 config digest는 `sha256:5abe99165e0d76e625974b1456a48f74612de138b0319eaecc201a79589abe1f`, OCI Docker의 manifest digest는 `sha256:4735dfaca965d8764b07a0c1040408b03844be16742a2820d14c18dc5c932147`이다. 동일 아카이브의 index → manifest → config와 30개 blob·14개 layer를 대조해 관계를 확인한 뒤 두 값을 명시적으로 검사했다. 식별자 불일치를 무시한 것은 아니다. Docker 저장 방식에 따른 구분은 [containerd 공식 명령 문서](https://github.com/containerd/nerdctl/blob/main/docs/command-reference.md)와 [Docker 이미지 형식](https://github.com/moby/docker-image-spec/blob/main/spec.md)을 참고했다.

로컬 비밀값 없는 실행 결과는 `build/ocr-release/deployment-result.json`과 `public-check-result.json`에 보관한다. DB 덤프와 비밀 설정은 OCI의 접근 제한된 백업 폴더에만 둔다.

## 웹·스토어 배포 상태와 다음 확인

- 웹: 사용자 본인 확인 후 [Vercel 997K8PjwKc76HGpFfTmM6un1X195](https://vercel.com/kdks-projects-8235f941/jungsan/997K8PjwKc76HGpFfTmM6un1X195)가 Production / Ready와 운영 도메인 연결을 표시했다. 공개 개인정보 페이지가 새 안내를 포함하는 것도 확인했다.
- Android: `build/releases/android/2026-11-15-receipt-ocr/jungsan-0.1.0-7.aab`. 기존 버전 6과 같은 업로드 인증서, R8 최적화, bundletool·서명·64비트 네이티브 16KB 정렬 검사를 통과했다. Google Play 업로드·출시는 아직 수행하지 않았다.
- iOS: [빌드 21 작업 43713607477](https://github.com/owencity/jungsan_app/actions/runs/43713607477). 최초 Gradle 다운로드 DNS 오류 후 같은 작업을 재실행해 Release archive는 성공했다. Apple export/upload에서 `403 FORBIDDEN.REQUIRED_AGREEMENTS_MISSING_OR_EXPIRED`로 거절돼 TestFlight 업로드는 미완료다. 계정 소유자의 약관 확인·동의가 끝나면 같은 실패 작업을 재실행한다.
- 실제 기기: 새 앱에서 촬영·사진 여러 장 선택·금액 정정·차수 이동·중복 적용 방지·이미지 저장을 확인해야 한다. 자동 검사나 서버 합성 사진 검사로 이 확인을 대신하지 않는다.
