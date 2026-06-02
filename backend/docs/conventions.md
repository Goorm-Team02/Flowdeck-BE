# 백엔드 컨벤션

## 1. 품질 검사 기준

- 코드 포맷: Spotless + google-java-format
- 정적 스타일 검사: Checkstyle
- 구조 규칙 검사: ArchUnit
- CI 실행 명령어: `./gradlew check`

## 2. 패키지 구조

운영 백엔드 코드는 `com.flowdeck.backend` 아래에서 시작합니다.

```text
com.flowdeck.backend
├── global
│   ├── config
│   ├── entity
│   ├── error
│   ├── response
│   ├── security
│   └── transaction
└── {feature}
    ├── controller
    ├── service
    ├── dto
    ├── domain
    ├── repository
    ├── realtime
    └── store
```

현재 기능 패키지는 다음 기준으로 나눕니다.

- `auth`: 회원가입, 로그인, refresh, logout 같은 인증 흐름
- `user`: 내 정보 조회/수정, 회원 탈퇴
- `project`: 프로젝트 생성, 조회, 수정, 삭제
- `member`: 프로젝트 멤버와 역할 관리
- `permission`: 프로젝트 접근/권한 판정 공통 로직
- `file`: 파일/폴더 트리, 검색, 이름변경, 이동, 삭제와 파일 WebSocket 이벤트
- `version`: 파일 내용 저장, 명시적 버전 저장, 복원, 타임라인, diff
- `projectmessage`: 프로젝트 채팅/LOG 메시지 REST 및 STOMP 처리
- `presence`: 프로젝트 접속 상태 REST, STOMP heartbeat, Redis presence 저장소

`realtime` 패키지는 STOMP destination, broadcaster 등 WebSocket 발행 책임을 둡니다.
`store` 패키지는 Redis처럼 repository와 성격이 다른 임시 상태 저장소 어댑터에 사용합니다.
필요하지 않은 하위 패키지는 만들지 않습니다.

테스트 코드는 목적에 따라 `com.flowdeck.backend.{category}` 아래에 배치합니다.

- `com.flowdeck.backend.architecture`
- `com.flowdeck.backend.integration`
- `com.flowdeck.backend.isolated`
- `com.flowdeck.backend.unit`

스프링 부트 기본 기동 스모크 테스트는 관례에 따라 `com.flowdeck.backend.BackendApplicationTests`에 둘 수 있습니다.

공통 테스트 애노테이션과 격리 테스트 부트스트랩 코드는 메인 애플리케이션 스캔과 분리하기 위해 `com.flowdeck.testsupport` 패키지를 사용할 수 있습니다.

## 3. 계층별 역할

- `controller`: 요청 매핑, 입력 검증, 인증 사용자 바인딩, 응답 조립
- `service`: 비즈니스 로직, 트랜잭션 경계 설정, repository 및 외부 클라이언트 호출 조합
- `repository`: 영속성 처리만 담당, HTTP 관련 로직 금지
- `domain`: 엔티티 상태와 도메인 동작 표현
- `realtime`: WebSocket destination 구성과 이벤트 발행
- `store`: Redis 등 임시 상태 저장소 접근
- `global`: 공통 설정, 보안, 예외 처리 같은 횡단 관심사 담당

## 4. 처리 규칙

- `controller`는 `repository`를 직접 호출하면 안 됩니다.
- 트랜잭션 처리는 `service` 계층에만 둡니다.
- 의존성 주입은 생성자 주입만 사용합니다. `@Autowired` 필드 주입은 금지합니다.
- 요청과 응답은 DTO로 주고받습니다. `controller`에서 엔티티를 직접 노출하지 않습니다.
- 비즈니스 예외 처리는 하나의 `@RestControllerAdvice`로 모읍니다.
- 입력값 검증은 `controller` 경계에서 `@Valid`로 처리합니다.

## 5. 로컬 작업 방법

Gradle 실행 JDK는 21을 사용합니다. JDK 17에서는 포맷터가 안정적으로 동작하지 않습니다.

```bash
cd backend
./gradlew spotlessApply
./gradlew check
```

- `spotlessApply`: Java 코드 포맷을 자동으로 맞춥니다.
- `check`: 포맷, 스타일, 구조 규칙, 테스트를 한 번에 검사합니다.

커밋 전에 포맷이 바뀌었을 수 있으면 `spotlessApply`를 먼저 실행합니다. `check`는 CI가 pull request와 보호 브랜치에서 실행하는 것과 같은 검증입니다.


## Redis Key Convention

Redis는 영구 데이터 저장소가 아니라 토큰, 접속 상태, 임시 상태, 캐시 용도로만 사용합니다.
프로젝트, 파일, 버전, 채팅 원본 데이터는 PostgreSQL에 저장합니다.

Key는 도메인 prefix를 포함합니다.

- `auth:refresh:{userId}`: Refresh Token 저장 (백엔드1)
- `auth:blacklist:{sha256(accessToken)}`: 로그아웃/회원 탈퇴된 Access Token 차단 (백엔드1)
- `auth:force-logout:{userId}`: 권한 변경/탈퇴 시 이전 Access Token을 차단하기 위한 cutoff epoch millis. TTL은 Access Token 만료 시간과 동일하게 설정합니다. (백엔드1)
- `presence:project:{projectId}`: 프로젝트 접속 세션 상태. score는 마지막 heartbeat 시각 epoch millis 입니다. (백엔드3)
- `ws:session:{sessionId}`: WebSocket 세션 보조. `projectId`, `userId`, `lastSeenAt`를 저장하며 TTL 30초를 사용합니다. 사용자 이름은 저장하지 않습니다. (백엔드3)
- `presence:user:{userId}`: 사용자별 WebSocket sessionId 보조 인덱스. 회원 탈퇴 등 사용자 단위 presence 정리에 사용하며 TTL 60초를 사용합니다. (백엔드3)
- `presence:file:{projectId}:{fileId}`: 파일별 editing presence. 현재 편집자의 `userId`, `userName`, `sessionId`, `lastSeenAt`를 저장하며 TTL 30초를 사용합니다. (백엔드3)
- `run:result:{runId}`: 코드 실행 결과 임시 저장 (Could 기능)

TTL이 필요한 key는 반드시 만료 시간을 설정합니다.
