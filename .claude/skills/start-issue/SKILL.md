---
name: start-issue
description: milestones.md 키워드로 이슈를 찾아 브랜치를 생성. 사용: /start-issue 회원가입
allowed-tools: Read, Bash
---

ARGUMENTS: $ARGUMENTS (검색 키워드. 예: 회원가입)

## 실행 순서

1. `docs/draft/milestones.md`에서 제목에 $ARGUMENTS 포함된 이슈 검색
2. 복수 매칭 시 목록 출력 후 선택 요청
3. 이슈 제목과 prefix로 브랜치명 생성: `{prefix}/{도메인}/{설명}`
   - 예) `[FEAT] 회원가입 API 구현` → `feat/user/register`
4. 현재 브랜치가 dev가 아니면 경고
5. `git switch dev && git pull origin dev`
6. `git switch -c {브랜치명}`
7. 완료 안내: "브랜치 {브랜치명} 생성 완료. 작업 후 /create-pr 실행하세요."