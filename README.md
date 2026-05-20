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
