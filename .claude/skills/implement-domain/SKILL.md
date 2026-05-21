---
name: implement-domain
description: 도메인 전체 스캐폴딩. Entity→Repository→Service→Controller→DTO→Mapper→Exception 생성 후 /write-junit-test 연계. 사용: /implement-domain User
allowed-tools: Read, Glob, Grep, Bash, Write
---

ARGUMENTS: $ARGUMENTS (도메인명. 예: User, Article, Comment)

## 실행 순서

### 0. 사전 확인

읽기:
- `docs/conventions.md` (전체)
- `docs/api-docs.json` (해당 도메인 엔드포인트)
- 기존 도메인 파일 하나 (패턴 참조)

생성 파일 목록 출력 후 사용자 확인 받기.

### 1. Entity

```java
@Entity @Table(name = "{도메인소문자}s")   // 소문자 복수형
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class {도메인} {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 연관관계: 전부 FetchType.LAZY

    @CreatedDate @Column(nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    private Instant deletedAt;  // 소프트딜리트 대상만

    // setter 없음. 상태 변경 메서드만:
    public void softDelete() { this.deletedAt = Instant.now(); }
}
```

### 2. DTO (record 사용)

```java
public record {도메인}CreateRequest(
    @NotBlank String field
) {}

public record {도메인}Response(UUID id, String field, Instant createdAt) {}
```

목록 응답: `CursorPageResponse<{도메인}Response>`

### 3. Repository (QueryDSL 커스텀 레포 포함)

```java
// repository/querydsl/{도메인}CustomRepository.java
public interface {도메인}CustomRepository {
    CursorPageResponse<{도메인}Response> findByCondition({도메인}QueryCondition condition, UUID requestUserId);
}

// repository/querydsl/impl/{도메인}CustomRepositoryImpl.java
@RequiredArgsConstructor
public class {도메인}CustomRepositoryImpl implements {도메인}CustomRepository {
    private final JPAQueryFactory queryFactory;
}

// repository/{도메인}Repository.java
public interface {도메인}Repository extends JpaRepository<{도메인}, UUID>, {도메인}CustomRepository {}
```

### 4. Service

```java
@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class {도메인}Service {
    private final {도메인}Repository repo;
    private final {도메인}Mapper mapper;

    public {도메인}Response get(UUID id) {
        return repo.findById(id).map(mapper::toResponse)
            .orElseThrow(() -> {도메인}NotFoundException.withId(id));
    }

    @Transactional
    public {도메인}Response create({도메인}CreateRequest req) {
        log.debug("{도메인} 등록 시도: {}", req);
        {도메인} saved = repo.save({도메인}.create(req));
        log.info("{도메인} 등록 완료: id={}", saved.getId());
        return mapper.toResponse(saved);
    }
}
```

### 5. Controller (Api 인터페이스 분리)

```java
// controller/api/{도메인}Api.java
@Tag(name = "{도메인}", description = "{도메인} API")
public interface {도메인}Api {

    @Operation(summary = "{도메인} 생성")
    ResponseEntity<{도메인}Response> create(
        @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
        @Valid @RequestBody {도메인}CreateRequest req
    );
}

// controller/{도메인}Controller.java
@RestController @RequestMapping("/api/{도메인소문자}s")
@RequiredArgsConstructor
public class {도메인}Controller implements {도메인}Api {

    private final {도메인}Service service;

    @PostMapping
    @Override
    public ResponseEntity<{도메인}Response> create(
        @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
        @Valid @RequestBody {도메인}CreateRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }
}
```

성공 응답: DTO 직접 반환 (래퍼 없음)

### 6. Mapper

```java
@Mapper(componentModel = "spring")
public interface {도메인}Mapper {
    {도메인}Response toResponse({도메인} entity);
}
```

### 7. 예외

`ErrorCode` enum에 추가 후:
```java
public class {도메인}NotFoundException extends MonewException {
    public static {도메인}NotFoundException withId(UUID id) {
        return new {도메인}NotFoundException(ErrorCode.{도메인}_NOT_FOUND, Map.of("id", id));
    }
}
```

### 8. 완료 후

`/write-junit-test {도메인}Service` 실행 제안