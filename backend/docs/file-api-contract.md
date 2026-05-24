# File API Contract

이 문서는 FlowDeck MVP 기준 파일 상세 조회, 현재 내용 저장, 명시적 버전 저장, 버전 복원 API 계약을 정리합니다.

BE3 담당 영역인 WebSocket 파일 저장 알림, 채팅 LOG, 실시간 동기화는 이 문서 범위에서 제외합니다.

파일 WebSocket 이벤트와 Editing Presence 계약은 `docs/realtime-file-collaboration-contract.md`에서 별도로 관리합니다.

---

## 0. 프론트 연동 기본 흐름

프론트는 파일 편집 화면에서 다음 순서로 API를 사용합니다.

1. 파일 트리 조회로 파일/폴더 구조를 표시합니다.
2. 파일 상세 조회로 에디터 초기 상태를 구성합니다.
3. 파일 상세 응답의 `content`를 에디터 초기 내용으로 사용합니다.
4. 파일 상세 응답의 `editRevision`을 클라이언트 상태에 보관합니다.
5. Ctrl+S 또는 일반 저장 시 현재 `editRevision`을 `baseRevision`으로 보냅니다.
6. 저장 성공 응답의 `editRevision`으로 클라이언트 상태를 갱신합니다.
7. 명시적 버전 저장 버튼 클릭 시 버전 저장 API를 호출합니다.
8. 버전 복원 시 파일 상세 조회에서 받은 최신 `editRevision`을 `baseRevision`으로 보냅니다.
9. 파일/폴더 삭제 시 삭제 대상의 최신 `editRevision`을 `expectedRevision`으로 보냅니다.
10. 충돌 응답을 받으면 현재 에디터 내용을 바로 덮어쓰지 않고 사용자 선택을 받습니다.

프론트는 `currentVersion`과 `editRevision`을 서로 다른 값으로 취급해야 합니다.

- `currentVersion`: 사용자가 명시적으로 저장한 버전 번호입니다.
- `editRevision`: 현재 파일 내용 수정 충돌을 감지하기 위한 번호입니다.

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

## 6. 버전 diff 조회

```http
GET /api/projects/{projectId}/files/{fileId}/versions/diff?from={fromVersion}&to={toVersion}
```

### 목적

두 명시적 버전의 저장 내용을 비교해 버전 슬라이드 UI에서 라인 단위 변경점을 표시합니다.

프론트는 사용자가 선택한 버전을 기준으로 이전 버전 또는 비교 대상 버전을 지정해 호출합니다.

### Query parameters

| name | type | required | description |
| --- | --- | --- | --- |
| `from` | number | true | 비교 기준 버전 번호 |
| `to` | number | true | 비교 대상 버전 번호 |

### Response data

```json
{
  "fromVersion": 1,
  "toVersion": 2,
  "addedLines": 2,
  "removedLines": 1,
  "changes": [
    {
      "type": "UNCHANGED",
      "oldLineNumber": 1,
      "newLineNumber": 1,
      "content": "import React from 'react'"
    },
    {
      "type": "ADDED",
      "oldLineNumber": null,
      "newLineNumber": 2,
      "content": "import MonacoEditor from '@monaco-editor/react'"
    },
    {
      "type": "REMOVED",
      "oldLineNumber": 4,
      "newLineNumber": null,
      "content": "  return <div>에디터</div>"
    }
  ]
}
```

### Diff line type

| type | description |
| --- | --- |
| `UNCHANGED` | 양쪽 버전에 동일하게 존재하는 라인 |
| `ADDED` | `to` 버전에 새로 추가된 라인 |
| `REMOVED` | `from` 버전에서 제거된 라인 |

### 동작

- 서버는 두 버전의 `content`를 조회 시점에 비교합니다.
- 파일 버전은 전체 스냅샷으로 저장하며 diff 결과를 별도 테이블에 저장하지 않습니다.
- Myers 기반 라인 diff로 중간 삽입/삭제 이후의 동일 라인을 `UNCHANGED`로 정렬합니다.
- 한 줄 내용 변경은 `REMOVED` 1줄과 `ADDED` 1줄로 응답합니다.
- `addedLines`와 `removedLines`는 각각 `ADDED`, `REMOVED` 라인 수입니다.
- 비교 대상 버전이 없으면 `VERSION_404`를 반환합니다.

---

## 6-1. 파일 타임라인 조회

```http
GET /api/projects/{projectId}/files/{fileId}/versions/timeline?page=0&size=20
```

### 목적

타임라인 모달 최초 진입 시 필요한 버전 카드 메타데이터 목록을 조회합니다.

프론트는 이 응답으로 초기 목록 화면을 그리고, 사용자가 버전을 클릭하면 기존 `버전 상세 조회 API`와 `버전 diff 조회 API`를 조합해 화면을 갱신합니다.

### Query parameters

