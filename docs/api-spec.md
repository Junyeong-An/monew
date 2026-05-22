# 모뉴(MoNew) API 명세

> 프론트엔드 소스코드(`fe/src/api/`) 기반으로 정리한 실제 구현 기준 명세입니다.

---

## 공통

### Base URL
```
/api
```

### 인증 헤더
JWT 없음. 로그인 후 응답으로 받은 userId를 프론트가 sessionStorage에 저장하고, 인증이 필요한 요청마다 헤더에 포함합니다.

```
Monew-Request-User-ID: {userId}
```

> ⚠️ 헤더명 주의: `Monew-Request-User-ID` (대소문자 정확히 일치)

### 커서 페이지네이션

**공통 요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `cursor` | string | N | 정렬 기준값 커서 |
| `after` | string | N | ID 기반 커서 (동일 정렬값 구분용) |
| `limit` | number | Y | 페이지 크기 |
| `orderBy` | string | Y | 정렬 기준 (도메인별 상이) |
| `direction` | `ASC` \| `DESC` | Y | 정렬 방향 |

**공통 응답 구조**

```json
{
  "content": [],
  "nextCursor": "string | null",
  "nextAfter": "string | null",
  "size": 20,
  "totalElements": 100,
  "hasNext": true
}
```

> `cursor` + `after` 복합 커서 방식. 정렬값이 동일한 항목이 있어도 정확한 위치를 특정할 수 있습니다.

### 모든 ID 타입
UUID 형식: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

---

## 1. 사용자 (User)

### 공통 응답 타입
```typescript
User {
  id: UUID
  email: string
  nickname: string
  createdAt: string  // ISO 8601
}
```

---

### `POST /api/users` — 회원가입

**Request Body**
```json
{
  "email": "user@example.com",
  "nickname": "홍길동",
  "password": "password123"
}
```

**Response `200`**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

---

### `POST /api/users/login` — 로그인

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response `200`**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

> 응답의 `id`를 프론트가 sessionStorage에 저장 후 이후 요청의 `Monew-Request-User-ID` 헤더로 사용합니다.

---

### `PATCH /api/users/{userId}` — 닉네임 수정

**Request Body**
```json
{
  "nickname": "새닉네임"
}
```

**Response `200`** — `User`

---

### `DELETE /api/users/{userId}` — 논리 삭제

**Response `204`** — 없음

---

### `DELETE /api/users/{userId}/hard` — 물리 삭제

**Response `204`** — 없음

---

## 2. 관심사 (Interest)

### 공통 응답 타입
```typescript
InterestListItem {
  id: UUID
  name: string
  keywords: string[]
  subscriberCount: number
  subscribedByMe: boolean
}
```

---

### `GET /api/interests` — 목록 조회

**Header** `Monew-Request-User-ID` 필수 (`subscribedByMe` 판단용)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `keyword` | string | N | 이름·키워드 부분일치 검색 |
| `orderBy` | `name` \| `subscriberCount` | Y | 정렬 기준 |
| `direction` | `ASC` \| `DESC` | Y | 정렬 방향 |
| `cursor` | string | N | 커서 |
| `after` | string | N | ID 커서 |
| `limit` | number | Y | 페이지 크기 |

**Response `200`** — `CursorPageResponse<InterestListItem>`

---

### `POST /api/interests` — 관심사 등록

**Request Body**
```json
{
  "name": "AI",
  "keywords": ["인공지능", "머신러닝", "딥러닝"]
}
```

**Response `201`** — `InterestListItem`

---

### `PATCH /api/interests/{interestId}` — 키워드 수정

**Request Body**
```json
{
  "keywords": ["인공지능", "ChatGPT"]
}
```

**Response `200`** — `InterestListItem`

---

### `DELETE /api/interests/{interestId}` — 물리 삭제

**Response `204`** — 없음

---

### `POST /api/interests/{interestId}/subscriptions` — 구독

**Header** `Monew-Request-User-ID` 필수

**Request Body** 없음

