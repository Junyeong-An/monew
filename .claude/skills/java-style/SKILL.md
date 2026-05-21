---
name: java-style
description: PR 전 변경된 Java 파일의 Google Java Style Guide 준수 여부 점검
allowed-tools: Bash
---

## 실행

1. `google-java-format` 설치 여부 확인:
   ```
   which google-java-format
   ```
   미설치 시 아래 안내 출력 후 중단:
   ```
   google-java-format 미설치. 설치 후 재실행:
     brew install google-java-format
   ```

2. 변경된 `.java` 파일 목록 수집:
   ```
   git diff --name-only upstream/dev HEAD -- '*.java' 2>/dev/null \
     || git diff --name-only origin/dev HEAD -- '*.java'
   ```
   목록이 비어 있으면 "변경된 Java 파일 없음" 출력 후 종료.

3. 수집한 파일 목록을 대상으로 스타일 검사:
   ```
   google-java-format --dry-run --set-exit-if-changed <파일목록>
   ```

4. 결과 출력:
   - 종료 코드 0 → "✅ 스타일 검사 통과"
   - 종료 코드 비0 → 재포맷 필요한 파일 목록 출력 후:
     ```
     ❌ 위 파일은 Google Java Style 재포맷이 필요합니다.
     IntelliJ에서 해당 파일을 열고 Reformat Code (Cmd+Alt+L / Ctrl+Alt+L) 후 재커밋하세요.
     ```
