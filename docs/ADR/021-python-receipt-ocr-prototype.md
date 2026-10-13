# ADR-021 · 무료 한국어 영수증 OCR을 Python에서 독립 검증한다

**상태** 초기 검증 구현 승인(CTO, 2026-10-14) · 운영 채택·ARM 성능은 미검증

## 맥락

총무가 영수증 사진에서 총액과 주류 단가·수량을 미리 채우려 한다. 유료 호출을 사용할 수 없고
Spring 개발과 Python OCR 개발을 병행하려 한다. REQUIREMENTS §9는 사람 확인·샘플 평가를 요구한다.

## 결정

PaddleOCR 3.7.0의 PP-OCRv5 모바일 검출 + korean_PP-OCRv5_mobile_rec를 CPU로 실행한다.
Python 표준 라이브러리 추출 정책과 MD 사전을 분리하고 FastAPI로 내부 호출 경계를 만든다.
PaddlePaddle 3.3.1, FastAPI, Uvicorn, Pillow는 사용자가 승인한 조합의 실행·이미지 처리를 위한 의존성이다.
Java/Kotlin core·공개 Spring API·DB·운영 compose에는 이번 변경을 연결하지 않는다.

## 근거

PaddleOCR 한국어 모델은 한국어·영어·숫자를 지원하고 코드·해당 모델은 Apache-2.0으로 공개됐다.
CPU에서 검증할 수 있어 유료 API·GPU 없이 시작할 수 있다. EasyOCR는 같은 토큰 경계로 비교 가능하지만
실제 영수증 우열을 아직 측정하지 않았으므로 "가장 정확하다"고 확정하지 않는다.
OCR 텍스트만으로 주류·수량·합계를 알 수 없어 위치 기반 행 묶기와 명시된 열 순서가 별도로 필요하다.

- [공식 OCR 문서](https://www.paddleocr.ai/latest/en/version3.x/pipeline_usage/OCR.html)
- [한국어 모델·라이선스](https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec)
- [PaddleOCR 코드·라이선스](https://github.com/PaddlePaddle/PaddleOCR)

## 대가

Python 런타임과 모델 캐시를 관리해야 한다. MD 사전과 영수증 형식별 개선이 필요하다.
헤더 없는 표·할인·접힌 사진·미등록 주류는 수동 확인이 남는다. 최초 모델 다운로드는 인터넷이 필요하다.
OCI ARM 설치/메모리/지연 검증 전에는 운영 적용을 주장하지 않는다. Python 추가만으로 MSA·Kafka·Redis·k3s를 도입하지 않는다.

## 재검토 조건

실제 술집 영수증 50~100장으로 총액·주류 필드 완전일치율과 처리 지연·메모리를 측정한다.
ARM에서 설치/추론이 안 되거나 목표 품질이 부족하면 동일 경계의 다른 엔진과 비교한다.
사진 저장·외부 전송·비전 LLM·독립 서비스 확장은 추가 제품/운영 결정과 측정 근거를 남긴다.
