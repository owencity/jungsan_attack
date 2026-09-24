# n8n 자동 PR 코드리뷰 파이프라인

`.claude/skills/jeongsan-review/SKILL.md`(로컬 Claude Code용 코드리뷰
페르소나)를 서버에서 자동으로 돌리기 위한 n8n 워크플로다. 멘토 숙제
2단계("n8n 서버에서 작동하는 자동화 워크플로 코드리뷰")의 산출물이며,
아직 설계 단계다 — 이 폴더는 그 설계를 코드/워크플로로 옮기는 자리다.

## 지금까지 정한 것

- **트리거**: 개인 GitHub 저장소 PR 웹훅(opened·synchronize). n8n
  Webhook 노드에서 `X-Hub-Signature-256` 검증 필수.
- **엔진**: 초기엔 Gemini API 무료 티어. 나중에 Claude로 교체 가능하게
  설계한다 — 교체 시 "API 호출" 노드 하나만 바뀌고 나머지(컨텍스트
  수집·프롬프트 조립·구조화 출력 파싱·Slack/PR 코멘트 포스팅)는 그대로
  가도록 인터페이스를 스키마 기준으로 고정한다.
- **컨텍스트 수집 방식**: Gemini CLI 같은 에이전트가 저장소를 직접
  열어 파일을 찾아 읽는 방식(Agentic Retrieval) 대신, n8n이 PR이 건드린
  경로를 보고 필요한 문서를 미리 선별해 프롬프트에 첨부하는 **단일 호출
  구조**로 시작한다 — `jeongsan-review` SKILL.md의 "검토 전에 반드시
  읽는다" 목록(AGENTS.md·ARCHITECTURE.md·ADR 인덱스 + PR 영역별
  REQUIREMENTS/CALC_RULES_V2/DOMAIN_DB_DESIGN_V2)을 그대로 옮긴다.
  현재 규모에서 가장 단순한 선택이고, 실제 한계(문서 과다·diff 과대·
  품질 저하)가 관찰되면 그때 Agentic Retrieval을 추가한다.
- **구조화 출력**: 자유 텍스트가 아니라 JSON 스키마로 강제한다 —
  `file`·`line`·`summary`·`failure_scenario`·`severity`·
  `recommended_fix`. Gemini는 `generationConfig.responseSchema` +
  `responseMimeType: application/json`으로 강제하고, Claude로 옮길 때도
  같은 스키마를 그대로 쓴다.
- **결과 처리**: Slack 채널에 요약 포스팅 + (선택) GitHub PR 코멘트 등록.
- **비용**: 자동화·서버 트리거는 구독 로그인이 아니라 **API 키**로만
  인증한다(Anthropic·OpenAI 모두 구독 자동화 라우팅을 약관으로 금지).
  Gemini 무료 티어는 제출 콘텐츠가 학습에 쓰일 수 있다는 조건을 알고
  쓴다.

## 아직 안 정한 것

- n8n 호스팅 위치(OCI 단일 VM에 컨테이너 추가 vs 별도)
- 리뷰 결과를 PR 코멘트로도 남길지, Slack만으로 충분할지
- Agentic Retrieval로 넘어갈 구체적 트리거 조건의 수치화

## 파일 배치(예정)

```
automation/n8n-review/
  README.md          이 문서
  workflow.json       n8n export
  prompts/
    review-schema.json   구조화 출력 JSON 스키마
```
