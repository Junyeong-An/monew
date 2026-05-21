---
name: create-issues
description: milestones.md에서 특정 phase 전체 이슈를 GitHub에 일괄 생성. 사용: /create-issues 1
allowed-tools: Read, Bash, Agent
---

ARGUMENTS: $ARGUMENTS (phase 번호. 예: 1. 생략 시 전체)

## 실행 순서

1. `.claude/issue_config.json` 읽기 → FILL_IN 남아있으면 중단하고 입력 요청
2. `docs/draft/milestones.md`에서 Phase $ARGUMENTS 이슈 전체 파싱
3. 각 이슈별 title/label/assignee/milestone/body 데이터 완성
4. Subagent에 이슈 목록 전달하여 실행 위임:
   - Milestone 생성 (존재하면 skip)
   - label 없으면 자동 생성
   - 각 이슈 `gh issue create` 실행 (동일 제목 open 이슈 존재하면 skip)
5. 완료 요약만 반환: "Phase N: X개 생성, Y개 skip"

> bulk 실행이므로 subagent에 위임 — main 컨텍스트에 gh 결과 누적 없음