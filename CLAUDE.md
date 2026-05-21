# CLAUDE.md

> **IMPORTANT**: Java 코드를 작성하기 전에 반드시 아래 "절대 하지 말 것" 전체를 읽고 준수할 것.
> 위반 시 hook이 경고를 출력하며, PR 리뷰에서 즉시 reject 사유가 됨.

## 절대 하지 말 것

- `FetchType.EAGER` — LAZY + fetch join 필수
- `@Transactional` in Controller — Service 레이어에서만
- `System.out.println` / `e.printStackTrace()` — `@Slf4j` + log 레벨 사용
- Entity에 setter — 상태 변경은 의도를 드러내는 메서드명으로
- Entity에 `@Builder` — 정적 팩토리 메서드 사용
- `Long` PK — UUID만 사용
- Service에서 Entity→DTO 직접 변환 — MapStruct Mapper 사용
- 성공 응답을 래퍼로 감싸기 — DTO 직접 반환. 에러만 `ErrorResponse`
- 헤더 오타: 반드시 `Monew-Request-User-ID` (M 대문자, onew 소문자)
- `deletedAt`/`isDeleted` 혼용 — `deletedAt`(Instant) null 여부만 사용
- `@Autowired` 필드 주입 — `@RequiredArgsConstructor` + 생성자 주입
- `Optional.get()` 직접 호출 — `orElseThrow()`
- N+1 쿼리 — fetch join / `@EntityGraph` 사용
- `save()` 반환값 무시 — 반환된 영속 엔티티 사용
- `@SpringBootTest` 남발 — 슬라이스 테스트 사용
- 테스트 간 상태 공유 — `@BeforeEach` 초기화 필수
- Controller에 비즈니스 로직 — Service 위임
- `String`으로 UUID 파라미터 — `UUID` 타입 직접 사용
- `@Scheduled` 메서드에 직접 로직 — Service 위임
- MongoDB/PostgreSQL 단일 트랜잭션 혼용 — 별도 트랜잭션 분리
- 커밋에 AI co-author 문구 — `Co-authored-by:` 등 AI 귀속 금지

## Project Overview

**모뉴(MoNew)** — 뉴스 API 통합 + 소셜 기능(댓글·좋아요) Spring Boot 서비스. 2026-05-22 ~ 06-16, 팀원 7명.

스택: Spring Boot, PostgreSQL, MongoDB(심화), springdoc-openapi, Spring Batch(심화), AWS S3/ECS

## Package Structure

```
com.sprint.mission.monew/
├── common/
│   ├── config/       # JpaConfig, QuerydslConfig, MongoConfig, S3Config, SwaggerConfig
│   ├── exception/    # MonewException, ErrorCode, GlobalExceptionHandler
│   ├── response/     # ErrorResponse, CursorPageResponse<T>
│   └── filter/       # MdcLoggingFilter
├── domain/           # user/ interest/ article/ comment/ notification/ useractivity/
├── external/         # naver/ rss/
└── batch/            # 심화 — NewsCollect, NotificationClean, ArticleBackup, LogUpload
```

각 도메인: `controller/(api/) → service/ → repository/(querydsl/)`, entity/, dto/, exception/, mapper/ 서브패키지 분리

## Commands

```bash
./gradlew test
./gradlew test --tests "com.sprint.mission.monew.domain.user.UserServiceTest"
./gradlew jacocoTestReport   # 커버리지 80% 이상 필수
./gradlew bootRun
```

## Key Rules (→ 상세: docs/conventions.md)

**Entity**: UUID PK, Instant 시간, `deletedAt` 소프트딜리트, LAZY fetch, `@EnableJpaAuditing`

**Service**: 클래스에 `@Transactional(readOnly=true)` 기본, 변경 메서드만 `@Transactional` 재정의

**예외**: `MonewException → ErrorCode enum → 팩토리 메서드` (`throw UserNotFoundException.withId(userId)`)

**테스트**: JUnit5, `@Nested`, 한글 메서드명, `given/when/then` 주석

**TDD 커밋**: `test(red):` 실패 테스트 → `test(green):` 구현 → `refactor:` 정리 (각 단계 별도 커밋 필수)

**일반 커밋**: 제목 한글 필수, body 선택 (`feat: 사용자 회원가입 구현`)
태그: `feat:` `fix:` `refactor:` `docs:` `test:` `chore:` `batch:` `deploy:`
> Squash and Merge이므로 PR 제목 = squash 커밋 메시지. 중간 커밋 메시지는 참고용.

## Key Files

- `docs/draft/milestones.md` — 전체 이슈 목록 (Phase별)
- `docs/api-docs.json` — OpenAPI 3.1.0 명세 (29개 엔드포인트)
- `.claude/issue_config.json` — 담당자·라벨·마일스톤 설정

## Claude Skills

```
/implement-domain {도메인}   # Entity→Service→Controller 전체 스캐폴딩
/write-junit-test {클래스}   # JUnit5 테스트 생성 + test(red): 커밋
/create-issue {키워드}       # milestones.md에서 이슈 검색 후 GitHub 생성
/start-issue {키워드}        # 이슈 찾아 브랜치 생성
/create-pr                   # 현재 브랜치 PR 자동 생성
/fix-review                  # PR 리뷰 코멘트 반영
/java-style                  # Google Java Style 준수 여부 점검
/sync-claude                 # claude-setup 브랜치에서 최신 설정 동기화
/remove-gitkeep              # 실제 파일이 생긴 디렉토리의 .gitkeep 제거
```

## Git Workflow

브랜치: `main → dev → {prefix}/{domain}/{description}`
(예: `feat/user/register`, `fix/article/duplicate-url`)
prefix: `feat` `fix` `refactor` `docs` `test` `chore` `batch` `deploy`

워크플로우:
1. Issue 등록
2. `git switch dev && git pull upstream dev && git push origin dev`
3. `git switch -c feat/user/register`
4. 개발
5. PR 전 `git pull upstream dev` → conflict 확인
6. `git push origin feat/user/register`
7. PR 생성: `dev ← feat/user/register`, Squash and Merge
   - 제목: `feat: 사용자 회원가입 구현` (squash 커밋 메시지와 동일)
   - **2인 이상** 리뷰 승인 + `Closes #이슈번호`

## API Spec

29개 엔드포인트: `docs/api-docs.json` (OpenAPI 3.1.0)

인증: `Monew-Request-User-ID` 헤더로 userId 전달 (JWT 없음)

커서 페이지네이션: `cursor`(정렬값) + `after`(ID) 복합 커서, 응답은 `CursorPageResponse<T>`