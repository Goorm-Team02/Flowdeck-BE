# Realtime File Collaboration Contract

이 문서는 FlowDeck MVP 이후 WebSocket 기반 파일 협업 기능을 연결하기 위한 계약 초안입니다.

현재 백엔드는 CRDT/Yjs 기반 실시간 병합을 도입하지 않습니다.

파일 충돌 방지는 다음 3단계 흐름으로 관리합니다.

1. Optimistic Locking
2. File Save Notification
3. Editing Presence

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

### 3단계: Editing Presence

Redis + WebSocket 기반으로 누가 파일을 보고 있거나 편집 중인지 표시합니다.

이 단계는 저장을 막는 lock이 아닙니다.

프론트 상단에 편집자 상태를 고정 표시하여 충돌 가능성을 줄이는 UX입니다.

---

## 2. WebSocket 파일 이벤트

### 공통 목적

파일 저장, 복원, 삭제, 이름변경, 이동이 발생했을 때 다른 사용자의 화면을 갱신하기 위한 이벤트입니다.

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

폴더 삭제 시 하위 파일/폴더가 함께 삭제될 수 있으므로 payload 확장을 검토할 수 있습니다.

후보 확장:

```json
{
  "deletedFileIds": [1, 2, 3]
}
```

### FILE_RENAMED

파일 또는 폴더 이름변경이 성공했을 때 발행합니다.

이름변경은 `editRevision`을 증가시키지 않습니다.

후보 확장:

```json
{
  "oldName": "Main.java",
  "newName": "App.java"
}
```

### FILE_MOVED

파일 또는 폴더 이동이 성공했을 때 발행합니다.

이동은 `editRevision`을 증가시키지 않습니다.

후보 확장:

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

## 5. Editing Presence

### 목적

같은 파일을 누가 보고 있거나 편집 중인지 표시합니다.

충돌을 강제로 막지는 않습니다.

최종 충돌 방지는 `editRevision` 기반 optimistic locking이 담당합니다.

### 상태

```text
VIEWING
EDITING
```

- `VIEWING`: 사용자가 파일을 열고 있습니다.
- `EDITING`: 사용자가 최근에 파일 내용을 입력했습니다.

### Redis key 후보

```text
presence:file:{fileId}
```

value 후보:

```json
{
  "projectId": "project-public-id",
  "fileId": 1,
  "userId": 10,
  "userName": "홍길동",
  "status": "EDITING",
  "lastSeenAt": "2026-05-20T12:00:00Z"
}
```

### TTL / heartbeat 후보

- TTL: 30초
- heartbeat: 10초
- 마지막 입력 후 10초 동안 `EDITING`
- 입력이 없으면 `VIEWING`으로 전환
- heartbeat가 끊기면 Redis TTL로 자동 제거

### 프론트 표시 기준

상단 고정 영역에 표시합니다.

예시:

```text
김철수님이 이 파일을 편집 중입니다.
```

여러 명일 경우:

```text
김철수님 외 2명이 이 파일을 보고 있습니다.
```

---

## 6. 도입하지 않는 범위

MVP에서는 다음 기능을 도입하지 않습니다.

- CRDT/Yjs 기반 실시간 코드 병합
- Redis 기반 강제 편집 lock
- 먼저 편집한 사용자만 저장 가능한 단일 편집자 정책
- 라인별 커서/선택 영역 공유

이유:

- lock 해제, TTL 연장, 브라우저 종료, 네트워크 단절 처리가 필요합니다.
- 프론트 UX 합의가 필요합니다.
- MVP에서는 `editRevision` 충돌 방지와 WebSocket 알림만으로도 덮어쓰기 위험을 줄일 수 있습니다.

---

## 7. 후속 결정 필요 사항

- BE3 WebSocket destination naming
- 프로젝트 단위 구독과 파일 단위 구독 중 선택
- 파일 이벤트 payload 최종 필드
- 채팅 LOG와 파일 이벤트를 같은 timeline에 보여줄지 여부
- Editing Presence Redis 자료구조
- 프론트 상단 presence UI 위치
- 파일 삭제 시 하위 파일 id payload 포함 여부
