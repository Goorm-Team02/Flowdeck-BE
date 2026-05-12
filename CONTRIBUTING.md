# Flowdeck-BE 협업 가이드

## 1. 브랜치 전략

- `main`: 배포 가능하거나 최종 제출 가능한 안정 코드 유지
- `develop`: 백엔드 개발 내용을 통합하는 기본 브랜치
- `feature/*`: 기능 개발용 브랜치
- `fix/*`: 버그 수정용 브랜치
- `refactor/*`: 동작 변경 없이 구조를 정리하는 브랜치
- `docs/*`: 문서 수정용 브랜치

중요한 점은 브랜치 이름이 `develop/feature/*`가 아니라는 것.
정확한 흐름은 `develop`에서 `feature/*`, `fix/*`, `refactor/*`, `docs/*` 브랜치를 따서 작업한 뒤, pull request로 다시 `develop`에 합치는 방식

## 2. 브랜치 이름 규칙

- 형식: `<type>/<name>`
- 허용 type: `feature`, `fix`, `refactor`, `docs`
- 이름 규칙: 영어 소문자, 숫자, 하이픈(`-`)만 사용
- 정규식: `^(feature|fix|refactor|docs)\/[a-z0-9]+(-[a-z0-9]+)*$`

예시:

- `feature/auth`
- `feature/project-member`
- `fix/login-token-error`
- `refactor/auth-service`
- `docs/api-spec`

지양할 예시:

- `feature/로그인`
- `Feature/Login`
- `feature_login`
- `feature/login기능`

## 3. 기본 작업 흐름

1. `develop` 최신 상태를 가져옵니다.
2. 작업 목적에 맞는 브랜치를 `develop`에서 생성합니다.
3. 구현 후 포맷과 검증을 실행합니다.
4. 커밋 메시지 규칙에 맞춰 커밋합니다.
5. pull request를 `develop`으로 생성합니다.
6. 리뷰 후 `develop`에 merge 합니다.
7. 배포나 제출 시점에는 `develop`에서 `main`으로 pull request를 생성합니다.

예시 명령:

```bash
git switch develop
git pull origin develop
git switch -c feature/auth

cd backend
./gradlew spotlessApply
./gradlew check
```

커밋 메시지 형식 : `[TYPE] subject`.
예시 : `[FEAT] add login API`

## 4. 저장소에서 강제하는 규칙

- `.githooks/pre-push`: `main`, `develop` 직접 push를 막고 브랜치 이름 형식 검사
- `.github/workflows/branch-policy.yml`: pull request의 source/base 브랜치 조합 검사
- `.github/workflows/commit-message.yml`: pull request 범위의 커밋 메시지 형식 검사

## 5. GitHub 설정 

- `main`, `develop` direct push 제한
- pull request merge만 허용
- 최소 1명 이상 코드 리뷰 승인 필수
- CI 성공 후에만 merge 허용

## 6. 로컬 설정

이 저장소의 Git hook을 사용하려면 한 번만 아래 명령 실행

```bash
git config core.hooksPath .githooks
```
