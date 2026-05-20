# DB Migration Plan

FlowDeck MVP 이후 운영 배포를 고려해 DB 스키마 변경 사항과 마이그레이션 전략을 정리합니다.

현재 로컬 개발 환경은 `spring.jpa.hibernate.ddl-auto=update` 기준으로 동작할 수 있습니다.
하지만 운영 환경에서는 Hibernate 자동 변경에 의존하지 않고, 명시적인 SQL 또는 마이그레이션 도구를 사용하는 방향이 안전합니다.

---

## 1. 현재 기준

### 로컬 개발

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}
```

로컬 개발에서는 `ddl-auto=update`로 빠르게 개발할 수 있습니다.
단, 이 설정은 운영 DB 스키마 관리 방식으로 사용하지 않습니다.

### 운영 배포

운영 배포 전에는 다음을 원칙으로 합니다.

- DB 스키마 변경은 명시적으로 기록합니다.
- 기존 데이터가 있는 테이블에는 기본값과 null 처리 전략을 먼저 정합니다.
- 컬럼 추가, 제약 조건 추가, 인덱스 추가는 배포 순서를 고려합니다.
- 가능한 경우 Flyway 또는 Liquibase 같은 마이그레이션 도구 도입을 검토합니다.

---

## 2. 현재 추가된 주요 컬럼

### project_files.current_content

파일의 현재 작업 내용을 저장하는 컬럼입니다.

```java
@Column(name = "current_content", nullable = false, columnDefinition = "text")
private String currentContent = "";
```

운영 DB에 기존 `project_files` 행이 있다면 `NOT NULL` 컬럼을 바로 추가할 때 실패할 수 있습니다.
기존 행에 기본값을 채워 넣는 전략이 필요합니다.

권장 SQL 초안:

```sql
ALTER TABLE project_files
ADD COLUMN current_content text NOT NULL DEFAULT '';
```

운영 정책에 따라 기본값을 유지할지 제거할지 추후 결정할 수 있습니다.

```sql
ALTER TABLE project_files
ALTER COLUMN current_content DROP DEFAULT;
```

MVP에서는 새 파일 생성 시 애플리케이션에서 빈 문자열을 기본값으로 사용합니다.

---

### project_files.edit_revision

파일 저장 충돌 감지를 위한 수정 번호입니다.

```java
@Column(name = "edit_revision", nullable = false)
private long editRevision = 0L;
```

파일 상세 조회 응답으로 내려가며, 저장 요청의 `baseRevision`과 비교합니다.

권장 SQL 초안:

```sql
ALTER TABLE project_files
ADD COLUMN edit_revision bigint NOT NULL DEFAULT 0;
```

운영 정책에 따라 기본값을 유지할지 제거할지 추후 결정할 수 있습니다.

```sql
ALTER TABLE project_files
ALTER COLUMN edit_revision DROP DEFAULT;
```

---

## 3. 파일 저장 제한 정책

현재 파일 저장 API는 단일 파일 content를 UTF-8 byte 기준 최대 1MB로 제한합니다.

현재 구현은 서버 상수 기반입니다.

```java
private static final int MAX_FILE_CONTENT_BYTES = 1024 * 1024;
```

운영 환경에서는 다음 중 하나를 검토할 수 있습니다.

- application 설정값으로 분리
- 프로젝트별 제한값 도입
- 사용자 플랜별 제한값 도입
- DB 저장 대신 S3 저장으로 전환

MVP에서는 서버 상수 1MB 기준을 유지합니다.

---

## 4. Flyway 또는 Liquibase 도입 시점

즉시 도입하지 않아도 되는 이유:

- 현재 MVP 단계에서는 스키마 변경 규모가 작습니다.
- 로컬/CI 개발 속도가 중요합니다.
- H2 PostgreSQL Mode 기반 테스트 전략을 유지하고 있습니다.

도입을 검토해야 하는 시점:

- 운영 DB에 실제 사용자 데이터가 쌓이기 시작할 때
- partial unique index 같은 PostgreSQL 전용 DDL이 필요할 때
- 스키마 변경 이력을 팀 단위로 추적해야 할 때
- 배포 롤백 전략이 필요할 때
- 운영 환경과 테스트 환경의 차이로 문제가 발생할 때

---

## 5. 향후 예상 마이그레이션 후보

### 파일/폴더 이름 중복 방지

현재는 서비스 레이어에서 같은 위치의 파일명 중복을 검증합니다.

PostgreSQL 기준으로 루트 폴더의 `parent_id IS NULL` 중복까지 DB에서 완전히 막으려면 partial unique index가 필요할 수 있습니다.

예상 SQL:

```sql
CREATE UNIQUE INDEX unique_project_root_file_name
ON project_files (project_id, name)
WHERE parent_id IS NULL;

CREATE UNIQUE INDEX unique_project_child_file_name
ON project_files (project_id, parent_id, name)
WHERE parent_id IS NOT NULL;
```

이 기능을 도입하면 Testcontainers 전환도 함께 검토하는 것이 좋습니다.

---

### public_id 확장

현재 파일과 버전 API는 내부 Long id를 사용합니다.

추후 외부 노출 ID를 UUID 기반으로 통일하려면 다음 컬럼 추가를 검토할 수 있습니다.

- `project_files.public_id`
- `file_versions.public_id`

이 변경은 API URL과 프론트 연동에도 영향을 주므로 별도 브랜치에서 검토합니다.

---

## 6. 운영 배포 전 체크리스트

- [ ] 운영 환경에서 `ddl-auto`를 `validate` 또는 `none`으로 전환할지 결정
- [ ] `project_files.current_content` 마이그레이션 SQL 확정
- [ ] `project_files.edit_revision` 마이그레이션 SQL 확정
- [ ] 기존 데이터 기본값 처리 검증
- [ ] 파일 크기 제한값 설정 분리 여부 결정
- [ ] PostgreSQL 전용 DDL 사용 여부 검토
- [ ] Flyway 또는 Liquibase 도입 시점 결정
- [ ] 배포 전 staging DB에서 SQL 검증
