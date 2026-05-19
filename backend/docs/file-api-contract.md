# File API Contract

이 문서는 FlowDeck MVP 기준 파일 상세 조회, 현재 내용 저장, 명시적 버전 저장, 버전 복원 API 계약을 정리합니다.

BE3 담당 영역인 WebSocket 파일 저장 알림, 채팅 LOG, 실시간 동기화는 이 문서 범위에서 제외합니다.

---

## 1. 파일 상세 조회

```http
GET /api/projects/{projectId}/files/{fileId}
```

### 목적

에디터에서 파일을 열 때 필요한 메타데이터와 현재 파일 내용을 한 번에 조회합니다.

### Response data

```json
{
  "fileId": 1,
  "parentId": null,
  "name": "Main.java",
  "type": "FILE",
  "currentVersion": 2,
  "editRevision": 4,
  "content": "public class Main {}",
  "createdAt": "2026-05-19T10:00:00Z",
  "updatedAt": "2026-05-19T10:10:00Z"
}
```

### 프론트 사용 기준

- `content`: Monaco Editor 초기 내용으로 사용합니다.
- `editRevision`: 다음 저장 요청의 `baseRevision`으로 사용합니다.
- `currentVersion`: 명시적으로 저장된 버전 번호입니다.

---

## 2. 현재 내용 저장

```http
PUT /api/projects/{projectId}/files/{fileId}
```

### 목적

Ctrl+S 또는 일반 저장 시 현재 작업 내용을 저장합니다.

이 API는 `FileVersion`을 생성하지 않습니다.

### Request

```json
{
  "content": "public class Main {}",
  "baseRevision": 4,
  "changeMessage": "현재 내용 저장"
}
```

### Response data

```json
{
  "fileId": 1,
  "name": "Main.java",
  "currentVersion": 2,
  "editRevision": 5,
  "updatedAt": "2026-05-19T10:12:00Z"
}
```

### 동작

- `baseRevision`이 현재 파일의 `editRevision`과 같으면 저장합니다.
- 저장 전 `content` 크기를 UTF-8 byte 기준으로 검증합니다.
- MVP 기준 단일 파일 최대 크기는 1MB입니다.
- 제한을 초과하면 `FILE_400_3` 에러를 반환합니다.
- 저장 성공 시 `currentContent`를 갱신합니다.
- 저장 성공 시 `editRevision`을 1 증가시킵니다.
- `currentVersion`은 증가하지 않습니다.
- `FileVersion`은 생성하지 않습니다.

---

## 3. 저장 충돌

### 조건

저장 요청의 `baseRevision`이 서버의 현재 `editRevision`과 다르면 충돌로 처리합니다.

### Error response

```json
{
  "success": false,
  "code": "FILE_409",
  "message": "파일이 다른 사용자에 의해 수정되었습니다.",
  "data": null
}
```

### HTTP status

```http
409 Conflict
```

### 프론트 처리 기준

- 충돌 안내 모달을 보여줍니다.
- 사용자가 최신 내용을 다시 불러올 수 있게 합니다.
- 현재 작성 중인 내용을 보존할 UX는 프론트에서 별도 결정합니다.

---

## 4. 명시적 버전 저장

```http
POST /api/projects/{projectId}/files/{fileId}/versions
```

### 목적

사용자가 의미 있는 시점의 코드를 버전 스냅샷으로 저장합니다.

### Request

```json
{
  "changeMessage": "Monaco Editor 연결"
}
```

### Response data

```json
{
  "fileId": 1,
  "name": "Main.java",
  "currentVersion": 3,
  "editRevision": 5,
  "updatedAt": "2026-05-19T10:15:00Z"
}
```

### 동작

- 현재 `currentContent`를 기준으로 `FileVersion`을 생성합니다.
- `currentVersion`을 1 증가시킵니다.
- `editRevision`은 증가하지 않습니다.

---

## 5. 버전 복원

```http
POST /api/projects/{projectId}/files/{fileId}/versions/{versionId}/restore
```

### 목적

특정 버전의 내용을 현재 파일 내용으로 복원합니다.

### Response data

```json
{
  "fileId": 1,
  "name": "Main.java",
  "currentVersion": 4,
  "editRevision": 6,
  "updatedAt": "2026-05-19T10:20:00Z"
}
```

### 동작

- 선택한 버전의 `content`를 `currentContent`에 반영합니다.
- 복원 이력을 새 `FileVersion`으로 저장합니다.
- `currentVersion`을 1 증가시킵니다.
- `editRevision`을 1 증가시킵니다.

---

## 6. 추후 검토 항목

- 충돌 응답에 서버 최신 `editRevision`과 최신 `content`를 포함할지 여부
- 프론트 충돌 모달 UX
- 자동 저장 on/off 정책
- WebSocket 파일 저장/복원 알림 payload
- `FileVersion publicId` 도입 여부
- 운영 DB 마이그레이션 전략
- 파일 크기 제한값을 운영 설정으로 분리할지 여부
