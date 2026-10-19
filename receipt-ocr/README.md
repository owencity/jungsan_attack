# 정산어택 영수증 추출 — Python 초기 구현

PaddleOCR CPU 한국어 모델로 총액·주류 후보를 읽고 총무 확인용 JSON을 반환한다.
별도 저장소로 옮길 수 있도록 이 폴더 안에 실행·사전·테스트를 모았다.
2026-10-14 사용자 승인으로 개발을 시작했다. 현재 Spring 연결·운영 배포 전이다.

사용자 결정과 후속 작업은 [작업 기억](MEMORY.md), 앱 촬영 화면 전달문은
[프론트 프롬프트](docs/frontend-capture-prompt.md)에 보존한다.

## 실행

Python 3.12. 모델을 받는 첫 실행에는 인터넷이 필요하며, 이후 로컬 모델 캐시를 사용한다.
아래 명령은 `receipt-ocr` 폴더에서 실행한다.

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements-test.txt
.venv\Scripts\python -m pip install -r requirements.txt
.venv\Scripts\python -m unittest discover -s tests -v
.venv\Scripts\python -m receipt_ocr --tokens examples/receipt-tokens.json
.venv\Scripts\python -m receipt_ocr --image .samples/receipt.jpg
```

Linux는 `.venv/bin/python`을 사용한다. 버전은 requirements에 고정했지만 전이 의존성은
완전 잠금 전이다. 운영 채택 전 깨끗한 환경·OCI ARM에서 설치, 모델 추론, 지연·메모리를 실측한다.
모델이 필요 없는 단위/API 테스트는 requirements-test만 설치해도 실행 가능하다.

API 실행은 Spring→Python 내부 통신용 서비스 키가 필요하다.
사용자 로그인 토큰과 별개이며 앱에 전달하지 않는다. 32자 이상의 임의 ASCII 비밀값을
`RECEIPT_SERVICE_KEY`에 설정한다. 사진·원문은 파일이나 DB에 저장하지 않는다.

```powershell
# RECEIPT_SERVICE_KEY를 로컬 환경에 설정한 뒤 실행한다.
.venv\Scripts\python -m uvicorn receipt_ocr.api:application --factory --host 127.0.0.1 --port 8091 --workers 1 --limit-concurrency 4 --no-access-log
```

- `POST /api/v1/receipts/extract`: JPEG/PNG **바이너리 본문**, `Content-Type: image/jpeg` 또는 `image/png`, `X-Receipt-Service-Key`.
- `GET /health`: 기동 완료 확인. 기동 시 모델 초기화가 실패하면 서버가 뜨지 않는다.
- 원본 8MiB 이하·1,600만 픽셀 이하. EXIF 방향 적용·긴 변 최대 2400px·메타데이터 제거.
- 프로세스당 한 장만 처리하고 겹친 요청은 503 `OCR_BUSY` + Retry-After 2초.
- 운영에서는 공인 포트를 열지 않고 Spring만 접근하게 연결한다. Spring이 사용자 인증·총무 권한·단위 상태·업로드 제한을 검사한다.
- Spring 호출 시간 제한과 사용자별 요청 제한은 연결 작업에서 구현한다. Python CPU 추론 중단은 HTTP 시간 제한만으로 보장되지 않는다.

## MD 사전 만들기

`data/drinks.md`를 수정하거나 같은 형식의 파일을 `--dictionary`로 지정한다.
API는 `RECEIPT_DICTIONARY_PATH`로 지정하고 재시작한다. 잘못된 분류·빈 사전·상충하는 중복은 기동/실행을 거절한다.

| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 | 제공 방식 |
|---|---|---|---|---|
| 참이슬 | 소주 | 참이슬 (세부형 미상) | 미상 | 미상 |
| TERRA | 맥주 | 테라 | 미상 | 미상 |
| 병맥주 | 맥주 | 맥주 (브랜드 미상) | 병 | 미상 |
| 생맥주 | 맥주 | 생맥주 (브랜드 미상) | 미상 | 생맥 |
| 카스 0.0 | 비주류 | 카스 0.0 | 미상 | 미상 |

종류는 **소주·맥주·막걸리·청주약주·와인·위스키증류주·혼합주·기타주류·비주류·비음료·미상**,
단위는 **병·캔·잔·피처·팩·미상**, 제공 방식은 **생맥·미상**이다.
제공 방식 칸을 생략한 4열 표도 읽지만, **브랜드만으로 병·캔·잔을 지정하면 실행을 거절한다.**
단위는 해당 표기 자체에 병·캔·잔 등의 근거가 있을 때만 지정한다. 브랜드만 있으면 미상이다.
생맥 제공 방식은 일반 생맥 메뉴 표기에만 지정한다. 브랜드의 생드래프트 제품명에서 추측하지 않는다.
팩트체크와 정책 설명은 [검토 문서](docs/receipt_alcohol_ocr_dictionary.md),
실행용 148개 표기는 [사전](data/drinks.md)에 분리했다. 문서 전체를 실행 사전으로 지정하지 않는다.
제품명·별칭은 한 행씩 쓰고 가격은 넣지 않는다. 별칭 매칭은 공백·대소문자·Unicode 폭을
정규화한 **전체 이름 일치**다. 부분일치·유사도·LLM으로 모르는 제품을 주류로 추측하지 않는다.
비주류·비음료 행은 OCR에서 읽혀도 주류 결과에서 제외한다. 무알코올 제품의 실제 표기를 따로 적는다.

## 지원 범위와 한계

- 품목 행을 좌표로 묶고 `단가 수량 금액`, `수량 금액`, `수량 할인 금액` 헤더 순서대로 읽는다. 순서는 달라도 된다.
- 단가 열이 없으면 수량과 항목 금액만 읽고 단가는 null이다. 할인 열의 0은 허용하며 할인 금액이 있으면 확인 사유를 붙인다.
- `생맥주(카스300cc)`는 일반 생맥 메뉴와 사전의 정확한 맥주 브랜드가 함께 있을 때만 조합한다. 임의 괄호·무알코올 표기는 버리지 않는다.
- 헤더가 없으면 가격 열은 읽지 않는다. `소주 2병`, `소주 수량 2`, `소주 x2`처럼 명시된 수량은 읽는다.
- 수량·가격이 빠지면 역산하지 않는다. 음수·소수·잘못된 쉼표는 숫자 전체를 검증해 거절한다.
- 상품명 주변의 명시된 용량·용기는 별도로 읽는다. `0.5L`는 500mL, `12,500ml`는 12,500mL이다.
- 브랜드와 수량만 있으면 `unit=UNKNOWN`, `bottleCount=null`이다. 제공 방식은 `servingType`으로 분리한다.
- 팩의 판매 수량은 보존하지만 병/캔 개수로 환산하지 않는다. 취소·증정·세트는 자동 판매 수량으로 채우지 않는다.
- 가격·수량·항목 금액 불일치와 낮은 신뢰도는 reviewReasons에 표시한다.
- 작은 수량·할인이 빠진 칸은 명시 헤더/금액 행 좌표로만 제한해 같은 모델로 다시 읽는다. 기존 숫자는 덮어쓰지 않는다.
  인접 숫자 행 중심으로 경계를 제한하고 공개 TextRecognition API로 위치 검출 없이 읽는다.
  한국어 가중치는 같지만 전체 OCR과 별도 인식 인스턴스를 두므로 추가 메모리와 운영 지연 실측이 필요하다.
  회색조·대비 보정·4배 확대, 여백 16/32px, 정수 인식 점수 0.90 이상과 고신뢰도 이견 검사를 사용한다.
  빈 칸·잘린 글자·겹친 글자는 건너뛰며 최대 8칸·16회만 추가 호출한다. 채택한 품목에 NUMERIC_REGION_RECOGNIZED를 표시한다.
- 잔·캔은 병 수 계약에 그대로 넣을 수 없어 `UNIT_REQUIRES_MAPPING`을 반환한다.
- 총액 라벨은 합계·합계금액·총액·총금액·결제금액·총결제금액·받을금액. 공급가액·부가세·받은 돈·거스름돈은 제외한다.
- 가로로 붙은 표의 수량·금액 헤더가 중복되면 `MULTIPLE_RECEIPTS_SUSPECTED`와 빈 후보를 반환한다. 모든 복수 영수증을 검출하는 기능은 아니다.
- 여러 총액 후보가 다르면 null + TOTAL_CONFLICT. 할인 전 합계/할인 후 결제액 중 하나를 임의 선택하지 않는다.
- 음식값 때문에 술값 합계와 총액이 같을 필요는 없다. 술값이 총액을 넘으면 모순으로 표시한다.
- 사진의 90도 회전은 EXIF 정보로만 보정한다. 사진 원근·접힘·희미한 인쇄·여러 줄 품목·무헤더 양식·누락/미등록 주류는 실제 샘플 개선 대상이다.
- 후보 합계·검증은 영수증 추출용 정수 연산이다. 참여자 부담·송금 정산은 기존 Kotlin/Java core에서만 수행한다.
- 반환하는 이름은 사전 표시 이름이다. 카드번호·승인번호·상호·OCR 원문은 응답에 포함하지 않는다.
- `requiresConfirmation`은 **항상 true**다. 경고가 없어도 누락이 없는 영수증이라는 보장은 아니다.

## 실제 영수증 평가

실제 사진과 `examples/manifest.example.json` 형식의 정답 목록을 `.samples/`에 두고 실행한다.
manifest의 image는 manifest 파일 기준 상대 경로다. 사진과 정답은 커밋하지 않는다.

```powershell
.venv\Scripts\python -m receipt_ocr.evaluate .samples/manifest.json
```

보고서는 총액 완전일치율, 모든 주류의 이름·분류·용기·제공 방식·단가·수량·항목 금액·용량·병 수 완전일치율(중복 포함), 실패 수,
경고 수, 모델 초기화를 제외한 평균 처리시간만 반환한다. 실패 표본도 정확도 분모에 포함한다.
다른 엔진도 같은 `recognize(image) -> list[Token]` 경계로 비교할 수 있다.
실제 사진 정확도·ARM 운영 성능은 합성 데이터 테스트와 구분해서 기록한다.

공개 웹 이미지로 발견한 양식·수정 전후·미지원 범위는 [공개 예시 검증](docs/public-sample-validation.md)에 기록한다.
작은 수량 누락의 후속 수정과 대조 테스트는 [숫자 영역 검증](docs/numeric-region-validation.md)을 본다.
추가 원본·실패 사례·원근 줄 묶기는 [확장 검증](docs/expanded-public-validation.md),
Spring 요청·응답·오류 의미와 연결 완료 기준은 [인계 자료](docs/spring-handoff.md)를 본다.
숫자 직접 인식과 인접 행 경계 후속은 [직접 인식 검증](docs/direct-recognition-validation.md)을 본다.
이는 작은 개발용 사례 점검이며 대표 표본의 정확도 평가가 아니다. 사용자의 직접 촬영 샘플은 아직 없다.

API 필드와 오류는 [계약 문서](../docs/RECEIPT_OCR.md)를 따른다.
