---
name: publish-claude
description: 현재 .claude/, CLAUDE.md 변경사항을 claude-setup 브랜치에 push (팀장 전용)
allowed-tools: Bash
---

## 실행

현재 브랜치명을 저장한 뒤 아래 순서로 실행:

1. `CURRENT=$(git branch --show-current)`
2. `git fetch upstream`
3. `git switch claude-setup 2>/dev/null || git switch -c claude-setup upstream/claude-setup 2>/dev/null || git switch -c claude-setup`
4. `git checkout $CURRENT -- .claude/ CLAUDE.md`
5. `git add -f .claude/ CLAUDE.md`
6. `git diff --cached --name-only` 로 변경 파일 확인 — 없으면 중단
7. `git commit -m "chore: update claude settings"`
8. `git push upstream claude-setup`
9. `git switch $CURRENT`

완료 후 변경된 파일 목록과 push 결과를 출력.
