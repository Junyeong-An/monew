---
name: remove-gitkeep
description: 실제 파일이 생겨 불필요해진 .gitkeep만 제거하고 커밋. 사용: /remove-gitkeep
allowed-tools: Bash
---

## 실행

1. 제거 대상 탐색 — 다른 파일이 존재하는 디렉토리의 `.gitkeep`만 선택:
   ```bash
   find . -name ".gitkeep" -not -path "./.git/*" | while read f; do
     dir=$(dirname "$f")
     count=$(find "$dir" -maxdepth 1 -not -name ".gitkeep" -not -name "." | wc -l)
     [ "$count" -gt 0 ] && echo "$f"
   done
   ```
   없으면 "제거할 .gitkeep 파일이 없습니다." 출력 후 중단.

2. 목록을 사용자에게 출력 후 삭제:
   ```bash
   find . -name ".gitkeep" -not -path "./.git/*" | while read f; do
     dir=$(dirname "$f")
     count=$(find "$dir" -maxdepth 1 -not -name ".gitkeep" -not -name "." | wc -l)
     [ "$count" -gt 0 ] && rm "$f"
   done
   ```

3. 스테이징 및 커밋:
   ```bash
   git add -u
   git diff --cached --name-only
   git commit -m "chore: .gitkeep 제거"
   ```

완료 후 삭제된 파일 목록 출력.