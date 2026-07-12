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

- 백엔드: `SPRING_PROFILES_ACTIVE=prod`로 기동, `backend/.env.example`에 정리된 환경변수(`JWT_SECRET`, `DATABASE_URL`, `KAKAO_REST_API_KEY`, `CORS_ALLOWED_ORIGINS`)를 배포 플랫폼에 등록
- 프론트엔드: `npm run build` 후 정적 호스팅, `VITE_API_BASE_URL`/`VITE_KAKAO_JS_KEY`를 빌드 환경변수로 설정
