---
name: create-issue
description: milestones.md 키워드로 이슈를 찾아 GitHub에 생성. 사용: /create-issue 회원가입
allowed-tools: Read, Bash
---

ARGUMENTS: $ARGUMENTS (검색 키워드. 예: 회원가입, 관심사 등록)

## 실행 순서

1. `.claude/issue_config.json` 읽기
2. `docs/draft/milestones.md`에서 제목에 $ARGUMENTS 포함된 이슈 검색
   - **매칭 있음**: 3번으로
   - **매칭 없음 → 신규 이슈 모드**:
     - 타입 선택: FEAT / FIX / CHORE / INFRA / DEPLOY / BATCH / TEST / REFACTOR / DOCS
     - 제목 입력 (사용자)
     - 담당자 선택 (issue_config.json assignees 목록 표시)
     - Milestone 선택 (issue_config.json milestones 목록 표시)
     - 4번으로
3. 복수 매칭 시 목록 출력 후 선택 요청
4. prefix → label, 담당자 이름 → GitHub username 변환
5. prefix에 맞는 `.github/ISSUE_TEMPLATE/*.md` 구조로 본문 생성
6. Milestone 없으면 먼저 생성: `gh api repos/{owner}/{repo}/milestones --method POST -f title=... -f due_on=...`
7. `gh issue create --title "..." --label "..." --assignee "..." --body "..."`