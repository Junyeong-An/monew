# 모뉴(MoNew) 코딩 컨벤션

> **기준**: [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) 준수.

### 적용 방법

**IntelliJ IDEA**

1. [intellij-java-google-style.xml](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml) 다운로드
2. `Settings > Editor > Code Style > Java > ⚙️ > Import Scheme > IntelliJ IDEA code style XML`
3. `Scheme:GoogleStyle > ⚙️ > Copy to Project > Apply` - `.idea/`에 저장되어 팀 전체 공유
4. PR 전 `Reformat Code` (`Cmd+Alt+L` / `Ctrl+Alt+L`) 실행

PR 생성 전 스타일 점검은 `/java-style` 커맨드로 실행합니다.

---

## 1. 패키지 & 클래스 구조

### 전체 구조

```
com.sprint.mission.monew/
├── common/
│   ├── config/
│   │   ├── JpaConfig.java               # @EnableJpaAuditing
│   │   ├── QuerydslConfig.java          # JPAQueryFactory bean
│   │   ├── MongoConfig.java             # MongoDB 설정 (심화)
│   │   ├── S3Config.java                # AWS S3
│   │   └── SwaggerConfig.java          # springdoc-openapi
│   ├── exception/
│   │   ├── MonewException.java          # 추상 기본 예외
│   │   ├── ErrorCode.java               # 에러 코드 enum
│   │   └── GlobalExceptionHandler.java  # @ControllerAdvice
│   ├── dto/
│   │   ├── CursorPageResponse.java
│   │   └── ErrorResponse.java
│   └── filter/
│       └── MdcLoggingFilter.java        # 요청 ID + IP MDC 주입
├── domain/
│   ├── user/
│   ├── interest/
│   ├── article/                         # event/ — ArticleCreatedEvent
│   ├── comment/                         # event/ — CommentLikedEvent
│   ├── notification/                    # event/ — NotificationEventListener (@EventListener)
│   └── useractivity/                    # MongoDB (심화)
├── external/
│   ├── naver/
│   │   ├── NaverNewsClient.java
│   │   └── dto/
│   │       └── NaverNewsResponse.java
│   └── rss/
│       ├── RssNewsParser.java
│       └── dto/
│           └── RssItem.java
└── batch/                               # 심화
    ├── NewsCollectJobConfig.java        # 매 시간 — 뉴스 수집
    ├── NotificationCleanJobConfig.java  # 매일 — 알림 삭제
    ├── ArticleBackupJobConfig.java      # 매일 — S3 백업
    └── LogUploadJobConfig.java          # 매일 — 로그 업로드
```

### 테스트 구조

main 패키지 구조를 그대로 미러링합니다. 테스트 유형별 어노테이션은 위치로 구분합니다.

```
src/test/java/com/sprint/mission/monew/
├── common/
│   └── exception/
│       └── GlobalExceptionHandlerTest.java
├── domain/
│   ├── user/
│   │   ├── controller/
│   │   │   └── UserControllerTest.java       # @WebMvcTest
│   │   ├── repository/
│   │   │   └── UserRepositoryTest.java       # @DataJpaTest
│   │   └── service/
│   │       └── UserServiceTest.java          # @ExtendWith(MockitoExtension.class)
│   ├── interest/
│   │   ├── controller/
│   │   ├── repository/
│   │   └── service/
│   └── ...
└── batch/
    └── NewsCollectJobTest.java               # @SpringBatchTest (심화)
```

| 위치 | 어노테이션 | 특징 |
|------|-----------|------|
| `service/` | `@ExtendWith(MockitoExtension.class)` | Mock 의존성, 빠름 |
| `repository/` | `@DataJpaTest` | 실제 DB(H2/TestContainers), JPA 레이어만 로드 |
| `controller/` | `@WebMvcTest` | MockMvc, 서비스는 `@MockBean` |
| `batch/` | `@SpringBatchTest` | Job/Step 단위 테스트 |

### 도메인 구조

