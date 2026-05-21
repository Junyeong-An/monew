---
name: start-issue
description: milestones.md 키워드로 이슈를 찾아 브랜치를 생성. 사용: /start-issue 회원가입
allowed-tools: Read, Bash
---

ARGUMENTS: $ARGUMENTS (검색 키워드. 예: 회원가입)

## 실행 순서

1. `docs/milestones.md`에서 제목에 $ARGUMENTS 포함된 이슈 검색
2. 복수 매칭 시 목록 출력 후 선택 요청
3. 이슈 제목과 prefix로 브랜치명 생성: `{prefix}/{도메인}/{설명}`
   - 예) `[FEAT] 회원가입 API 구현` → `feat/user/register`
   - 도메인 매핑: `공통` → `infra`
4. 현재 브랜치가 dev가 아니면 경고
5. `git switch dev && git pull upstream dev && git push origin dev`
6. `git switch -c {브랜치명}`
7. 이슈 분석 후 작업 목록 출력 (아래 규칙 참고)
8. 완료 안내: "작업 후 /create-pr 실행하세요."

## 작업 목록 출력 규칙

이슈 제목과 도메인을 분석해 구현 순서와 `/write-junit-test` 명령어를 함께 출력한다.

### 도메인 API [FEAT] 이슈 (회원가입, 관심사, 기사 등)

`docs/api-docs.json`에서 해당 엔드포인트를 찾아 request/response 스펙을 확인한다.

출력 형식:
```
📋 작업 목록 — {이슈 제목}

구현 순서:
1. {Domain}Entity
2. {Domain}Repository
3. {Domain}Service     → /write-junit-test {Domain}Service
4. {Domain}Controller  → /write-junit-test {Domain}Controller

API 스펙:
  {HTTP Method} {path}
  Request:  {주요 필드}
  Response: {주요 필드}
```

### 공통 인프라 [FEAT] 이슈 (MDC, ErrorResponse 등)

이슈 설명을 기반으로 생성할 파일 목록을 추론한다.

출력 형식:
```
📋 작업 목록 — {이슈 제목}

구현 순서:
1. {파일명} ({패키지})  → /write-junit-test {클래스명}  (테스트 필요 시)
2. ...
```

### [FEAT] 외 이슈 ([CHORE], [DEPLOY], [DOCS] 등)

작업 목록 없이 브랜치 생성만 안내한다.