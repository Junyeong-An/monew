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
7. 사용자에게 Priority 선택 요청 (Very High / High / Middle / Low / Very Low, 기본값 Middle)
   Target date 입력 요청 (기본값: 오늘 날짜, 사용자 입력 있으면 입력값 사용, YYYY-MM-DD)
8. `gh issue create --repo {upstream_owner}/{upstream_repo} --title "..." --label "type_label" --label "domain_label" --assignee "..." --milestone "{milestone_title}" --project "monew" --body "..."` → 반환된 issue URL 저장
9. issue node ID 조회: `gh api repos/{owner}/{repo}/issues/{number} --jq '.node_id'`
   project item ID 조회:
   ```
   gh api graphql -f query='{ node(id: "PVT_kwDOENwgXM4BYEXZ") { ... on ProjectV2 { items(first: 50) { nodes { id content { ... on Issue { number } } } } } } }' --jq '.data.node.items.nodes[] | select(.content.number == {issue_number}) | .id'
   ```
10. GraphQL로 이슈 타입 설정 (issue_config.json `issue_types`에서 ID 조회):
    ```
    gh api graphql -f query='mutation { updateIssue(input: {id: "{issue_node_id}", issueTypeId: "{type_id}"}) { issue { id } } }'
    ```
11. GraphQL로 Project 필드 일괄 설정 (issue_config.json `project_fields`에서 ID 조회):
    - Status → Todo
    - Priority → 선택값
    - Start date → 오늘 날짜 (YYYY-MM-DD)
    - Target date → 입력값
    - Category → type label 기준 자동 매핑
    ```
    gh api graphql -f query='mutation { updateProjectV2ItemFieldValue(input: { projectId: "PVT_kwDOENwgXM4BYEXZ", itemId: "{item_id}", fieldId: "{field_id}", value: { singleSelectOptionId: "{option_id}" } }) { projectV2Item { id } } }'
    ```
    날짜 필드는 `value: { date: "YYYY-MM-DD" }` 사용
12. type label이 `adr`이면 issue_log.json 기재를 건너뛴다 (ADR은 PR 불필요).
    그 외 타입은 `.claude/issue_log.json`에 아래 형식으로 항목 추가 (파일 없으면 생성):
    ```json
    { "milestone_seq": {milestones.md의 #번호}, "github_number": {실제 이슈 번호}, "title": "{이슈 제목}", "branch": "{생성된 브랜치명}" }
    ```