각 도메인은 독립적인 패키지 안에서 Controller → Service → Repository 레이어로 구성합니다.
DTO는 `dto/` 하위 패키지로 분리하고, 요청/응답을 명확히 구분합니다.

Controller에서 요청 DTO는 반드시 `@Valid`를 붙입니다.

```java
@PostMapping
public ResponseEntity<UserResponse> register(
    @Valid @RequestBody UserRegisterRequest request
) { ... }
```

Bean Validation 어노테이션: `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`

```
domain/
  user/
    controller/
      api/
        UserApi.java             # Swagger 인터페이스 (@Tag, @Operation)
      UserController.java        # implements UserApi
    dto/
      UserRegisterRequest.java
      UserQueryCondition.java    # 목록 조회 조건
      UserResponse.java
      UserUpdateRequest.java
    entity/
      User.java                  # JPA Entity
    event/
      UserDeletedEvent.java      # ApplicationEvent 구현체 (발행 도메인에 위치)
    exception/
      UserException.java         # 추상 중간 예외
      UserNotFoundException.java # 팩토리 메서드 포함
    mapper/
      UserMapper.java
    repository/
      querydsl/
        impl/
          UserCustomRepositoryImpl.java
        UserCustomRepository.java
      UserRepository.java        # extends JpaRepository + UserCustomRepository
    service/
      UserService.java
```

Swagger 어노테이션(`@Tag`, `@Operation`)은 `controller/api/*Api` 인터페이스에만 작성합니다. Controller는 인터페이스를 구현하고 비즈니스 로직에만 집중합니다.

```java
// controller/api/UserApi.java
@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @Operation(summary = "회원가입")
    ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request);
}

// controller/UserController.java
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController implements UserApi {

    @PostMapping
    @Override
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }
}
```

커서 페이지네이션이 필요한 도메인은 QueryDSL 커스텀 레포를 사용합니다.

```java
// repository/querydsl/UserCustomRepository.java
public interface UserCustomRepository {
    CursorPageResponse<UserResponse> findByCondition(UserQueryCondition condition, UUID requestUserId);
}

// repository/querydsl/impl/UserCustomRepositoryImpl.java
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {
    private final JPAQueryFactory queryFactory;
}

// repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, UUID>, UserCustomRepository {}
```

- MongoDB document는 `document/` 패키지로 분리합니다.

```
domain/
  useractivity/
    document/
      UserActivity.java          # @Document
      RecentComment.java         # 내장 문서
    service/
      UserActivityService.java
```

- DTO는 `record`를 사용합니다. 불변성을 보장하고 보일러플레이트를 줄입니다.

```java
// 요청 DTO
public record UserRegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 20) String nickname,
    @NotBlank String password
) {}

// 응답 DTO
public record UserResponse(
    UUID id,
    String email,
    String nickname,
    Instant createdAt
) {}
```

---

## 2. JPA Entity

PK는 UUID를 사용합니다. Long 시퀀스는 값 추측이 가능하고 분산 환경에서 충돌 위험이 있습니다.
시간 타입은 `Instant`로 통일합니다. `LocalDateTime`은 시간대 처리가 필요한 경우 문제가 됩니다.
소프트딜리트는 `deletedAt`(Instant) null 여부로 판단합니다. `isDeleted` boolean은 사용하지 않습니다.

Spring Data Auditing을 사용합니다. `@EnableJpaAuditing`을 설정 클래스에 추가해야 합니다.

모든 엔티티는 `BaseEntity` 또는 `BaseUpdatableEntity`를 상속합니다.
- `BaseEntity` — id + createdAt만 필요한 불변형 엔티티 (예: Notification)
- `BaseUpdatableEntity` — updatedAt이 추가되는 수정 가능 엔티티 (예: User, Article, Comment)

```java
// common/entity/BaseEntity.java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class BaseEntity {

    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    @Id
    private UUID id = UUID.randomUUID();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

// common/entity/BaseUpdatableEntity.java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseUpdatableEntity extends BaseEntity {

    @LastModifiedDate
    private Instant updatedAt;
}
```

