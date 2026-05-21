---
name: create-issue
description: milestones.md 키워드로 이슈를 찾아 GitHub에 생성. 사용: /create-issue 회원가입
allowed-tools: Read, Bash
---

ARGUMENTS: $ARGUMENTS (검색 키워드. 예: 회원가입, 관심사 등록)

## 실행 순서

1. `.claude/issue_config.json` 읽기
2. `docs/milestones.md`에서 제목에 $ARGUMENTS 포함된 이슈 검색
   - **매칭 있음**: 3번으로
   - **매칭 없음 → 신규 이슈 모드**:
     - 타입 선택: FEAT / FIX / CHORE / DEPLOY / BATCH / TEST / REFACTOR / DOCS / ADR
     - 도메인 선택 (선택): user / interest / article / comment / notification / activity / infra / 없음
     - 제목 입력 (사용자)
     - 담당자 선택 (issue_config.json assignees 목록 표시)
     - Milestone 선택 (issue_config.json milestones 목록 표시)
     - 4번으로
3. 복수 매칭 시 목록 출력 후 선택 요청
4. prefix → type label, 이슈 제목에서 도메인 키워드 추론 → domain label (해당 없으면 질문)
   담당자 이름 → GitHub username 변환
5. prefix에 맞는 `.github/ISSUE_TEMPLATE/*.md` 구조로 본문 생성
6. repo 정보 확인: `gh repo view --json nameWithOwner` → upstream repo 식별
   Milestone은 `--milestone "{title}"` 형식으로 전달 (번호 아닌 제목 사용)
   Milestone 없으면 먼저 생성: `gh api repos/{owner}/{repo}/milestones --method POST -f title=... -f due_on=...`
7. `gh issue create --repo {upstream_owner}/{upstream_repo} --title "..." --label "type_label" --label "domain_label" --assignee "..." --milestone "{milestone_title}" --body "..."`