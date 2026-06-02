# Realtime File Collaboration Contract

이 문서는 FlowDeck WebSocket 기반 파일 이벤트, 프로젝트 채팅, 프로젝트 presence 계약을 정리합니다.

현재 백엔드는 CRDT/Yjs 기반 실시간 병합을 도입하지 않습니다.

파일 충돌 방지는 다음 3단계 흐름으로 관리합니다.

1. Optimistic Locking
2. File Save Notification
3. File Editing Presence
4. Project Presence

---

## 1. 충돌 방지 단계

### 1단계: Optimistic Locking

현재 구현된 단계입니다.

파일 상세 조회 응답의 `editRevision`을 저장, 복원, 삭제 요청의 기준값으로 사용합니다.

- 저장: `baseRevision`
- 복원: `baseRevision`
- 삭제: `expectedRevision`

서버의 현재 `editRevision`과 요청 기준 revision이 다르면 `FILE_409`를 반환합니다.

이 단계는 최종 덮어쓰기를 방지하는 서버 측 안전장치입니다.

### 2단계: File Save Notification

현재 구현된 단계입니다.

한 사용자가 파일 상태를 변경하면 같은 프로젝트 또는 같은 파일을 보고 있는 사용자에게 이벤트를 발행합니다.

이 단계는 충돌을 직접 막기보다 다른 사용자의 변경을 빠르게 인지시키는 역할입니다.

### 3단계: File Editing Presence

현재 구현된 단계입니다.

파일 에디터 진입 시 프론트가 편집 시작 이벤트를 보내면 서버는 Redis TTL 기반으로 현재 편집자를 기록하고
`/topic/projects/{projectId}/files/{fileId}/editing`으로 상태를 발행합니다.

같은 파일에 이미 다른 사용자의 편집 세션이 있으면 새 사용자가 편집권을 선점하지 못하고 기존 편집자 정보가 그대로 반환됩니다.

이 단계는 프론트가 에디터를 읽기 전용으로 전환하거나 저장 버튼을 비활성화하기 위한 단일 편집자 상태입니다.
서버 저장 API의 최종 충돌 방지는 여전히 `editRevision` 기반 optimistic locking이 담당합니다.

### 4단계: Project Presence

Redis + WebSocket 기반으로 누가 프로젝트에 접속 중인지 표시합니다.

이 단계는 저장을 막는 lock이 아닙니다.

프론트 상단 또는 사이드 영역에 프로젝트 접속자를 표시하여 협업 상태를 보여주는 UX입니다.

---

## 2. WebSocket 파일 이벤트

### 공통 목적

파일 생성, 저장, 복원, 삭제, 이름변경, 이동이 발생했을 때 다른 사용자의 화면을 갱신하기 위한 이벤트입니다.

현재 백엔드는 각 작업이 성공적으로 커밋된 뒤 `/topic/projects/{projectId}/files`로 이벤트를 발행합니다.

### 공통 payload

```json
{
  "eventType": "FILE_SAVED",
  "projectId": "project-public-id",
  "fileId": 1,
  "actorId": 10,
  "actorName": "홍길동",
  "editRevision": 5,
  "currentVersion": 2,
  "occurredAt": "2026-05-20T12:00:00Z"
}
```

### 공통 필드

- `eventType`: 파일 이벤트 타입입니다.
- `projectId`: 프로젝트 public id입니다.
- `fileId`: 이벤트 대상 파일 id입니다.
- `actorId`: 이벤트를 발생시킨 사용자 id입니다.
- `actorName`: 이벤트를 발생시킨 사용자 이름입니다.
- `editRevision`: 이벤트 이후 서버의 현재 파일 수정 번호입니다.
- `currentVersion`: 이벤트 이후 현재 명시적 버전 번호입니다.
- `occurredAt`: 이벤트 발생 시각입니다.

---

## 3. 이벤트 타입

### FILE_CREATED

파일 또는 폴더 생성이 성공했을 때 발행합니다.

수신자는 파일 트리를 다시 조회하거나 `newName`, `newParentId`를 사용해 로컬 트리를 갱신할 수 있습니다.

### FILE_SAVED

현재 파일 내용 저장이 성공했을 때 발행합니다.

파일 내용은 payload에 직접 포함하지 않습니다.

수신자는 필요 시 파일 상세 조회 API로 최신 `content`를 다시 가져옵니다.

### FILE_RESTORED

특정 버전 복원이 성공했을 때 발행합니다.

복원은 `currentContent`, `editRevision`, `currentVersion`을 모두 변경합니다.

수신자는 현재 열린 파일이 대상 파일이면 최신 내용을 다시 불러올 수 있게 안내합니다.

