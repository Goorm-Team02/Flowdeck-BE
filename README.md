# Flowdeck-BE

## Backend Quality Gate

- Backend conventions: [`backend/docs/conventions.md`](backend/docs/conventions.md)
- Collaboration guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Gradle runtime JDK: 21
- Format Java code: `cd backend && ./gradlew spotlessApply`
- Run the same verification as CI: `cd backend && ./gradlew check`

## 브랜치 전략 요약

- 기준 브랜치: `develop`
- 작업 브랜치: `feature/*`, `fix/*`, `refactor/*`, `docs/*`
- 배포/최종 제출: `develop`을 검증한 뒤 `main`으로 반영
- 상세 규칙과 작업 흐름: [`CONTRIBUTING.md`](CONTRIBUTING.md)

## 개발용 데이터베이스

- 백엔드 로컬 개발 DB : `PostgreSQL`
- 기본 로컬 연결 정보:
  - URL: `jdbc:postgresql://localhost:5432/flowdeck`
  - username: `flowdeck`
  - password: `flowdeck`
- 필요하면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`, `JPA_SHOW_SQL`로 값을 덮어쓰기 가능
- 로컬 DB는 `docker compose -f docker-compose.dev.yaml up -d`로 실행
- 예시 환경 변수 : `.env.example`

## Docker 배포 이미지

- 백엔드 이미지는 `backend/Dockerfile` 기준으로 빌드
- 기본 실행 프로필은 `dev`, 운영 배포 시에는 `SPRING_PROFILES_ACTIVE=prod`로 덮어쓰기

```bash
docker build -f backend/Dockerfile -t flowdeck-backend:dev backend
```

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/flowdeck \
  -e DB_USERNAME=flowdeck \
  -e DB_PASSWORD=flowdeck \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=change-me \
  flowdeck-backend:dev
```