구체 엔티티는 기반 클래스를 상속하고 도메인 필드만 선언합니다.
`@EntityListeners`는 `BaseEntity`에서 상속되므로 재선언하지 않습니다.
생성은 정적 팩토리 메서드로 합니다. 빌더(`@Builder`, `@SuperBuilder`) 금지.

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")   // 테이블명: 소문자 복수형
@Entity
public class User extends BaseUpdatableEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private Instant deletedAt;

    public static User create(String email, String nickname) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        return user;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
```

`@EnableJpaAuditing`은 메인 클래스 또는 별도 Config 클래스에 추가합니다.

```java
@EnableJpaAuditing
@Configuration
public class JpaConfig {}
```

- 연관관계 fetch 전략은 **LAZY**로 통일합니다. 필요한 경우에만 JPQL fetch join으로 명시적으로 로딩합니다.
- **단방향 `@ManyToOne`만 사용합니다.** `@OneToMany`는 선언하지 않습니다. 역방향 조회가 필요하면 Repository 쿼리로 해결합니다.
- `@JoinColumn(name = "...")` 은 항상 명시합니다. 컬럼명은 `{필드명}_id` 규칙을 따릅니다.
- cascade 범위는 ADR-02 결정에 따릅니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "article_id", nullable = false)
private Article article;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

- Setter는 만들지 않습니다. 상태 변경은 의도가 드러나는 메서드명으로 작성합니다.

```java
// 나쁜 예
user.setDeletedAt(Instant.now());

// 좋은 예
user.softDelete();

public void softDelete() {
    this.deletedAt = Instant.now();
}
```

---

## 3. 인증 헤더

JWT 없음. 로그인 응답의 `id`를 클라이언트가 저장하고, 인증이 필요한 요청마다 헤더에 포함합니다.

```
Monew-Request-User-ID: {userId}
```

> ⚠️ 헤더명 대소문자 주의: `Monew-Request-User-ID` (M 대문자, 이후 소문자)

컨트롤러에서 헤더를 수신하는 방법:

```java
@GetMapping("/api/articles")
public ResponseEntity<CursorPageResponse<ArticleResponse>> getArticles(
    @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
    ...
) { ... }
```

`Monew-Request-User-ID` 헤더가 필요한 엔드포인트:
- 목록/단건 조회: `viewedByMe`, `subscribedByMe`, `likedByMe` 필드 계산에 사용
- 구독·조회수·좋아요: 주체 식별에 사용
- 댓글 수정: 본인 검증에 사용
- 알림 조회·확인: 사용자 식별에 사용

---

## 4. 공통 응답 포맷

성공 응답은 DTO를 직접 반환합니다. 프론트엔드가 래퍼 없이 DTO를 직접 수신하도록 구현되어 있습니다.
에러 응답만 공통 포맷 `ErrorResponse`로 통일합니다.

```java
// 성공 — 데이터 있음: DTO 직접 반환
return ResponseEntity.ok(userResponse);

// 성공 — 201 Created
return ResponseEntity.status(HttpStatus.CREATED).body(commentResponse);

// 성공 — 데이터 없음 (삭제, 확인 등)
return ResponseEntity.noContent().build();

// 에러 응답 구조 (@ControllerAdvice에서 사용)
public record ErrorResponse(
    Instant timestamp,
    String code,                    // ErrorCode enum name
    String message,
    Map<String, Object> details,    // 어떤 값이 원인인지 (예: {"userId": "uuid-..."})
    String exceptionType,           // 예외 클래스 단순명
    int status
) {}
```

```java
// @RestControllerAdvice 예시
@ExceptionHandler(MonewException.class)
public ResponseEntity<ErrorResponse> handleMonewException(MonewException e) {
    ErrorCode code = e.getErrorCode();
    return ResponseEntity
        .status(code.getStatus())
        .body(new ErrorResponse(
            Instant.now(),
            code.name(),
            code.getMessage(),
            e.getDetails(),
            e.getClass().getSimpleName(),
            code.getStatus().value()
        ));
}
```

---

## 5. 커스텀 예외 & 에러 코드

예외는 `{도메인}_{동사}_{명사}` 형태의 `ErrorCode` enum으로 관리합니다.
`@RestControllerAdvice`에서 일괄 처리합니다.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_OWNER(HttpStatus.FORBIDDEN, "본인만 수행할 수 있습니다."),

    // Interest
    INTEREST_NAME_SIMILAR(HttpStatus.CONFLICT, "유사한 이름의 관심사가 이미 존재합니다."),
    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),
    INTEREST_ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "이미 구독 중인 관심사입니다."),

    // Article
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "뉴스 기사를 찾을 수 없습니다."),
    ARTICLE_URL_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 기사 링크입니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 댓글만 수정할 수 있습니다."),
    COMMENT_LIKE_DUPLICATE(HttpStatus.CONFLICT, "이미 좋아요를 누른 댓글입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
```