### FILE_DELETED

파일 또는 폴더 삭제가 성공했을 때 발행합니다.

폴더 삭제 시 하위 파일/폴더가 함께 삭제될 수 있으므로 `deletedFileIds`를 함께 제공합니다.

```json
{
  "deletedFileIds": [1, 2, 3]
}
```

### FILE_RENAMED

파일 또는 폴더 이름변경이 성공했을 때 발행합니다.

이름변경은 `editRevision`을 증가시키지 않습니다.

```json
{
  "oldName": "Main.java",
  "newName": "App.java"
}
```

### FILE_MOVED

파일 또는 폴더 이동이 성공했을 때 발행합니다.

이동은 `editRevision`을 증가시키지 않습니다.

```json
{
  "oldParentId": 1,
  "newParentId": 2
}
```

---

## 4. 프론트 처리 기준

### 현재 열고 있는 파일이 저장됨

다른 사용자가 같은 파일을 저장하면 상단 또는 모달로 안내합니다.

예시:

```text
다른 사용자가 이 파일을 저장했습니다. 최신 내용을 불러오시겠습니까?
```

프론트 선택지:

- 최신 내용 불러오기
- 내 작업 유지
- 나중에 비교하기

### 현재 열고 있는 파일이 복원됨

복원은 현재 파일 내용을 바꾸는 작업입니다.

수신자는 저장 알림보다 강한 안내를 보여주는 것이 좋습니다.

### 현재 열고 있는 파일이 삭제됨

삭제된 파일을 열고 있던 사용자는 에디터를 닫거나 읽기 전용 상태로 전환합니다.

파일 트리는 즉시 갱신합니다.

### 현재 열고 있는 파일이 이름변경/이동됨

에디터 내용은 유지합니다.

상단 파일명, 경로, 파일 트리를 갱신합니다.

---

## 5. Project Presence

### 목적

같은 프로젝트에 누가 접속 중인지 표시합니다.

충돌을 강제로 막지는 않습니다.

최종 충돌 방지는 `editRevision` 기반 optimistic locking이 담당합니다.

### REST 조회

```http
GET /api/projects/{projectId}/presence
```

### STOMP destination

```text
/app/projects/{projectId}/presence/join
/app/projects/{projectId}/presence/heartbeat
/topic/projects/{projectId}/presence
```

### Response payload

```json
{
  "projectId": "project-public-id",
  "connectedCount": 2,
  "members": [
    {
      "userId": 10,
      "userName": "홍길동",
      "sessionCount": 1,
      "lastSeenAt": "2026-05-20T12:00:00Z"
    }
  ],
  "occurredAt": "2026-05-20T12:00:00Z"
}
```

### Redis key

```text
presence:project:{projectId}
ws:session:{sessionId}
presence:user:{userId}
```

- `presence:project:{projectId}`: 프로젝트별 sessionId sorted set. score는 마지막 heartbeat epoch millis입니다.
- `ws:session:{sessionId}`: `projectId`, `userId`, `lastSeenAt`를 저장하며 TTL 30초를 사용합니다.
- `presence:user:{userId}`: 사용자별 sessionId 보조 인덱스이며 TTL 60초를 사용합니다.

heartbeat가 끊기면 `ws:session:{sessionId}` TTL과 stale session pruning으로 접속 상태에서 제거됩니다.

### 프론트 표시 기준

상단 고정 영역에 표시합니다.

예시:

```text
김철수님이 프로젝트에 접속 중입니다.
```

여러 명일 경우:

```text
김철수님 외 2명이 프로젝트에 접속 중입니다.
```

현재 payload는 프로젝트 접속자 기준이므로 파일명/편집 상태는 포함하지 않습니다.

---

## 6. File Editing Presence

### 목적

같은 파일을 누가 편집 중인지 표시하고, 프론트에서 다른 사용자의 동시 수정을 막기 위한 상태입니다.

프론트는 파일 에디터를 열어 편집 가능한 상태로 진입할 때 `start`를 보내고, 편집 중에는 `heartbeat`를 주기적으로 보냅니다.
에디터를 닫거나 다른 파일로 이동할 때 `stop`을 보냅니다.

### STOMP destination

```text
/app/projects/{projectId}/files/{fileId}/editing/start
/app/projects/{projectId}/files/{fileId}/editing/heartbeat
/app/projects/{projectId}/files/{fileId}/editing/stop
/topic/projects/{projectId}/files/{fileId}/editing
```

### Response payload

편집자가 있는 경우:

