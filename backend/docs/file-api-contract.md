# File API Contract

이 문서는 FlowDeck MVP 기준 파일 상세 조회, 현재 내용 저장, 명시적 버전 저장, 버전 복원 API 계약을 정리합니다.

BE3 담당 영역인 WebSocket 파일 저장 알림, 채팅 LOG, 실시간 동기화는 이 문서 범위에서 제외합니다.

파일 WebSocket 이벤트와 Editing Presence 계약은 `docs/realtime-file-collaboration-contract.md`에서 별도로 관리합니다.

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
  "data": {
    "fileId": 1,
    "baseRevision": 4,
    "currentRevision": 5,
    "currentVersion": 2,
    "latestContent": "public class Main {}"
  }
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

### Conflict data

- `fileId`: 충돌이 발생한 파일 ID입니다.
- `baseRevision`: 클라이언트가 요청에 담아 보낸 기준 revision입니다.
- `currentRevision`: 서버에 저장된 현재 `editRevision`입니다.
- `currentVersion`: 현재 명시적 버전 번호입니다.
- `latestContent`: 서버에 저장된 최신 파일 내용입니다.

### 동시 편집 범위

현재 MVP에서는 CRDT/Yjs 기반 실시간 병합과 파일 편집 lock을 제공하지 않습니다.

여러 사용자가 같은 파일을 동시에 편집할 수 있지만,
저장 시점에 `baseRevision`과 서버의 `editRevision`을 비교해
오래된 기준의 저장 요청은 `409 FILE_409`로 거부합니다.

WebSocket 파일 저장 알림, 편집 presence,
저장 전 충돌 가능성 선제 경고는 후속 기능으로 검토합니다.

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

### Request

```json
{
  "baseRevision": 5
}
```

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

- `baseRevision`이 현재 파일의 `editRevision`과 같으면 복원합니다.
- `baseRevision`이 현재 파일의 `editRevision`과 다르면 `FILE_409` 충돌 응답을 반환합니다.
- 선택한 버전의 `content`를 `currentContent`에 반영합니다.
- 복원 이력을 새 `FileVersion`으로 저장합니다.
- `currentVersion`을 1 증가시킵니다.
- `editRevision`을 1 증가시킵니다.

---

## 6. 파일 삭제

```http
DELETE /api/projects/{projectId}/files/{fileId}?expectedRevision=5
```

### 목적

파일 또는 폴더를 삭제합니다.

폴더 삭제 시 하위 파일/폴더와 연결된 버전 데이터도 함께 삭제합니다.

### 동작

- `expectedRevision`은 삭제 대상 파일/폴더의 현재 `editRevision`과 비교합니다.
- `expectedRevision`이 현재 `editRevision`과 같으면 삭제합니다.
- `expectedRevision`이 현재 `editRevision`과 다르면 `FILE_409` 충돌 응답을 반환합니다.
- MVP 기준으로 폴더 삭제 시 하위 파일 전체 revision은 검사하지 않습니다.
- 하위 파일 편집 중 삭제 위험은 후속 WebSocket 알림과 Editing Presence로 보완합니다.

---

## 7. 파일 이름변경 / 이동

파일 이름변경과 이동은 파일 내용 변경이 아니라 파일 트리/메타데이터 변경으로 분류합니다.

MVP 기준으로 이름변경과 이동은 `editRevision`을 증가시키지 않습니다.

`editRevision`은 파일 내용 저장과 버전 복원처럼 `currentContent`가 바뀌는 작업의 충돌 감지에 사용합니다.

따라서 프론트는 이름변경/이동 성공 응답을 기준으로 현재 파일 트리를 갱신합니다.

다른 사용자가 보고 있는 파일 트리 갱신은 후속 WebSocket 이벤트로 보완합니다.

후속 후보 이벤트:

- `file.renamed`
- `file.moved`
- `file.deleted`
- `file.restored`
- `file.saved`

파일 트리 변경 충돌을 더 엄격하게 다룰 필요가 생기면
`metadataRevision` 또는 `treeRevision` 도입을 검토합니다.

---

## 8. 운영 DB 마이그레이션 주의사항

현재 MVP 개발 환경에서는 JPA `ddl-auto` 기준으로 컬럼이 반영될 수 있습니다.

운영 배포 전에는 다음 컬럼에 대해 명시적 마이그레이션이 필요합니다.

### project_files.current_content

- 기존 행 기본값: `''`
- nullable: false
- 현재 파일 내용을 저장합니다.
- 일반 저장 API에서 갱신됩니다.

### project_files.edit_revision

- 기존 행 기본값: `0`
- nullable: false
- 파일 저장 충돌 감지를 위한 수정 번호입니다.
- 파일 저장 또는 버전 복원 성공 시 증가합니다.

### 파일 크기 제한

- 현재 서버 상수 기준 최대 1MB입니다.
- UTF-8 byte 기준으로 계산합니다.
- 추후 운영 설정으로 분리할 수 있습니다.

---

## 9. 추후 검토 항목

- 프론트 충돌 모달 UX
- 편집 presence 및 파일 저장/복원 WebSocket 알림
- 파일 편집 lock 또는 soft lock 도입 여부
- 파일 트리 변경 WebSocket 이벤트 payload
- `metadataRevision` 또는 `treeRevision` 도입 여부
- 자동 저장 on/off 정책
- WebSocket 파일 저장/복원 알림 payload
- `FileVersion publicId` 도입 여부
- 운영 DB 마이그레이션 전략
- 파일 크기 제한값을 운영 설정으로 분리할지 여부
