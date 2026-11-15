# 영수증 OCR 운영 실행 준비 — 2026-11-15

총무가 올린 영수증에서 총액과 술 항목을 읽고, 앱에서 고친 뒤 차수에 적용하게 한다.
Python은 후보만 반환한다. 정산 계산·참여자·차수 저장은 기존 Spring/core가 담당한다.

## 정확도와 현재 확인 범위

기존 [실제 사진 평가](round2-accuracy-hybrid-2026-11-04.md)를 그대로 보존했다.
32개 사진 중 정한 조건에 맞은 것은 14개, 맞지 않은 것은 18개다.
처음 본 매장 사진 3개는 주류 항목 조건을 모두 맞춘 사례가 없었다.
일반 영수증의 정확도나 자동 정산 가능성을 입증한 수치가 아니다.
`requiresConfirmation=true`를 유지하고, 총무가 원본과 후보를 확인한 뒤 적용해야 한다.
경고가 없거나 합성 영수증 테스트가 통과해도 항목이 빠지지 않았다는 뜻이 아니다.

기존 Windows x86 평가의 최고 RSS 약 1.29GiB와 처리 시간 약 3~6초는 ARM 운영 성능이 아니다.
이번 작업에서는 원본 branch의 사진·가중치·가상환경을 새 브랜치로 복사하지 않았다.
가중치는 공식 저장소에서 다시 받도록 빌드를 구성했다.

## 이미지 빌드와 모델 고정

빌드 위치는 저장소 루트 기준 `receipt-ocr`다.

```sh
docker build -t receipt-ocr:<commit> receipt-ocr
```

