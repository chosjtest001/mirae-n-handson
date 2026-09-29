# CLAUDE.md

## 빌드 · 테스트 명령

명령은 저장소 루트에서 각각 실행한다. API는 Java 21을 사용하며, Web의 선언된 요구 버전은 Node.js 22 이상이다.

```bash
# modern/api: 빌드 및 테스트
(cd modern/api && ./gradlew build)
(cd modern/api && ./gradlew test)

# modern/web: 빌드, 정적 검사 및 테스트
(cd modern/web && npm run build)
(cd modern/web && npm run lint)
(cd modern/web && npm run typecheck)
(cd modern/web && npm test)
```

- API 테스트는 H2를 사용하며 MariaDB 컨테이너에 의존하지 않는다.
- Web 명령은 프로젝트 의존성이 설치된 환경에서 실행한다. `npm test`는 Vitest를 단일 실행한다.

## 코딩 컨벤션

- 답변과 문서는 한국어로 작성한다. 클래스명, API 경로, 명령어는 원문을 유지한다.
- 소스 주석은 한국어로 작성하며 이모지를 사용하지 않는다.

### modern/api

- 코드는 `com.example.item`, `com.example.assignment`처럼 도메인별 패키지에 둔다.
- 의존성은 생성자로 주입한다. 단일 구현 서비스는 인터페이스 없이 `@Service` 구체 클래스로 작성한다.
- 도메인 컨트롤러는 요청 검증과 Service 호출을 담당하고, 업무 조건 판단과 데이터 변경은 Service에 둔다.
- 도메인 데이터의 성공 응답은 기존 record DTO 또는 DTO 목록으로 반환한다.
- 조회 서비스에는 `@Transactional(readOnly = true)`를 적용하고, 데이터 변경 메서드에는 `@Transactional`을 적용한다.
- 엔티티에서 DTO로의 변환은 서비스 트랜잭션 안에서 수행한다.
- 응답에 필요한 연관 데이터는 `@EntityGraph`, JPQL fetch join 또는 DTO 조회 쿼리로 가져온다.
- 예외의 HTTP 상태 코드와 오류 본문 변환은 `common/GlobalExceptionHandler`에서 수행한다.
- 업무 판단이나 저장에 사용하는 현재 시각은 주입받은 `Clock`으로 계산한다.
- 컨트롤러 테스트는 `@WebMvcTest`, 서비스 테스트는 Mockito, Repository 쿼리 테스트는 `@DataJpaTest`와 H2를 사용한다.
- Spring 컨텍스트를 사용하는 테스트는 `test` 프로필을 적용하고, `@DisplayName`은 한국어로 작성한다.

### modern/web

- React 컴포넌트는 함수로 작성하고, 컴포넌트 파일명은 PascalCase, 훅 이름은 `use` 접두사를 사용한다.
- API 호출 함수는 `src/api/`에 두고, HTTP 요청은 `src/api/client.ts`의 `getJson`을 거친다.
- 컴포넌트의 데이터 조회는 `src/hooks/`의 훅을 사용하고, 공통 요청 상태는 `useApiQuery`로 관리한다.
- API 응답 타입은 `src/api/types.ts`에 선언하고 백엔드 JSON의 필드명과 자료형에 맞춘다.
- 조회 화면에는 로딩, 오류, 빈 결과 상태를 표시하고 공통 표시는 `AsyncSection`을 사용한다.
- 테스트는 Vitest와 Testing Library를 사용하고, HTTP 응답은 `src/test/mockFetch.ts`로 대체한다.

## 금지 사항

### 팀 규칙

- **기존 API 계약 보존:** 별도 변경 요청이 없는 한 기존 API의 URL, HTTP 상태 코드, 요청 파라미터, 응답 필드명과 자료형을 변경하지 않는다.
- **변경 범위 제한:** 요청 범위 밖의 코드에 포맷팅, 이름 변경, 패키지 이동을 수행하지 않는다. 작업 시작 전에 존재하던 사용자 변경을 되돌리거나 덮어쓰지 않는다.
- **테스트 우회 금지:** 기대 동작의 변경이 승인되지 않은 상태에서 테스트를 통과시키기 위해 assertion을 삭제하거나, 테스트를 비활성화하거나, 스냅샷의 기대값을 변경하지 않는다.

### 공통 및 코드별 금지 사항

