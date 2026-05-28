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

- 백엔드 로컬 개발 인프라 : `PostgreSQL`, `Redis`
- 기본 로컬 연결 정보:
  - URL: `jdbc:postgresql://localhost:5432/flowdeck`
  - username: `flowdeck`
  - password: `flowdeck`
- 필요하면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`, `JPA_SHOW_SQL`로 값을 덮어쓰기 가능
- 개발 Docker 환경은 `docker compose --env-file .env -f docker-compose.dev.yaml up -d --build`로 실행
- 개발 예시 환경 변수 : `.env.dev.example`

## AWS Docker 배포

- 개발 Docker 이미지는 [`backend/Dockerfile.dev`](backend/Dockerfile.dev) 로 빌드합니다.
- 운영 Docker 이미지는 [`backend/Dockerfile.prod`](backend/Dockerfile.prod) 로 빌드합니다.
- 운영 compose는 [`docker-compose.prod.yaml`](docker-compose.prod.yaml) 입니다.
- 운영 서버에는 `.env.prod.example` 을 복사한 `.env.prod` 를 만들고 실제 비밀값을 채웁니다.
- `.env`, `.env.prod` 는 Git에 커밋하지 않습니다.

개발 서버 예시:

```bash
cd /opt/flowdeck/backend-dev/app
git checkout develop
git pull origin develop
cp .env.dev.example .env
docker compose --env-file .env -f docker-compose.dev.yaml up -d --build
```

운영 서버 예시:

```bash
cd /opt/flowdeck/backend-prod/app
git checkout main
git pull origin main
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

운영 권장값:

- `SPRING_PROFILES_ACTIVE=prod`
- `JPA_DDL_AUTO=validate`
- `JPA_SHOW_SQL=false`
- `JWT_SECRET` 은 32바이트 이상의 랜덤 문자열 사용
- `CORS_ALLOWED_ORIGINS` 는 실제 프론트엔드 도메인만 허용

## 정적 Swagger 문서 배포

- 프론트 공유용 문서는 GitHub Pages 기준으로 배포합니다.
- 페이지 엔트리는 [`docs/index.html`](docs/index.html) 이고, `develop` 기준 OpenAPI 문서는 워크플로에서 `docs/openapi.json` 으로 생성합니다.
- `Try it out` 은 정적 문서 배포에서 혼선을 줄이기 위해 비활성화했습니다.

배포 방식:

1. `develop` 브랜치에 머지
2. [`Swagger Docs Pages`](.github/workflows/swagger-docs-pages.yml) 워크플로 실행
3. 테스트 컨텍스트에서 `/v3/api-docs` 를 생성
4. `docs/` 아티팩트를 GitHub Pages에 배포

확인 주소:

- `https://<org-or-user>.github.io/<repo>/`
- 예: `https://goorm-team02.github.io/Flowdeck-BE/`

주의:

- 이 방식은 문서 확인용입니다.
- 실제 API 호출, DB/Redis 연동, WebSocket 검증은 포함하지 않습니다.
