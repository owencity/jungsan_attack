# 보존 원문 체크섬 검증

기준 archive commit: `2d3a39b88a9b03ec4abecabd4f1cb6fcaf650872`. 전체 35개 파일을 Git blob 원문으로 읽어 SHA-256을 보존했다.

초기 manifest는 Windows 작업 파일 바이트로 계산해 Git의 줄바꿈 정규화와 2개 차이가 있었다: guide.md, progress.json. 원본을 덮어쓰지 않고 `archive-canonical-checksums.json`에 정규화된 커밋 원문의 체크섬을 추가했다. 코드·before/after·정책·카탈로그 원문은 변경하지 않았다.