```json
{
  "projectId": "project-public-id",
  "fileId": 1,
  "editing": true,
  "editorId": 10,
  "editorName": "홍길동",
  "editorSessionId": "stomp-session-id",
  "lastSeenAt": "2026-05-20T12:00:00Z",
  "occurredAt": "2026-05-20T12:00:00Z"
}
```

편집자가 없는 경우:

```json
{
  "projectId": "project-public-id",
  "fileId": 1,
  "editing": false,
  "editorId": null,
  "editorName": null,
  "editorSessionId": null,
  "lastSeenAt": null,
  "occurredAt": "2026-05-20T12:00:00Z"
}
```

### Redis key

```text
presence:file:{projectId}:{fileId}
```

- TTL은 30초입니다.
- `heartbeat`가 끊기면 Redis TTL 만료 후 편집 상태가 해제됩니다.
- 같은 사용자 또는 같은 STOMP session은 `heartbeat`로 TTL을 연장할 수 있습니다.
- 다른 사용자가 `start`를 보내면 기존 편집자 정보가 반환됩니다.

### 프론트 처리 기준

파일 에디터 진입 시:

1. `/topic/projects/{projectId}/files/{fileId}/editing`을 구독합니다.
2. `/app/projects/{projectId}/files/{fileId}/editing/start`를 보냅니다.
3. 응답 payload의 `editing`이 `true`이고 `editorId`가 내 사용자 id가 아니면 에디터를 읽기 전용으로 전환합니다.

편집 중:

- 10초 이하 간격으로 `heartbeat`를 보냅니다.
- 내가 편집자인 상태에서 `editing=false` 또는 다른 `editorId`가 내려오면 저장 버튼을 비활성화하고 재진입을 유도합니다.

에디터 이탈 시:

- `/app/projects/{projectId}/files/{fileId}/editing/stop`을 보냅니다.

표시 예시:

```text
김철수님이 이 파일을 편집 중입니다.
```

---

## 7. 멤버 권한 변경 WebSocket

### 목적

프로젝트 멤버 권한이 변경된 대상 사용자가 즉시 자신의 권한 변경을 인지하기 위한 개인 알림입니다.

### STOMP destination

```text
/user/queue/project-members
```

### Response payload

```json
{
  "eventType": "MEMBER_ROLE_CHANGED",
  "projectId": "project-public-id",
  "memberId": 1,
  "userId": "user-public-id",
  "previousRole": "VIEWER",
  "currentRole": "EDITOR",
  "actorId": 10,
  "actorName": "홍길동",
  "occurredAt": "2026-05-20T12:00:00Z"
}
```

### 프론트 처리 기준

- 로그인 후 `/user/queue/project-members`를 구독합니다.
- `MEMBER_ROLE_CHANGED`를 받으면 해당 프로젝트의 권한 상태를 갱신합니다.
- `currentRole`이 `VIEWER`가 되면 편집 UI와 저장 버튼을 즉시 비활성화합니다.
- 이미 열린 파일 에디터가 있으면 파일 편집 presence `stop`을 보내고 읽기 전용으로 전환합니다.

---

## 8. 프로젝트 메시지 WebSocket

### STOMP destination

```text
/app/projects/{projectId}/messages
/topic/projects/{projectId}/messages
```

### REST API

```http
GET /api/projects/{projectId}/messages
GET /api/projects/{projectId}/messages/search
DELETE /api/projects/{projectId}/messages/{messageId}
```

프로젝트 메시지는 `CHAT`과 `LOG` 타입을 사용합니다.
사용자 채팅은 STOMP 또는 REST 조회 흐름으로 사용하고, 시스템/파일 이벤트성 기록은 `LOG` 메시지로 남길 수 있습니다.

---

## 9. 도입하지 않는 범위

MVP에서는 다음 기능을 도입하지 않습니다.

- CRDT/Yjs 기반 실시간 코드 병합
- Redis 기반 강제 편집 lock
- 서버 저장 API에서 먼저 편집한 사용자만 저장 가능한 강제 단일 편집자 정책
- 라인별 커서/선택 영역 공유

이유:

- lock 해제, TTL 연장, 브라우저 종료, 네트워크 단절 처리가 필요합니다.
- 프론트 UX 합의가 필요합니다.
- MVP에서는 `editRevision` 충돌 방지, 파일 WebSocket 이벤트, 파일 편집 presence로 덮어쓰기 위험과 협업 상태 인지 문제를 줄입니다.

---

## 10. 후속 결정 필요 사항

- 채팅 LOG와 파일 이벤트를 같은 timeline에 보여줄지 여부
- 파일 단위 presence 프론트 UI 위치
- 파일 이벤트와 프로젝트 메시지 LOG의 중복 표시 정책
- 서버 저장 API까지 편집 lock을 강제할지 여부
