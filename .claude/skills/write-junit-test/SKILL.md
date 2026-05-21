---
name: write-junit-test
description: 모뉴 프로젝트 JUnit5+Mockito 테스트 파일 생성. TDD [RED] 커밋까지 수행. 사용: /write-junit-test UserService
allowed-tools: Read, Glob, Grep, Bash, Write
---

ARGUMENTS: $ARGUMENTS (대상 클래스명. 예: UserService)

## 실행 순서

### 1. 컨텍스트 수집

다음을 읽기:
- `src/main/java/com/sprint/mission/monew/domain/**/$ARGUMENTS.java` (대상 클래스)
- `src/test/java/com/sprint/mission/monew/domain/**/*Test.java` (기존 테스트 패턴 참조)
- `docs/conventions.md` 섹션 8

### 2. 레이어별 설정

| 파일 패턴 | 어노테이션 |
|-----------|-----------|
| `*Service.java` | `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` + `@Mock` |
| `*Repository.java` | `@DataJpaTest`, `@Autowired` |
| `*Controller.java` | `@WebMvcTest`, `MockMvc`, `@MockBean` |

### 3. 파일 생성 규칙

```java
class {대상}Test {

    @Nested
    @DisplayName("{public 메서드 한글명}")
    class {한글명} {

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

Controller 추가:
- HTTP 상태코드 검증 (`isOk()`, `isCreated()`, `isNotFound()` 등)
- `Monew-Request-User-ID` 헤더 포함 여부 (필요한 엔드포인트)
- Bean Validation 실패 → 400 케이스

### 4. 저장 위치

`src/test/java/com/sprint/mission/monew/domain/{도메인}/{대상}Test.java`

### 5. TDD test(red): 커밋

```bash
./gradlew test --tests "*.{대상}Test"
```

실패 확인 후:
```bash
git add src/test/java/.../{대상}Test.java
git commit -m "test(red): {대상} 실패 테스트 추가"
```

### 완료 후 안내

구현 완료 → `./gradlew test` 통과 확인 → `git commit -m "test(green): {대상} 구현"`
리팩토링 후 → `git commit -m "refactor: {대상} 정리"`