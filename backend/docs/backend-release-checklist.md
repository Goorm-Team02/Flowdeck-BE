# Backend Release Checklist

FlowDeck 백엔드 배포 전 확인해야 할 운영 체크리스트입니다.

이 문서는 MVP 개발 환경과 운영 배포 환경의 차이를 명확히 하고, 배포 전 누락되기 쉬운 설정과 검증 항목을 정리하기 위한 문서입니다.

---

## 1. 환경 변수 확인

운영 환경에서는 로컬 기본값에 의존하지 않습니다.

### Database

- [ ] `DB_URL`
- [ ] `DB_USERNAME`
- [ ] `DB_PASSWORD`
- [ ] RDS 또는 운영 PostgreSQL 접속 확인

### Redis

- [ ] `REDIS_HOST`
- [ ] `REDIS_PORT`
- [ ] Redis 접속 확인
- [ ] 인증용 Redis key prefix 충돌 여부 확인

### JWT

- [ ] `JWT_SECRET`
- [ ] `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS`
- [ ] `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS`
- [ ] 운영 JWT secret은 로컬 기본값을 사용하지 않음

### CORS

- [ ] `CORS_ALLOWED_ORIGINS`
- [ ] 운영 프론트 도메인 반영
- [ ] localhost origin이 운영에 남아있지 않은지 확인

---

## 2. JPA ddl-auto 설정

로컬 개발에서는 빠른 개발을 위해 `ddl-auto=update`를 사용할 수 있습니다.

운영에서는 Hibernate 자동 스키마 변경에 의존하지 않습니다.

권장 기준:

- local: `update`
- test: `create-drop` 또는 테스트 profile 설정
- production: `validate` 또는 `none`

체크:

- [ ] 운영 profile에서 `JPA_DDL_AUTO=update`를 사용하지 않음
- [ ] 운영 DB 스키마는 명시적 SQL 또는 마이그레이션 도구로 관리
- [ ] 배포 전 staging DB에서 schema validation 확인

---

## 3. DB 마이그레이션 확인

관련 문서:

- `docs/db-migration-plan.md`

현재 운영 배포 전 확인해야 할 주요 컬럼:

- [ ] `project_files.current_content`
- [ ] `project_files.edit_revision`

예상 SQL:

```sql
ALTER TABLE project_files
ADD COLUMN current_content text NOT NULL DEFAULT '';

ALTER TABLE project_files
ADD COLUMN edit_revision bigint NOT NULL DEFAULT 0;
```

체크:

- [ ] 기존 데이터가 있는 DB에서 SQL 적용 가능 여부 확인
- [ ] `NOT NULL` 컬럼 추가 시 기본값 처리 확인
- [ ] SQL 적용 후 애플리케이션 기동 확인
- [ ] 추후 default 제거 여부 결정

---

## 4. Redis 동작 확인

현재 Redis 사용 목적:

- Refresh Token 저장
- Access Token blacklist
- 권한 변경/탈퇴 시 force logout
- 추후 WebSocket presence/session

체크:

- [ ] 로그인 시 `auth:refresh:{userId}` 저장 확인
- [ ] 로그아웃 시 `auth:blacklist:{token}` 저장 확인
- [ ] 로그아웃 시 refresh token 삭제 확인
- [ ] 멤버 권한 변경/탈퇴 시 `auth:force-logout:{userId}` 저장 확인
- [ ] TTL이 의도한 시간으로 설정되는지 확인
- [ ] Redis에 토큰 원문 저장 여부와 해시 저장 전환 필요성 검토

---

## 5. 보안 확인

### JWT

- [ ] 운영 JWT secret 교체
- [x] Access Token 만료 시간 30분 확인
- [x] Refresh Token 만료 시간 14일 확인
- [x] 만료/무효/블랙리스트 토큰 응답 확인

### API 접근 제어