| name | type | required | default | max | description |
| --- | --- | --- | --- | --- | --- |
| `page` | number | false | 0 | - | 0부터 시작하는 페이지 번호 |
| `size` | number | false | 20 | 100 | 한 페이지에 조회할 버전 수 |

### Response data

```json
{
  "fileId": 1,
  "fileName": "Editor.jsx",
  "totalVersions": 125,
  "page": 0,
  "size": 20,
  "hasNext": true,
  "versions": [
    {
      "versionId": 11,
      "versionNumber": 1,
      "changeMessage": "최초 생성",
      "createdBy": 3,
      "createdByName": "홍길동",
      "createdAt": "2026-05-19T10:00:00Z"
    },
    {
      "versionId": 12,
      "versionNumber": 2,
      "changeMessage": "Monaco 연결",
      "createdBy": 7,
      "createdByName": "김철수",
      "createdAt": "2026-05-19T10:15:00Z"
    }
  ]
}
```

### 동작

- `versions`는 요청한 페이지 범위 안에서 오래된 버전부터 최신 버전까지 오름차순으로 정렬합니다.
- `size`는 최대 100으로 제한합니다.
- `totalVersions`는 전체 버전 수이며 매 페이지 응답에 포함합니다.
- `hasNext`는 현재 페이지 뒤에 추가 페이지가 있는지 여부입니다.
- 요청한 페이지가 마지막 페이지를 초과하면 `totalVersions`는 유지하고 `versions = []`, `hasNext = false`로 응답합니다.
- 이 API는 초기 진입 성능을 위해 버전 `content`와 diff 결과를 포함하지 않습니다.
- 버전 내용은 `GET /api/projects/{projectId}/files/{fileId}/versions/{versionId}`로 조회합니다.
- 버전 diff는 `GET /api/projects/{projectId}/files/{fileId}/versions/diff?from={fromVersion}&to={toVersion}`로 조회합니다.
- 버전이 하나도 없는 파일은 `totalVersions = 0`, `hasNext = false`, `versions = []`로 응답합니다.
- `createdByName`은 작성자 정보가 없으면 `시스템`, 작성자 ID는 있으나 사용자 정보를 찾지 못하면 `알 수 없음`으로 응답합니다.
- 현재는 `Page` 기반으로 `totalVersions`와 `hasNext`를 함께 제공합니다. 버전 수가 매우 많아져 `count query` 비용이 문제가 되면 `Slice` 또는 cursor 기반 조회로 전환을 검토합니다.

---

## 7. 파일 삭제

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

## 8. 파일 이름변경 / 이동

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

## 9. 프론트 에러 분기 기준

프론트는 공통 응답의 `code` 값을 기준으로 UI를 분기합니다.

| code | HTTP status | 대표 상황 | 권장 UI |
| --- | --- | --- | --- |
| `FILE_409` | 409 | 저장/복원/삭제 충돌 | 충돌 모달 표시, 최신 내용 확인 또는 재시도 선택 |
| `FILE_409_1` | 409 | 같은 위치 파일/폴더명 중복 | 이름 중복 안내, 입력값 유지 |
| `FILE_400` | 400 | 폴더를 파일처럼 조회/저장하는 등 타입 오류 | 파일을 다시 선택하도록 안내 |
| `FILE_400_1` | 400 | 빈 검색어 등 잘못된 파일 검색어 | 검색어 입력 안내 |
| `FILE_400_2` | 400 | 자기 자신 또는 하위 폴더로 이동 | 이동 불가 안내 |
| `FILE_400_3` | 400 | 1MB 초과 파일 저장 | 파일 크기 초과 안내 |
| `FILE_404` | 404 | 파일 없음 | 파일 트리 새로고침 안내 |
| `VERSION_404` | 404 | 버전 없음 | 버전 목록 새로고침 안내 |
| `USER_409` | 409 | 마지막 OWNER 프로젝트 보유 상태에서 회원 탈퇴 요청 | 소유권 이전 또는 프로젝트 삭제 안내 |
| `COMMON_400` | 400 | 필수 파라미터 누락 또는 검증 실패 | 요청값 확인 안내 |
| `AUTH_401` | 401 | 인증 없음 또는 만료 | 로그인 또는 토큰 재발급 흐름 |
| `AUTH_403` | 403 | 권한 없음 | 권한 부족 안내 |

`FILE_409` 응답에는 `data`가 포함됩니다.

나머지 단순 에러 응답은 기본적으로 `data`가 없습니다.

회원 탈퇴 성공 후 기존 access token은 blacklist 처리되고 refresh token은 삭제됩니다.
프론트는 탈퇴 성공 응답을 받으면 로컬 인증 상태를 즉시 제거하고 로그인 화면으로 이동합니다.

회원 탈퇴 사용자의 WebSocket presence 즉시 제거는 2차 작업으로 분리되어 있으며,
1차에서는 presence TTL 기반 자연 만료를 사용합니다.

---

## 10. 운영 DB 마이그레이션 주의사항

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

## 11. 추후 검토 항목

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
