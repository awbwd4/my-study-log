# my-study-log

학원(강사-반-학생) 구조를 가진 오답노트 서비스. 학생은 문제 사진과 함께 오답을 기록하고
단어장을 관리하며, 강사는 담당 반 학생들의 오답노트를 열람할 수 있습니다.

## 기술 스택

- 백엔드: Kotlin + Spring Boot 3 (Gradle Kotlin DSL), Spring Data JPA, Spring Security
- 프론트엔드: React + TypeScript (Vite)
- 인증: 카카오 OAuth (프론트에서 Kakao JS SDK로 로그인 → 액세스 토큰을 백엔드가 검증 → 자체 JWT 발급)
- DB: 로컬 개발은 파일 기반 H2, 배포 시 PostgreSQL(`DATABASE_URL`)

## 로컬 실행

### 사전 준비 (JDK 17)

이 프로젝트는 JDK 17이 필요합니다. 시스템에 없다면 아래처럼 로컬 전용으로 받아 써도 됩니다.

```bash
mkdir -p ~/tools && cd ~/tools
curl -sSL "https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse?project=jdk" -o jdk17.tar.gz
tar -xzf jdk17.tar.gz && rm jdk17.tar.gz
```

### 백엔드

```bash
cd backend
JAVA_HOME=~/tools/jdk-17.0.19+10/Contents/Home ./gradlew bootRun
```

- 기본적으로 `dev` 프로필로 실행되며(`application.yml`), `http://localhost:8081`에서 뜹니다 (8080은 다른 프로세스가 쓰는 경우가 많아 dev 기본값을 8081로 잡았습니다).
- H2 DB 파일은 `backend/data/`, 업로드 이미지는 `backend/uploads/`에 저장됩니다 (둘 다 gitignore).

#### 카카오 로그인 없이 테스트하기 (dev-login)

카카오 디벨로퍼스 앱을 아직 만들지 않았다면, `dev` 프로필에서만 열리는 우회 로그인을 쓸 수 있습니다.

```bash
curl -X POST http://localhost:8081/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"kakaoId":"teacher-1","type":"TEACHER","name":"김선생","academyName":"my학원"}'
```

`/api/auth/dev-login`은 `@Profile("dev")`에서만 등록되며 운영 환경에는 노출되지 않습니다.

#### 실제 카카오 로그인 켜기

1. https://developers.kakao.com 에서 애플리케이션 생성
2. "플랫폼" 설정에 프론트 도메인(`http://localhost:5173` 등) 등록
3. 앱 키의 **JavaScript 키**를 `frontend/.env`의 `VITE_KAKAO_JS_KEY`에 설정
4. (선택) REST API 키가 필요해지면 백엔드 `KAKAO_REST_API_KEY` 환경변수에 설정 — 현재 백엔드는 카카오 토큰 검증에 REST API 키를 사용하지 않고, 프론트가 전달한 액세스 토큰으로 `kapi.kakao.com/v2/user/me`를 직접 호출해 검증합니다.

### 프론트엔드

```bash
cd frontend
cp .env.example .env   # 필요시 VITE_KAKAO_JS_KEY 채우기
npm install
npm run dev
```

`http://localhost:5173`에서 뜨며, 백엔드(`http://localhost:8081`)를 호출합니다. `VITE_KAKAO_JS_KEY`가 비어 있으면 로그인 화면의 "카카오로 시작하기" 버튼은 비활성화되고, 대신 "개발자 로그인" 폼(dev 빌드에서만 노출)으로 로그인할 수 있습니다.

## 배포

### 백엔드 (Railway)

Railway 프로젝트 `my-study-log`에 `backend` 서비스(Kotlin/Spring Boot)와 `Postgres` 서비스가 떠 있습니다.

- **배포는 수동으로만 합니다.** GitHub 저장소 연동(자동배포)은 의도적으로 끊어뒀습니다 — 이 레포가
  `backend/`, `frontend/`가 함께 있는 모노레포라, Railway의 서비스별 "Root Directory" 설정과
  `railway up --path-as-root`로 스냅샷을 직접 올리는 방식이 서로 충돌해서(둘 다 같은 이름의
  하위 디렉터리를 한 번 더 찾으려 해서) root directory를 지정해두면 수동 배포가 깨지고,
  지정 안 해두면 GitHub push 자동배포가 깨지는 문제가 있었습니다. 수동 배포 쪽이 검증된
  방식이라 이걸로 통일했습니다.
- 코드를 고친 뒤 배포하려면:
  ```bash
  cd ~/my-study-log
  railway up backend --path-as-root --service backend --ci
  ```
- 환경변수(`SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET`, `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`,
  `KAKAO_REST_API_KEY`, `CORS_ALLOWED_ORIGINS`)는 이미 Railway `backend` 서비스에 등록돼 있습니다.
  `DATABASE_URL`은 PostgreSQL JDBC 드라이버가 `user:pass@host` 형태를 지원하지 않기 때문에
  `jdbc:postgresql://호스트:포트/DB`만 담고, 인증정보는 `DATABASE_USERNAME`/`DATABASE_PASSWORD`로 분리했습니다.
  변경하려면: `railway variable set KEY=VALUE --service backend`
- 시작 명령어는 `backend/railway.json`에 명시돼 있습니다 (Railway의 Railpack 빌더가 자동 생성하는
  기본 명령어는 하위 모듈이 있는 멀티 프로젝트 구조를 가정해서 우리 같은 단일 Gradle 프로젝트에서는
  jar를 못 찾는 버그가 있어 직접 지정함).
- 공개 URL: 배포 후 `railway domain --service backend`로 확인/발급

### 프론트엔드 (Vercel)

- `npm run build` 후 정적 호스팅, 빌드 환경변수로 `VITE_API_BASE_URL`(백엔드 Railway URL)과
  `VITE_KAKAO_JS_KEY`를 설정
- 프론트 도메인이 정해지면 백엔드 `CORS_ALLOWED_ORIGINS`도 그 도메인으로 업데이트해야 합니다:
  `railway variable set CORS_ALLOWED_ORIGINS=https://your-app.vercel.app --service backend`