- [x] 인증 필요 API가 비로그인 요청을 차단하는지 확인
- [x] OWNER 전용 API에 EDITOR/VIEWER 접근 불가 확인
- [x] EDITOR 이상 API에 VIEWER 접근 불가 확인
- [x] 프로젝트 비멤버 접근 차단 확인

### Swagger

- [ ] 운영 환경에서 Swagger 공개 여부 결정
- [ ] 공개하지 않을 경우 security 설정 또는 배포 설정으로 차단

---

## 6. 파일 API 확인

관련 문서:

- `docs/file-api-contract.md`

체크:

- [x] 파일 상세 조회 응답에 `content` 포함
- [x] 파일 상세 조회 응답에 `editRevision` 포함
- [x] 파일 저장 요청에 `baseRevision` 포함
- [x] 저장 성공 시 `editRevision` 증가
- [x] 오래된 `baseRevision` 저장 시 `409 FILE_409` 응답
- [x] 1MB 초과 파일 저장 시 `FILE_400_3` 응답
- [x] 명시적 버전 저장 시 `currentVersion` 증가
- [x] 버전 복원 시 `currentContent`와 `editRevision` 갱신

---

## 7. 테스트 확인

배포 전 최소 확인:

```powershell
./gradlew spotlessApply
./gradlew check
```

체크:

- [x] 단위/통합 테스트 통과
- [x] ArchitectureRulesTest 통과
- [x] SwaggerIntegrationTest 통과
- [x] Global API foundation 테스트 통과
- [x] 파일 저장/버전/충돌 테스트 통과
- [x] 멤버/권한 테스트 통과

---

## 8. 수동 검증 시나리오

배포 후 또는 staging 환경에서 최소 수동 검증을 진행합니다.

### 인증

- [ ] 회원가입
- [ ] 로그인
- [ ] refresh token 재발급
- [ ] 로그아웃
- [ ] 로그아웃된 access token 차단

### 프로젝트

- [ ] 프로젝트 생성
- [ ] 프로젝트 목록 조회
- [ ] 프로젝트 상세 조회
- [ ] 프로젝트 수정
- [ ] 프로젝트 삭제

### 멤버/권한

- [ ] 멤버 초대
- [ ] 멤버 권한 변경
- [ ] VIEWER 파일 저장 실패
- [ ] OWNER 마지막 1명 나가기 방지

### 파일/버전

- [ ] 파일 생성
- [ ] 폴더 생성
- [ ] 파일 상세 조회
- [ ] 현재 내용 저장
- [ ] 명시적 버전 저장
- [ ] 버전 목록 조회
- [ ] 버전 복원
- [ ] 저장 충돌 409 확인
- [ ] 동시 편집 중 오래된 baseRevision 저장 요청 차단 확인
- [ ] 파일 검색

### 채팅/WebSocket

- [ ] WebSocket 연결
- [ ] STOMP JWT 인증
- [ ] 프로젝트 채팅 조회
- [ ] 프로젝트 채팅 전송
- [ ] 프로젝트 채널 권한 검증

---

## 9. 배포 후 모니터링

체크:

- [ ] 애플리케이션 기동 로그 확인
- [ ] DB connection pool 정상 확인
- [ ] Redis connection 정상 확인
- [ ] 인증 실패/권한 실패 로그 과다 발생 여부 확인
- [ ] 500 에러 발생 여부 확인
- [ ] 파일 저장/버전 저장 API 응답 시간 확인
- [ ] WebSocket 연결 실패 로그 확인

---

## 10. 추후 보완 항목

- [ ] Flyway 또는 Liquibase 도입
- [ ] Testcontainers 전환 검토
- [ ] 운영 profile 분리 강화
- [ ] Swagger 운영 차단 정책 확정
- [ ] 파일 크기 제한값 설정 분리
- [ ] Public 프로젝트 공개 범위 최종 확정
- [ ] WebSocket presence Redis TTL 정책 확정
- [ ] 파일 편집 presence / soft lock 도입 여부 결정
- [ ] CRDT/Yjs 기반 실시간 병합 도입 여부 결정
