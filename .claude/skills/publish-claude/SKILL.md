---
name: publish-claude
description: 현재 .claude/, CLAUDE.md 변경사항을 claude-setup 브랜치에 push (팀장 전용)
allowed-tools: Bash
---

## 실행

브랜치 전환 전 파일을 임시 디렉토리에 보관한 뒤 아래 순서로 실행:

1. `CURRENT=$(git branch --show-current)`
2. `TMPDIR=$(mktemp -d)` — 임시 디렉토리 생성
3. `cp -r .claude "$TMPDIR/" 2>/dev/null; cp CLAUDE.md "$TMPDIR/" 2>/dev/null` — 파일 백업 (브랜치 전환 시 덮어써지는 것 방지)
4. `git fetch upstream`
5. `git switch claude-setup 2>/dev/null || git switch -c claude-setup upstream/claude-setup 2>/dev/null || git switch -c claude-setup`
6. `cp -r "$TMPDIR/.claude" . 2>/dev/null; cp "$TMPDIR/CLAUDE.md" . 2>/dev/null` — 백업 복원 (tmpdir 아직 유지)
7. `git add -f .claude/ CLAUDE.md`
8. `git diff --cached --name-only` 로 변경 파일 확인 — 없으면 중단
9. `git commit -m "chore: update claude settings"`
10. `git push upstream claude-setup`
11. `git switch $CURRENT`
12. `cp -r "$TMPDIR/.claude" . 2>/dev/null; cp "$TMPDIR/CLAUDE.md" . 2>/dev/null; rm -rf "$TMPDIR"` — 원래 브랜치에도 파일 복원 후 tmpdir 삭제

완료 후 변경된 파일 목록과 push 결과를 출력.
