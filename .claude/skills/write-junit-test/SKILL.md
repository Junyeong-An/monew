---
name: write-junit-test
description: 모뉴 프로젝트 JUnit5+Mockito 테스트 파일 생성. TDD 첫 번째 시나리오 RED 커밋까지 수행. 사용: /write-junit-test UserService
allowed-tools: Read, Glob, Grep, Bash, Write
---

**ARGUMENTS:** $ARGUMENTS (대상 클래스명. 예: UserService)

## 실행 순서

### 1. 컨텍스트 수집

다음을 읽기:
- `src/main/java/com/sprint/mission/monew/**/$ARGUMENTS.java` (대상 클래스 — domain/ 및 common/ 포함)
- `src/test/java/com/sprint/mission/monew/**/*Test.java` (기존 테스트 패턴 참조)
- `docs/conventions.md` 섹션 8
- 대상 클래스가 속한 도메인의 `CLAUDE.md` (예: `UserService` → `domain/user/CLAUDE.md`)
  - **실패 케이스** 항목을 추출해 시나리오 목록에 반영
  - 파일이 없으면 skip

### 2. 레이어별 설정

| 파일 패턴 | 어노테이션 | 비고 |
|-----------|-----------|------|
| `*Entity.java` | 순수 JUnit5 (`@Test`만, Spring 컨텍스트 없음) | 정적 팩토리, 상태변경, 소프트딜리트 |
| `*Service.java` | `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` + `@Mock` | |
| `*Repository.java` | `@DataJpaTest`, `@Autowired` | |
| `*Controller.java` (`@RestController`) | `@WebMvcTest({대상}Controller.class)`, `@Autowired MockMvc`, `@MockBean` Service | `Monew-Request-User-ID` 헤더, Bean Validation 400 케이스 포함 |
| `*ExceptionHandler.java` | `@WebMvcTest(FakeController.class)`, 내부 `static @RestController FakeController` | |

### 3. 파일 생성 규칙

```java
class {대상}Test {

    // ⚠️ @Nested 클래스명: 반드시 영문 PascalCase
    //    한글 클래스명 → Gradle 클래스 로딩 시 wrong name 오류
    //    @DisplayName만 한글 유지
    @Nested
    @DisplayName("{메서드 한글명}")
    class RegisterUser {   // ← 영문 PascalCase

        @Test
        @DisplayName("성공 케이스 한글 설명")
        void 성공_케이스_한글_설명() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("실패 케이스 — 예외 발생")
        void 실패_케이스_예외_발생() {
            // given
            // when & then
            assertThatThrownBy(() -> ...)
                .isInstanceOf({구체예외}.class);
        }
    }
}
```

메서드당 커버: 정상 케이스, 예외 케이스(throw 지점마다), 경계값(null/empty/중복)

**Entity 테스트 패턴 (테스트 클래스명: `{Domain}Test`, EntityTest 아님):**
```java
class UserTest {
    @Nested
    @DisplayName("정적 팩토리 메서드")
    class Create {
        @Test void 정상_생성() { ... }
        @Test void 이메일_형식_오류_예외() { ... }
    }
    @Nested
    @DisplayName("소프트 딜리트")
    class SoftDelete {
        @Test void 삭제_시_deletedAt_설정() { ... }
    }
}
```

**ExceptionHandler FakeController 패턴:**
```java
@WebMvcTest({대상}ExceptionHandlerTest.FakeController.class)
class {대상}ExceptionHandlerTest {

    @RestController
    @RequestMapping("/test")
    static class FakeController {
        // 각 핸들러를 트리거하는 엔드포인트 정의
        // ⚠️ @RequestBody String은 JSON 파싱 안 함
        //    HttpMessageNotReadableException 유발 시 반드시 POJO 사용
        @PostMapping("/body")
        void body(@RequestBody BodyRequest req) {}
    }
    record BodyRequest(String name) {}
}
```

### 4. 저장 위치

대상 클래스의 main 경로를 그대로 test로 미러링:
- `src/main/.../domain/user/service/UserService.java` → `src/test/.../domain/user/service/UserServiceTest.java`
- `src/main/.../domain/user/entity/User.java` → `src/test/.../domain/user/entity/UserTest.java`
- `src/main/.../common/exception/GlobalExceptionHandler.java` → `src/test/.../common/exception/GlobalExceptionHandlerTest.java`

### 5. TDD test(red): 커밋

테스트 파일 저장 전, **구현 파일 존재 여부를 먼저 확인**:

```bash
find src/main -name "$ARGUMENTS.java"
```

**구현 파일이 없는 경우 (정상 TDD 순서):**

1. 전체 테스트 시나리오 목록 도출 (도메인 CLAUDE.md 실패 케이스 포함) 후 출력
2. **첫 번째 시나리오만** `@Nested` 블록으로 작성
3. 테스트 파일 저장
4. 테스트 실행:
```bash
./gradlew clean test --tests "*.{대상}Test" 2>&1
```
출력 결과를 사용자에게 그대로 표시 후:
```bash
git add src/test/java/.../{대상}Test.java
git commit -m "test(red): {첫 번째 시나리오} 실패 테스트 추가"
```

완료 안내:
```
✅ 완료: {첫 번째 시나리오}
⏳ 남은 시나리오:
  2. {두 번째 시나리오}
  3. {세 번째 시나리오}
  ...

각 시나리오마다:
  @Nested 블록 추가
  → ./gradlew clean test (red 확인) → test(red): 커밋
  → 구현 → ./gradlew clean test (green 확인) → test(green): 커밋
마지막: refactor 정리 → git commit -m "refactor: {대상} 정리"
```

**구현 파일이 이미 있는 경우 (TDD 순서 역전):**
- ⚠️ 경고: "구현 파일이 이미 존재합니다. red 상태를 만들 수 없습니다."
- 테스트 파일 커밋하지 않고 중단
- 사용자에게 선택지 안내:
  1. `git stash` 또는 구현 파일 임시 삭제 → `test(red):` 커밋 → 복원 → `test(green):` 커밋
  2. TDD 순서를 포기하고 `test(green):` 으로 직접 커밋