도메인별로 구체 예외 클래스를 정의합니다. 팩토리 메서드로 어떤 값이 원인인지 명시합니다.

```java
// 추상 기본 클래스
@Getter
public abstract class MonewException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected MonewException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}

// 도메인별 중간 클래스
public abstract class UserException extends MonewException {
    protected UserException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}

// 구체 예외 — 팩토리 메서드로 원인 명시
public class UserNotFoundException extends UserException {
    private UserNotFoundException(Map<String, Object> details) {
        super(ErrorCode.USER_NOT_FOUND, details);
    }

    public static UserNotFoundException withId(UUID userId) {
        return new UserNotFoundException(Map.of("userId", userId));
    }

    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException(Map.of("email", email));
    }
}

// 예외 발생
throw UserNotFoundException.withId(userId);
throw UserNotFoundException.withEmail(email);
```

---

## 6. 커서 페이지네이션

커서는 `cursor`(정렬 기준값)와 `after`(ID) 두 개를 함께 사용합니다.
정렬값이 동일한 항목이 있어도 `after`로 정확한 위치를 특정할 수 있습니다.

```java
// 공통 응답 구조
public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,      // 다음 페이지 없으면 null
    Instant nextAfter,      // 다음 페이지 없으면 null
    boolean hasNext,
    int size,
    Long totalElements      // count 쿼리 생략 시 null
) {}
```

목록 조회 조건은 도메인별 `QueryCondition` record로 정의합니다.
Controller에서 개별 `@RequestParam`을 받아 record로 조합한 뒤 Service로 전달합니다.

```java
// dto/ArticleQueryCondition.java
public record ArticleQueryCondition(
    String cursor,
    Instant after,
    int limit,
    ArticleOrderBy orderBy,
    SortDirection direction,
    UUID interestId,
    List<String> sourceIn
) {}
```

```java
// 요청 파라미터 — 모든 목록 API 통일
@GetMapping("/api/articles")
public ResponseEntity<CursorPageResponse<ArticleResponse>> getArticles(
    @ParameterObject @ModelAttribute @Valid ArticleQueryCondition condition,
    @RequestHeader("Monew-Request-User-ID") UUID requestUserId
) {
    return ResponseEntity.ok(articleService.getArticles(condition, requestUserId));
}
```

정렬 기준 Enum의 값은 프론트엔드 파라미터와 정확히 일치해야 합니다.
`@JsonProperty`로 직렬화 이름을 camelCase로 지정합니다.

```java
public enum ArticleOrderBy {
    @JsonProperty("publishDate")    PUBLISH_DATE,
    @JsonProperty("viewCount")      VIEW_COUNT,
    @JsonProperty("commentCount")   COMMENT_COUNT
}

public enum InterestOrderBy {
    @JsonProperty("name")               NAME,
    @JsonProperty("subscriberCount")    SUBSCRIBER_COUNT
}

public enum CommentOrderBy {
    @JsonProperty("createdAt")  CREATED_AT,
    @JsonProperty("likeCount")  LIKE_COUNT
}

public enum SortDirection { ASC, DESC }
```

---

## 7. @Transactional 사용 규칙

Service 클래스에 `@Transactional(readOnly = true)`를 기본으로 걸고,
데이터 변경이 있는 메서드에만 `@Transactional`을 개별 적용합니다.