**Response `201`**
```json
{
  "id": "uuid",
  "interestId": "uuid",
  "interestName": "AI",
  "interestKeywords": ["인공지능", "머신러닝"],
  "interestSubscriberCount": 42,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

---

### `DELETE /api/interests/{interestId}/subscriptions` — 구독 취소

**Header** `Monew-Request-User-ID` 필수

**Response `204`** — 없음

---

## 3. 뉴스 기사 (Article)

### 공통 응답 타입
```typescript
ArticleListItem {
  id: UUID
  source: string
  sourceUrl: string
  title: string
  publishDate: string        // ISO 8601
  summary: string
  commentCount: number
  viewCount: number
  viewedByMe: boolean
}
```

---

### `GET /api/articles` — 목록 조회

**Header** `Monew-Request-User-ID` 필수 (`viewedByMe` 판단용)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `keyword` | string | N | 제목·요약 부분일치 검색 |
| `interestId` | UUID | N | 관심사 필터 |
| `sourceIn` | string[] | N | 출처 필터 (복수 선택 가능) |
| `publishDateFrom` | string | N | 날짜 범위 시작 (ISO 8601) |
| `publishDateTo` | string | N | 날짜 범위 끝 (ISO 8601) |
| `orderBy` | `publishDate` \| `viewCount` \| `commentCount` | Y | 정렬 기준 |
| `direction` | `ASC` \| `DESC` | Y | 정렬 방향 |
| `cursor` | string | N | 커서 |
| `after` | string | N | ID 커서 |
| `limit` | number | Y | 페이지 크기 |

**Response `200`** — `CursorPageResponse<ArticleListItem>`

---

### `GET /api/articles/{articleId}` — 단건 조회

**Header** `Monew-Request-User-ID` 필수

**Response `200`** — `ArticleListItem`

---

### `GET /api/articles/sources` — 출처 목록 조회

**Response `200`**
```json
["naver", "hankyung", "chosun", "yonhap"]
```

---

### `POST /api/articles/{articleId}/article-views` — 조회수 등록

**Header** `Monew-Request-User-ID` 필수

**Request Body** 없음

**Response `201`**
```json
{
  "id": "uuid",
  "viewedBy": "userId",
  "createdAt": "2024-01-01T00:00:00Z",
  "articleId": "uuid",
  "source": "naver",
  "sourceUrl": "https://...",
  "articleTitle": "기사 제목",
  "articlePublishedDate": "2024-01-01T00:00:00Z",
  "articleSummary": "기사 요약",
  "articleCommentCount": 5,
  "articleViewCount": 100
}
```

---

### `GET /api/articles/restore` — 유실 기사 복구

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `from` | string | Y | 복구 시작 날짜 (ISO 8601) |
| `to` | string | Y | 복구 종료 날짜 (ISO 8601) |

**Response `200`**
```json
{
  "restoreDate": "2024-01-01",
  "restoredArticleIds": ["uuid1", "uuid2"],
  "restoredArticleCount": 2
}
```

---

### `DELETE /api/articles/{articleId}` — 논리 삭제

**Response `204`** — 없음

---

### `DELETE /api/articles/{articleId}/hard` — 물리 삭제

**Response `204`** — 없음

---

## 4. 댓글 (Comment)

### 공통 응답 타입
```typescript
CommentItem {
  id: UUID
  articleId: UUID
  userId: UUID
  userNickname: string
  content: string
  likeCount: number
  likedByMe: boolean
  createdAt: string   // ISO 8601
}
```

---

### `GET /api/comments` — 기사별 댓글 목록 조회

**Header** `Monew-Request-User-ID` 필수 (`likedByMe` 판단용)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `articleId` | UUID | Y | 기사 ID |
| `orderBy` | `createdAt` \| `likeCount` | Y | 정렬 기준 |
| `direction` | `ASC` \| `DESC` | Y | 정렬 방향 |
| `cursor` | string | N | 커서 |
| `after` | string | N | ID 커서 |
| `limit` | number | Y | 페이지 크기 |

**Response `200`** — `CursorPageResponse<CommentItem>`

---

### `POST /api/comments` — 댓글 등록

**Request Body**
```json
{
  "articleId": "uuid",
  "userId": "uuid",
  "content": "댓글 내용입니다."
}
```

**Response `201`** — `CommentItem`

---

### `PATCH /api/comments/{commentId}` — 댓글 수정

**Header** `Monew-Request-User-ID` 필수

**Request Body**
```json
{
  "content": "수정된 댓글 내용"
}
```

**Response `200`** — `CommentItem`

---

### `DELETE /api/comments/{commentId}` — 논리 삭제

**Response `204`** — 없음

---

### `DELETE /api/comments/{commentId}/hard` — 물리 삭제

**Response `204`** — 없음

---

### `POST /api/comments/{commentId}/comment-likes` — 좋아요

**Header** `Monew-Request-User-ID` 필수

**Request Body** 없음

**Response `201`**
```json
{
  "id": "uuid",
  "likedBy": "userId",
  "createdAt": "2024-01-01T00:00:00Z",
  "commentId": "uuid",
  "articleId": "uuid",
  "articleTitle": "기사 제목",
  "commentUserId": "uuid",
  "commentUserNickname": "작성자닉네임",
  "commentContent": "댓글 내용",
  "commentLikeCount": 10,
  "commentCreatedAt": "2024-01-01T00:00:00Z"
}
```

---

### `DELETE /api/comments/{commentId}/comment-likes` — 좋아요 취소

**Header** `Monew-Request-User-ID` 필수

**Response `204`** — 없음

---

## 5. 알림 (Notification)

### 공통 응답 타입
```typescript
NotificationsItem {
  id: UUID
  createdAt: string
  updatedAt: string
  confirmed: boolean
  userId: UUID
  content: string
  resourceType: "interest" | "comment"
  resourceId: UUID
}
```

---

### `GET /api/notifications` — 알림 목록 조회 (미확인만)

**Header** `Monew-Request-User-ID` 필수

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `cursor` | string | N | 커서 |
| `after` | string | N | ID 커서 |
| `limit` | number | Y | 페이지 크기 |

**Response `200`** — `CursorPageResponse<NotificationsItem>`

---

### `PATCH /api/notifications/{notificationId}` — 단건 확인

**Header** `Monew-Request-User-ID` 필수

**Request Body** 없음

**Response `204`** — 없음

---

### `PATCH /api/notifications` — 전체 확인

**Header** `Monew-Request-User-ID` 필수

**Request Body** 없음

**Response `204`** — 없음

---

## 6. 활동 내역 (UserActivity)

### `GET /api/user-activities/{userId}` — 활동 내역 조회

**Header** 불필요

**Response `200`**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "nickname": "홍길동",
  "createdAt": "2024-01-01T00:00:00Z",
  "subscriptions": [
    {
      "id": "uuid",
      "interestId": "uuid",
      "interestName": "AI",
      "interestKeywords": ["인공지능"],
      "interestSubscriberCount": 42,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "comments": [
    {
      "id": "uuid",
      "articleId": "uuid",
      "articleTitle": "기사 제목",
      "userId": "uuid",
      "userNickname": "홍길동",
      "content": "댓글 내용",
      "likeCount": 3,
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "commentLikes": [
    {
      "id": "uuid",
      "createdAt": "2024-01-01T00:00:00Z",
      "commentId": "uuid",
      "articleId": "uuid",
      "articleTitle": "기사 제목",
      "commentUserId": "uuid",
      "commentUserNickname": "작성자닉네임",
      "commentContent": "댓글 내용",
      "commentLikeCount": 10,
      "commentCreatedAt": "2024-01-01T00:00:00Z"
    }
  ],
  "articleViews": [
    {
      "id": "uuid",
      "viewedBy": "userId",
      "createdAt": "2024-01-01T00:00:00Z",
      "articleId": "uuid",
      "source": "naver",
      "sourceUrl": "https://...",
      "articleTitle": "기사 제목",
      "articlePublishedDate": "2024-01-01T00:00:00Z",
      "articleSummary": "기사 요약",
      "articleCommentCount": 5,
      "articleViewCount": 100
    }
  ]
}
```

