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
4. **이슈 번호 확인**:
   - `.claude/issue_log.json`에서 현재 브랜치명과 일치하는 항목의 `github_number` 조회
   - 로그에 있으면 자동 사용, 없으면 사용자에게 직접 입력 요청 (milestones.md 순번 아님, `gh issue list`로 확인 가능)
5. 브랜치 prefix 기반 type label 결정, domain label 추론 (불확실하면 사용자 확인)
6. `.github/pull_request_template.md` 읽기 → 본문 기반으로 body 작성:
   - 체크박스 항목은 해당하는 것만 `[x]`로 체크, **해당 없는 항목은 목록에서 완전히 제거**
   - `test(red):` / `test(green):` / `refactor:` 커밋 존재 여부로 TDD 체크박스 자동 체크
   - 리뷰 포인트: `git diff upstream/dev...HEAD --stat` 기반 1-2줄 생성
   - 스크린샷/참고 자료: 없으면 섹션 자체 제거
7. PR 작성자를 reviewer 목록에서 제외 후:
   ```
   gh pr create \
     --repo {upstream_owner}/{upstream_repo} \
     --head {fork_owner}:{브랜치} \
     --base dev \
     --title "..." \
     --body "..." \
     --reviewer "{reviewers}" \
     --assignee "{PR_작성자_GitHub_username}" \
     --label "{type_label}" \
     --label "{domain_label}"
   ```
8. PR 생성 성공 시 `.claude/issue_log.json`에서 현재 브랜치명과 일치하는 항목 삭제

## PR 제목 규칙

브랜치 `feat/user/register` → 제목 `feat: 사용자 회원가입 구현`
(도메인+설명을 한글로 풀어서 작성, squash 커밋 메시지와 동일하게)