```java
@Transactional(readOnly = true)   // 클래스 기본: 읽기 전용
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    // 조회 — 클래스 기본 적용
    public UserResponse getUser(UUID userId) {
        return userRepository.findById(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> UserNotFoundException.withId(userId));
    }

    // 변경 — 개별 재정의
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        ...
    }
}
```

---

## 8. 테스트

클래스명은 `{대상클래스}Test`, 메서드명과 `@DisplayName`은 한글로 작성합니다.
`@Nested` + `@DisplayName`으로 메서드 단위 시나리오를 그룹화합니다.
`given / when / then` 구분자 주석을 항상 작성합니다.

```java
// 단위 테스트 — Service
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks UserService userService;
    @Mock UserRepository userRepository;

    @Nested
    @DisplayName("회원가입")
    class 회원가입 {

        @Test
        @DisplayName("이메일 중복 시 예외 발생")
        void 이메일_중복_시_예외_발생() {
            // given
            given(userRepository.existsByEmail("test@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserEmailDuplicateException.class);
        }

        @Test
        @DisplayName("성공 시 저장된 사용자 반환")
        void 성공_시_저장된_사용자_반환() {
            // given
            // when
            // then
        }
    }
}

// Repository 테스트 — @DataJpaTest
@DataJpaTest
class ArticleRepositoryTest {

    @Autowired ArticleRepository articleRepository;

    @Nested
    @DisplayName("커서 페이지네이션 조회")
    class 커서_페이지네이션_조회 {

        @Test
        @DisplayName("cursor 없으면 첫 페이지 반환")
        void cursor_없으면_첫_페이지_반환() {
            // given
            // when
            // then
        }
    }
}

// Controller 테스트 — @WebMvcTest
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Nested
    @DisplayName("POST /api/users — 회원가입")
    class 회원가입 {

        @Test
        @DisplayName("성공 시 201 반환")
        void 성공_시_201_반환() throws Exception {
            // given
            // when & then
            mockMvc.perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("이메일 중복 시 409 반환")
        void 이메일_중복_시_409_반환() throws Exception {
            // given
            // when & then
        }
    }
}
```

---

## 9. Mapper (MapStruct)

Entity → DTO 변환은 MapStruct를 사용합니다. Service에서 직접 변환 로직을 작성하지 않습니다.

```java
// 기본
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}

// 필드명이 다르거나 계산이 필요한 경우
@Mapper(componentModel = "spring", uses = {InterestMapper.class})
public interface CommentMapper {

    @Mapping(target = "articleId", source = "comment.article.id")
    @Mapping(target = "likedByMe", expression = "java(comment.isLikedBy(requestUserId))")
    CommentItem toResponse(Comment comment, UUID requestUserId);
}
```

- `componentModel = "spring"`: 스프링 빈으로 등록
- `uses`: 다른 매퍼에 의존할 때 명시
- `@Mapping(expression = ...)`: 메서드 호출이 필요한 계산 필드

---

## 10. 로깅

`@Slf4j`를 사용합니다. `System.out.println` 사용 금지.

| 레벨 | 사용 시점 |
|------|---------|
| `log.debug` | 메서드 진입, 중간 상태값 확인 |
| `log.info` | 주요 액션 성공 (생성·삭제·배치 완료) |
| `log.warn` | 예외 발생, 비즈니스 규칙 위반 |
| `log.error` | 예상치 못한 오류, 외부 API 실패 |

```java
@Slf4j
@Service
public class ArticleService {

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        log.debug("기사 등록 시도: source={}, url={}", request.source(), request.sourceUrl());

        // 비즈니스 로직

        log.info("기사 등록 완료: id={}, source={}", article.getId(), article.getSource());
        return mapper.toResponse(article);
    }
}
```

---

## 11. Git 컨벤션

### 브랜치 전략

```
main (production)
└── dev (staging)
    └── {prefix}/{domain}/{description}
        예) feat/user/register, fix/article/duplicate-url
```

prefix: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `batch`, `deploy`