> `comments`에는 `likedByMe` 필드가 없습니다 (활동 내역 조회이므로).  
> `commentLikes`에는 `likedBy` 필드가 없습니다.  
> 각각 최대 10건.

---

## 도메인별 `Monew-Request-User-ID` 헤더 필요 여부 요약

| 엔드포인트 | 헤더 필요 | 용도 |
|-----------|----------|------|
| `POST /users`, `POST /users/login` | N | 인증 전 |
| `PATCH /users/{id}`, `DELETE /users/{id}*` | 미전송 (프론트 코드 기준) | — |
| `GET /interests` | Y | `subscribedByMe` |
| `POST /interests/{id}/subscriptions` | Y | 구독자 식별 |
| `DELETE /interests/{id}/subscriptions` | Y | 구독자 식별 |
| `GET /articles`, `GET /articles/{id}` | Y | `viewedByMe` |
| `POST /articles/{id}/article-views` | Y | 중복 제거 |
| `GET /comments` | Y | `likedByMe` |
| `PATCH /comments/{id}` | Y | 본인 검증 |
| `POST/DELETE /comments/{id}/comment-likes` | Y | 좋아요 주체 |
| `GET /notifications` | Y | 사용자 알림 조회 |
| `PATCH /notifications*` | Y | 사용자 알림 확인 |
| `GET /user-activities/{id}` | N | userId가 path에 있음 |
