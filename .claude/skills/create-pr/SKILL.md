---
name: create-pr
description: 현재 브랜치 기준으로 PR을 자동 생성. 사용: /create-pr
allowed-tools: Read, Bash
---

## 실행 순서

1. `.claude/issue_config.json` 읽기 (reviewer 목록)
2. `git branch --show-current` → 브랜치명 파싱 (prefix/domain/desc)
3. 커밋 확인: `git log upstream/dev..HEAD --oneline`
   - 커밋 있음 → 5번으로
   - **커밋 없음 → 미커밋 변경사항 탐색**:
     - `git status --short` + `git diff HEAD --stat` 출력
     - 커밋할 파일 목록을 사용자에게 보여주고 선택 요청
     - 선택된 파일 `git add (-f 포함)` → 커밋 메시지 제안 후 `git commit` → `git push origin {브랜치명}`
4. `docs/milestones.md`에서 브랜치 키워드로 이슈 번호 매칭 → `Closes #N`
5. 브랜치 prefix 기반 PR 유형 체크박스 자동 체크
6. `test(red):` / `test(green):` / `refactor:` 커밋 존재 여부로 TDD 체크박스 자동 체크
7. `git diff upstream/dev...HEAD --stat` 기반 리뷰 포인트 1-2줄 생성
8. PR 작성자를 reviewer 목록에서 제외 후 `gh pr create --repo {upstream} --head {fork}:{브랜치} --base dev --title "..." --body "..." --reviewer "..."`

## PR 제목 규칙

브랜치 `feat/user/register` → 제목 `feat: 사용자 회원가입 구현`
(도메인+설명을 한글로 풀어서 작성, squash 커밋 메시지와 동일하게)