> `dev` / `main` 직접 push 금지.

### 워크플로우 (9단계)

1. GitHub Issue 등록 — `[FEAT] 사용자 회원가입 구현` 형식
2. `git switch dev`
3. `git pull upstream dev`
4. `git push origin dev`
5. `git switch -c feat/user/register`
6. 개발 작업
7. PR 전 `git pull upstream dev` → conflict check
8. `git push origin feat/user/register`
9. PR 작성: `dev ← feat/user/register`, 제목은 squash 커밋과 동일 (`feat: 사용자 회원가입 구현`)
10. **2인 이상** 리뷰 승인 → Squash and Merge

### Issue 제목

타입 기반 대문자 prefix 사용:

```
[FEAT] 사용자 회원가입 구현
[FIX] 조회수 중복 카운트 버그 수정
[REFACTOR] UserService 예외 처리 공통화
[TEST] 회원가입 서비스 단위 테스트 작성
[DOCS] API 명세 업데이트
[BATCH] 뉴스 수집 배치 구현
[CHORE] 의존성 버전 업그레이드
[DEPLOY] AWS ECS 배포 설정
```

### PR 제목 / 커밋 메시지

> squash and merge 방식이므로 PR 제목 = squash 커밋 메시지. 커밋 단위 메시지는 참고용.

PR 제목:

```
feat: 사용자 회원가입 구현
fix: 조회수 중복 카운트 버그 수정
batch: 뉴스 수집 배치 구현
deploy: GitHub Actions CI 워크플로우 추가
docs: API 명세 업데이트
```

TDD 단계별 커밋 (참고용):

```
test(red): 이메일 중복 회원가입 예외 테스트 추가
test(green): 이메일 중복 검증 로직 구현
refactor: UserService 예외 처리 공통화
```

### PR / Issue 템플릿

`.github/pull_request_template.md` — PR 작성 시 자동 적용.
`.github/ISSUE_TEMPLATE/` — Issue 유형별 8종 (feat, fix, refactor, docs, test, chore, deploy, adr).

---

## 12. 금기 사항

다음 패턴은 절대 사용하지 않습니다. PR 리뷰 시 즉시 reject 사유입니다.

### 의존성 주입

- `@Autowired` 필드 주입 금지 → `@RequiredArgsConstructor` + 생성자 주입

### 예외 처리

- `Optional.get()` 직접 호출 금지 → `orElseThrow()`
- 빈 catch 블록 금지 → 최소 `log.error()` 로깅
- `e.printStackTrace()` 금지 → `log.error("message", e)`

### JPA / DB

- N+1 쿼리 — fetch join / `@EntityGraph` **권장** (금지 아님, 발생 시 트러블슈팅으로 해결)
- Entity에 `@Builder` / `@SuperBuilder` 금지 → 정적 팩토리 메서드 사용
- `save()` 반환값 무시 금지 → 반환된 영속 엔티티 사용
- MongoDB / PostgreSQL 단일 트랜잭션 혼용 금지 → 별도 트랜잭션으로 분리

### 설계

- Controller에 비즈니스 로직 금지 → Service 위임
- `String`으로 UUID 파라미터 처리 금지 → `UUID` 타입 직접 선언
- `@Scheduled` 메서드에 직접 로직 금지 → Service 메서드 위임
- Swagger 어노테이션(`@Tag`, `@Operation`)을 Controller에 직접 작성 금지 → `controller/api/*Api` 인터페이스에만
- Controller 설계 시 Api 문서도 동반 작성 필수

### 테스트

- `@SpringBootTest` 남발 금지 → 슬라이스 테스트 (`@WebMvcTest`, `@DataJpaTest`, `@ExtendWith(MockitoExtension.class)`)
- 테스트 간 상태 공유 금지 → `@BeforeEach`로 매 테스트 초기화

### 로깅

- `System.out.println` 금지 → `@Slf4j` + `log.info/debug/error`

### 커밋

- AI co-author 커밋 금지 — `Co-authored-by: Claude` 등 AI 귀속 문구를 커밋 메시지에 포함하지 않습니다.
