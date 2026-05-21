---
name: fix-review
description: PR 리뷰 코멘트를 읽고 수정사항을 반영. 사용: /fix-review 또는 /fix-review {PR번호}
allowed-tools: Read, Bash, Edit
---

ARGUMENTS: $ARGUMENTS (PR 번호. 생략 시 현재 브랜치 PR 자동 조회)

## 실행 순서

1. PR 번호 확인: 인자 없으면 `gh pr view --json number`로 현재 브랜치 PR 조회
2. `gh pr view {PR번호} --json reviews,comments` → 리뷰 코멘트 전체 조회
3. resolved 되지 않은 코멘트 목록 정리해서 출력
4. 각 코멘트별 파일 위치 파악 후 수정 제안
5. 사용자 승인 후 수정 적용
6. "수정 완료. git add → commit → git push 후 PR 확인하세요." 안내