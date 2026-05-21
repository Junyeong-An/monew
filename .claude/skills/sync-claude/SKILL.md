---
name: sync-claude
description: claude-setup 브랜치에서 최신 Claude Code 설정(.claude/, CLAUDE.md)을 현재 브랜치로 동기화
allowed-tools: Bash
---

## 실행

1. `git fetch upstream`
2. `git checkout upstream/claude-setup -- .claude/ CLAUDE.md`
3. 변경된 파일 목록을 출력해 사용자에게 알림
4. `git restore --staged .claude/ CLAUDE.md` — unstaged 상태로 변경
