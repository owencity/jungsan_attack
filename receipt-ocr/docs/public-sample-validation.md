# 공개 영수증 이미지 점검 · 2026-10-15

> 아래는 작은 숫자 영역 재인식 **이전**의 실패를 보존한 기록이다.
> 후속 구현·재검증은 [작은 숫자 검증](numeric-region-validation.md)을 본다. 이전 실패 결과를 지우지 않는다.

## 범위

사용자의 직접 촬영 사진이 없어 공개 웹 이미지 3개로 개발용 사례 점검을 했다.
검색 이미지의 자동 설명을 정답으로 쓰지 않고 원본을 눈으로 확인했다.
가려진 상품명은 복원하지 않았고 두 영수증을 한 거래로 합산하지 않았다.
이 사례를 보고 구현을 수정했으므로 독립 평가·대표 표본 정확도·운영 채택의 근거로 쓰지 않는다.

이미지·원문 HTML·OCR 토큰은 Git에 포함하지 않는다. 모델 학습·이미지 재배포도 하지 않는다.
로컬 `.samples/public-web`에는 검증용 파일, `reports/`에는 원문 없는 결과만 두었다.
운영 API가 업로드 사진이나 원문을 보관하는 기능을 추가한 것은 아니다.

## 출처와 수동 확인