- Python 3.12, PaddleOCR 3.7.0, PaddlePaddle 3.3.1을 유지한다.
- PyPI에는 PaddlePaddle 3.3.1의 Linux ARM wheel이 없다. [공식 CPU 인덱스](https://www.paddlepaddle.org.cn/packages/stable/cpu/paddlepaddle/)에는
  `paddlepaddle-3.3.1-cp312-cp312-linux_aarch64.whl`이 있고, 해당 파일 응답이 HTTP 200임을 확인했다.
  Docker는 이 공식 인덱스에서 PaddlePaddle을 먼저 설치한다.
- 기존 평가에 사용한 PaddleX 3.7.2, NumPy 2.3.5, OpenCV 4.10.0.84도 constraints로 고정했다.
  모든 전이 의존성과 OS 패키지를 잠근 것은 아니므로 검증한 이미지 태그/다이제스트로 배포한다.
- `models.lock.json`은 기존에 평가한 두 모델의 Hugging Face commit과 6개 파일 SHA256을 고정한다.
  다운로드와 해시 확인은 이미지 빌드에서만 한다. 다운로드 실패·내용 변경은 빌드를 실패시킨다.
- API 시작 때 같은 파일을 다시 검증하고 Paddle의 공개 `model_dir` 설정으로 직접 읽는다.
  `RECEIPT_MODEL_DIR`가 없거나 파일이 변조됐으면 시작하지 않는다. 운영 중 다운로드로 보충하지 않는다.
- `.dockerignore`와 명시적인 `COPY`로 영수증 사진, 개인 설정, 개발 캐시, 가상환경을 이미지에서 제외한다.

## 서버에서 실행할 조건

아래는 초기 제한안이다. 운영 ARM 측정 후 지연·메모리·Spring 응답 영향을 보고 조정한다.

| 항목 | 조건과 이유 |
|---|---|
| CPU | 1 CPU 상당, 모델 스레드 1개. 총 2 CPU에서 Spring·DB가 사용할 시간을 남긴다. |
| 메모리 | 3GiB. 기존 x86 관측값에 여유를 둔 제안이며 ARM 통과를 보장하지 않는다. |
| 프로세스 | uvicorn worker 1개, 동시 추출 1개. 추가 요청은 `503 OCR_BUSY`로 즉시 거절한다. |
| 네트워크 | Spring만 연결된 내부 네트워크, 외부 게시 포트 없음. 실행 중 인터넷이 없어도 시작해야 한다. |
| 키 | Spring/Python 전용 32자 이상 무작위 ASCII 키. 앱·Git·로그에 넣지 않는다. |
| 파일 | 읽기 전용 루트, 임시 작업은 `/tmp`만. 사진·OCR 원문·결과를 파일이나 DB에 저장하지 않는다. |
| 종료·재시작 | `init: true`, `restart: unless-stopped` 필수. 감시 프로세스는 uvicorn만 종료한다. |

Linux PID 1은 일반 자식 프로세스가 보내는 강제 종료 신호를 무시할 수 있다.
따라서 Docker `--init` 또는 compose `init: true` 없이 API를 PID 1로 실행하면 시작을 거절한다.
전용 비특권 사용자로 실행하고 모든 capability를 제거한다.

`RECEIPT_REQUEST_TIMEOUT_SECONDS=40`은 사진 본문 수신부터 추론 완료까지 포함한다.
`RECEIPT_STARTUP_TIMEOUT_SECONDS=180`은 모델 초기화의 제한 시간이다.
Spring의 요청 제한 시간 45초보다 먼저 OCR 작업을 끝내거나 프로세스를 종료한다.
HTTP 연결 취소만으로 native CPU 작업이 중단되지는 않아 별도 감시 프로세스를 사용했다.
감시 대상에는 시각·단계만 보내며 사진·헤더·후보는 보내지 않는다.
시간을 넘기면 `RECEIPT_DEADLINE_EXCEEDED stage=request` 또는 `stage=startup`만 기록한다.
종료 중 요청은 실패하고 이미지 재업로드가 필요하다. 자동 무한 재시도는 하지 않는다.

## 검증과 남은 작업

`Receipt OCR CI`의 일반 테스트와 `arm-runtime`을 함께 통과시킨다.
ARM 작업은 Docker를 CPU 1개·3GiB·인터넷 차단 상태로 띄운다.
한글 글꼴로 직접 만든 가짜 영수증에서 실제 모델이 총액·주류를 읽는지 확인하고,
빈 이미지·손상 이미지·잘못된 서비스 키·동시 요청 거절과 이후 복구도 검사한다.
끝나지 않는 가짜 업로드를 보내 실제 40초 종료와 컨테이너 재시작을 확인한 뒤 같은 추론을 반복한다.
이 테스트에 실제 영수증, 카드 정보, 사용자 이름은 쓰지 않는다.

로컬에서 모델 고정 파일 6개의 공식 다운로드·SHA256 일치를 확인했다.
Windows CPU 1스레드로 합성 영수증의 총액 50,000원·술 항목 15,000원/3개와 확인 필수 여부를 확인했다.
ARM 실행은 [GitHub Actions 43676412714](https://github.com/owencity/jungsan_attack/actions/runs/43676412714)의
`arm-runtime` 작업에서 통과했다. 일반 테스트 173개와 ARM 이미지 빌드, 오프라인 기동,
합성 영수증·빈/손상 이미지·잘못된 키·동시 요청 거절, 40초 종료 및 재시작 후 동일 검사를 통과했다.
합성 영수증 첫 추론은 2.645초, 재시작 후 2.649초였다. 재시작 후 검사에서 컨테이너 최고 메모리는
726,409,216 bytes(약 692.8MiB), 종료 직전 사용량은 430.8MiB였고 OOM은 없었다.
이는 GitHub ARM runner의 합성 사진 측정이다. OCI 실사진 처리 시간·자원 여유·장기 부하는 아직 검증하지 않았다.
앱→Spring→Python 연결과 운영 반영 여부도 이 결과와 구분한다.
운영 반영 전 앱의 수동 확인·취소·잘못된 사진·재시도와 기존 차수 입력 보존을 추가 확인한다.

## 검증한 이미지 전달

수동 `Receipt OCR CI` 실행에서 `export_image=true`를 선택하면 단위 테스트와 ARM 실행 검증을
모두 통과한 이미지만 보관한다. `jeongsan-receipt:<실행 커밋 SHA>` 태그의 이미지가 실제 검사에
사용한 이미지 ID와 같은지 확인한 다음 `docker save`와 gzip으로 묶는다.
`receipt-ocr-arm64-<SHA>` artifact에는 압축 파일, SHA256, 이미지 ID와 소스 커밋이 들어가며 2일간 보관한다.
PR 검증 실행은 큰 배포 파일을 만들지 않는다. 수동 실행 때 지정한 ref의 SHA로 배포 기록을 남긴다.

OCI로 전달할 때는 압축 파일 SHA256을 먼저 확인하고 `docker load`한 이미지 ID가 기록과 같은지 대조한다.
배포 직전에 다시 빌드하면 검증한 바이너리와 달라질 수 있으므로 이 파일을 그대로 사용한다.