- `legacy/`는 분석·이관 대상이며, 사용자 허락 없이 수정하지 않는다.
- `vendor-prs/*.patch`와 `incident-logs/`의 원본 로그를 수정하지 않는다.
- 비밀번호, 인증 토큰, API 키를 소스에 하드코딩하지 않는다.
- 로그에 학생 식별자, 이메일, 인증 토큰의 원문을 기록하지 않는다.
- 컨트롤러에서 Repository를 주입하거나 SQL을 실행하지 않는다.
- JPA 엔티티를 API 응답으로 직접 반환하지 않는다.
- 컨트롤러에서 `try-catch`로 오류 응답을 만들거나 빈 `catch` 블록을 추가하지 않는다.
- 백엔드 로그는 SLF4J로 기록한다. `System.out.println`, `System.err.println`, `printStackTrace()`를 추가하지 않는다.
- 요청 값을 문자열로 이어 붙여 SQL이나 JPQL을 만들지 않는다. 쿼리 파라미터 바인딩을 사용한다.
- 프론트엔드 컴포넌트에서 `fetch`를 직접 호출하거나 API 서버 주소를 하드코딩하지 않는다.
- `any`, `as unknown as`, `@ts-ignore`, `dangerouslySetInnerHTML`을 추가하지 않는다.
- 클래스 컴포넌트, `React.FC`, `defaultProps`를 추가하거나 디버깅용 `console.log`를 변경 결과에 남기지 않는다.

## 완료 기준

- 이관 · 리팩토링 작업은 `characterization/`의 `npm test`가 전부 통과하기 전에는 완료로 보고하지 않는다.
- 테스트가 실패하면 실패한 케이스와 차이를 그대로 보고한다. "거의 됐다"처럼 요약해서 보고하지 않는다.
- 테스트를 통과시키려고 `characterization/`의 테스트 코드나 스냅샷 파일을 고치지 않는다. 스냅샷을 바꿔야 한다고 판단되면 작업을 멈추고 사용자에게 묻는다.
- 레거시 동작이 버그로 보여도 이관 중에는 고치지 않고, "의심 동작" 목록으로 따로 보고한다.

## 아키텍처 안내

- 이 저장소는 문항 은행, 과제 배포, 성적 집계의 레거시 코드를 `modern/`으로 이관하는 실습 프로젝트다.
- `modern/api`: Spring Boot 3.3, Java 21, Spring Data JPA 기반 REST API다. 업무 API의 호출 방향은 Controller → Service → Repository다.
- `modern/api`의 `item`은 문항 은행, `assignment`는 과제 배포와 집계를 담당한다. 공통 예외·시각 처리는 `common`, 웹 설정은 `config`에 둔다.
- `modern/api/src/main/resources/application.yml`은 MariaDB 연결과 커넥션 풀을 정의하며, OSIV는 비활성화되어 있다.
- 문항 상태는 `ItemStatus`의 `A`(공개), `R`(검수 중), `D`(삭제)다. 공개 목록은 `A`만 조회하고, 관리용 단건 조회는 상태와 무관하게 조회한다.
- 오류 본문은 `ErrorResponse`다. `GlobalExceptionHandler`는 없는 자원은 404, 입력 오류는 400, 상태 충돌은 409, 그 외 예외는 500으로 변환한다.
- `modern/web`: React 18, TypeScript, Vite 기반 화면이다. 컴포넌트 → `src/hooks/` → `src/api/` → `getJson` 순서로 데이터를 조회한다.
- API 기본 주소는 `VITE_API_BASE`로 설정한다. 빈 문자열이면 상대 경로를 사용하고, 개발 서버의 `/api` 요청은 Vite 프록시가 전달한다.
- `legacy/item-bank-php`는 `modern/api`의 `item`, `legacy/assignment-thymeleaf`는 `assignment`의 이관 근거다. `legacy/grade-mssql`의 대응 구현은 아직 없다.
- `characterization/`은 레거시와 새 API 응답을 정규화해 스냅샷과 비교한다. 이관 경로 대응은 `lib/target.mjs`, 응답 정규화는 `lib/normalize.mjs`에 있다.
- `templates/CLAUDE.spring.md`, `templates/CLAUDE.react.md`는 규칙의 참고 자료다. 위 규칙의 경로와 도구명은 현재 `modern/` 코드에 맞춘다.