| 사례 | 출처 | 확인 가능한 정답 | 평가 범위 |
|---|---|---|---|
| J · 상품명 가림 | [제주환경일보](https://www.newsje.com/news/articleView.html?idxno=268525) · [이미지](https://www.newsje.com/news/photo/202302/268525_258565_419.jpg) | 합계금액 59,000원 | 총액만. 가려진 이름·주종·주류 전체 정확도는 평가하지 않음 |
| N · 가로 복수 문서 | [뉴스타파](https://newstapa.org/article/wfzOH) · [이미지](https://d2kl0xuacqn1kv.cloudfront.net/2017/10/27225312/2017102705_01.jpg) | 서로 다른 두 영수증이 한 이미지에 있음. 총액 라벨 없음 | 복수 표 혼합 방지. 한 거래의 완전일치 평가에서 제외 |
| R · 생맥 메뉴 | [GeniusJW 방문 기록](https://geniusjw.com/2747) | 합계금액 18,500원, 생맥주(카스300cc) 판매 수량 1·할인 0·금액 2,000원. 단가 열 없음 | 총액과 주류 9개 필드 전체 비교 |

GeniusJW 이미지는 원문에서 연결한 영수증 사진이다. 이미지 주소의 만료 값이 바뀌므로
원문 페이지에서 현재 연결된 사진을 확인했다. 상품·수량·총액 외 개인정보는 평가 정답에 옮기지 않았다.
연합뉴스 등 검색에 나온 다른 사진은 이번 실행 표본에 포함하지 않았다.

검증한 원본의 SHA-256:

| 로컬 파일 | SHA-256 |
|---|---|
| jeju.jpg | `be6c7dd2f7bca02ad49bf2a0ba2a9ad77719406fe83763cb9c4fd7d819834d83` |
| two-receipts.jpg | `5174efb14651842a97f2cb61694565e37e7a45df2055f9c7eb25fd743fe201f4` |
| ramen.jpg | `c260098cfa4e8b9af5d364e3228952a6c7425c48b15db0e86e266c36bce87d8a` |

## 수정 전후

기준 코드는 `8269004`. PaddleOCR·인식 모델·사전·사진·이미지 제한을 유지하고 파서만 수정했다.
Windows x86_64 Python 3.12.6, CPU 2스레드, PP-OCRv5 mobile 검출·한국어 인식, 긴 변 2400px 제한이다.

| 사례 | 수정 전 | 수정 후 |
|---|---|---|
| J | total=null | total=59000. 가려진 주류는 여전히 빈 후보 |
| N | 양쪽 표가 한 행으로 묶임, 일부 맥주 후보만 남음 | total=null·drinks=[]·MULTIPLE_RECEIPTS_SUSPECTED |
| R | total=null·drinks=[] | total=18500, 카스·DRAFT·300mL 후보. 수량·단가·항목 금액은 null |

R의 실제 OCR은 작은 수량 `1`과 할인 `0`을 누락했다. 인쇄된 수량 1을 눈으로 볼 수 있어도
추출 코드는 이를 정답으로 채우지 않는다. ITEM_VALUES_MISSING으로 남기며 주류 전체 필드 일치는 **실패**다.
단가 없는 헤더와 모든 숫자가 OCR에 들어왔을 때 수량·금액을 읽는 정책은 회귀 테스트로 별도 검증했다.

추출 후 3개 이미지 처리시간은 각각 8.759초·5.082초·12.348초였다. 모델 초기화 제외, 각 1회다.
샘플/개발 PC 부하 차이가 있어 운영 지연·처리량 지표로 사용하지 않는다.

### 숫자 검출 비교

누락된 작은 숫자를 읽기 위해 공식 옵션을 두 가지 비교했다.

- 검출 박스 기준 0.6 → 0.4: R의 수량은 계속 누락, 처리시간 17.509초.
- 검출 긴 변 제한 1920px·max 명시: R의 할인 0 후보는 보였지만 수량 1은 계속 누락, 낮은 신뢰도 추가, 11.486초.

단건 실행이므로 옵션별 시간 우열은 판단하지 않는다. 수량이 복원되지 않아 **두 옵션 모두 채택하지 않았다.**
운영 엔진의 기본 검출 옵션·신뢰도 0.85·수량 역산 금지는 유지한다.
옵션 의미는 [PaddleOCR 공식 문서](https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html)를 대조했다.

## 재실행

완전한 정답 목록을 만들 수 있는 R만 기존 evaluator에 넣는다. J·N을 빼고 결과를 전체 사진 정확도로 표현하지 않는다.
`examples/manifest.public-web.example.json`의 정답은 인식 결과에 맞춰 수량을 null로 낮추지 않았다.
본인이 사용할 수 있는 원본을 로컬 `ramen.jpg`로 준비한 뒤 `receipt-ocr`에서 실행한다.

```powershell
Copy-Item examples/manifest.public-web.example.json .samples/public-web/manifest.json
$env:PADDLE_PDX_CACHE_HOME = "$PWD/.models/paddlex"
$env:HF_HOME = "$PWD/.models/huggingface"
.venv/Scripts/python.exe -m receipt_ocr.evaluate .samples/public-web/manifest.json
```

사진과 정답을 확보한 경로를 평가하는 명령이며 이미지를 자동 수집하거나 학습하지 않는다.
실행 결과: samples=1, failedSamples=0, totalExactRate=1.0,
allDrinkFieldsExactRate=0.0, warningSampleCount=1. **1장 개발 사례의 1/1·0/1**이며 일반화할 수 없다.
별도 evaluator 실행의 meanSecondsExcludingStartup=15.809초다. 위 사례 비교와 다른 단건 실행이다.

## 남은 작업

- 작은 수량 숫자가 OCR 검출에서 누락되는 문제: 표 헤더 좌표를 이용한 숫자 영역 재인식 등 별도 비교 필요.
- 새 사례와 수정에 쓰지 않은 직접 촬영 사진으로 수량·금액·비주류 오탐을 함께 검증한다.
- 미등록 상품·가림·기울기·여러 줄·모든 복수 문서 검출은 미완료다. 아직 소주/맥주 병 수 정확도를 주장하지 않는다.
- Spring/앱 연결·OCI ARM·운영 부하·공개 업로드 권한은 이번 검증의 범위 밖이다.

테스트 통과는 추출 정책이 지켜졌다는 근거이며 실제 사진에서 모든 숫자가 인식된다는 뜻이 아니다.
