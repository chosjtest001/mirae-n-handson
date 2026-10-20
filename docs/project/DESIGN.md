# 실전 프로젝트 설계 — 백엔드 / chosj

- 작성일: 2026-10-20
- 상태: 초안. "4. 미해결 질문" 1~2번이 답해지기 전에는 구현을 시작하지 않는다.

## 1. 목표

조회 API 1개를 Controller → Service → MyBatis Mapper 순으로 호출되게 만들고, 정상 · 없는 자원 · 잘못된 입력 응답을 테스트로 구분해 확인한다.

## 2. 재료

| 재료 | 있음/없음 | 경로 |
|---|---|---|
| 컨벤션 점검 Skill | 있음 | `.claude/skills/convention-check/SKILL.md` |
| 머지 전 검증 Skill | 있음 | `.claude/skills/verify/SKILL.md`, `.claude/skills/verify/docs/approval-checklist.md` |
| 영향도 분석 Skill | 있음 | `.claude/skills/impact/SKILL.md` |
| 기타 Skill(이번 트랙과 무관) | 있음 | `.claude/skills/batch-check/`, `.claude/skills/rca/`, `.claude/skills/document-module/` |
| 리뷰어 · 테스터 Sub-agent | 있음 | `.claude/agents/reviewer.md`, `.claude/agents/tester.md` |
| IaC 리뷰어 Sub-agent(이번 트랙과 무관) | 있음 | `.claude/agents/iac-reviewer.md` |
| 위험 명령 차단 · 시크릿 검사 Hook | 있음 | `hooks/block-dangerous.mjs`, `hooks/secret-scan.mjs`, 연결 설정 `.claude/settings.json` |
| characterization 테스트 | 있음 (단, 레거시와 기존 item API 비교용이며 새 엔드포인트는 다루지 않음) | `characterization/tests/`, `characterization/lib/target.mjs` |
| Spring Boot API 골격 (JPA 기반) | 있음 | `modern/api/build.gradle`, `modern/api/src/main/java/com/example/` |
| Controller → Service 흐름 참고 예시 (Repository는 JPA) | 있음 | `modern/api/src/main/java/com/example/item/UnitController.java`, `UnitService.java`, `UnitRepository.java` |
| 공통 예외 → HTTP 상태 변환 | 있음 | `modern/api/src/main/java/com/example/common/GlobalExceptionHandler.java`, `NotFoundException.java`, `ErrorResponse.java` |
| 테스트용 H2 설정 | 있음 (스키마는 JPA `ddl-auto: create-drop`이 생성) | `modern/api/src/test/resources/application-test.yml` |
| DB 스키마 (MariaDB) | 있음 | `db/mariadb/init/01-schema.sql` (unit, item, tag, item_tag, class, assignment, distribution, submission) |
| 스펙 기반 실습용 Java 골격 | 있음 (H2, `GET /health`만 있음) | `specs/starters/java/` |
| 승인 체크리스트 · Spring 규칙 템플릿 | 있음 | `docs/approval-checklist.md`, `templates/CLAUDE.spring.md` |
| pipeline-samples | 있음 (배치 로그 · Terraform 샘플, 이번 트랙과 무관) | `pipeline-samples/` |
| MyBatis 의존성 | 없음 → 새로 만들 것 | `modern/api/build.gradle`에 추가 예정 |
| MyBatis 설정 (mapper 위치, camelCase 매핑) | 없음 → 새로 만들 것 | `modern/api/src/main/resources/application.yml`, `application-test.yml` |
| Mapper 인터페이스 · Mapper XML | 없음 → 새로 만들 것 | `modern/api/src/main/java/com/example/<도메인>/*Mapper.java`, `modern/api/src/main/resources/mapper/*.xml` |
| Mapper 테스트 및 테스트용 스키마 | 없음 → 새로 만들 것 | `modern/api/src/test/java/com/example/<도메인>/*MapperTest.java`, 필요 시 `src/test/resources/` 아래 스키마 SQL |
| MyBatis 사용 예시 코드 | 없음 | 저장소 전체 검색 결과 없음. `legacy/assignment-thymeleaf`의 DAO는 MyBatis가 아니라 `JdbcTemplate` + `RowMapper`다 |

## 3. 범위

### 한다

1. MyBatis 의존성과 설정을 추가하고, 기존 테이블 1개를 조회하는 Mapper(SQL은 XML, 파라미터는 `#{}` 바인딩)를 만든다.
2. 그 Mapper를 호출하는 Service(`@Transactional(readOnly = true)`)와 GET 엔드포인트 1개를 가진 Controller를 만든다. 응답은 record DTO다.
3. 정상(200), 없는 자원(404), 잘못된 입력(400)을 Controller 테스트로, SQL 결과를 Mapper 테스트로 확인한다.

대상 테이블 · 엔드포인트는 미정이다(미해결 질문 1). 후보: `unit` 단건 조회, `class` 단건 조회.

### 하지 않는다

- 등록 · 수정 · 삭제 API: 40분 안에 조회 흐름 하나를 끝까지 확인하는 것이 우선이다.
- 기존 JPA Repository를 MyBatis로 교체: 기존 API 계약과 테스트에 영향을 주며 요청 범위 밖이다.
- 화면(`modern/web`) 연동: 트랙이 백엔드다.
- `legacy/grade-mssql` 이관 · characterization 스냅샷 추가: 새 엔드포인트는 레거시 대응이 없고, 스냅샷 변경은 사용자 승인 사항이다.
- 인증 · 권한 처리: 별도 과제이며 시간 안에 끝낼 수 없다.
- 페이징 · 동적 검색 조건: Mapper 동적 SQL까지 넣으면 범위가 커진다.

## 4. 미해결 질문

1. **범위 원문이 비어 있다.** 요청의 "범위" 칸이 `(여기에 위 표의 "최소 범위로 줄인다면" 칸)` 상태로 왔고, 저장소에서 그 표를 찾지 못했다. 어떤 테이블 · 엔드포인트를 만들지 정해 주어야 한다(후보: `unit`, `class` 단건 조회).
2. **어디에 만드는가.** 이 문서의 파일 경로는 `modern/api` 기준 가정이다. 선택지:
   - `modern/api`: Controller · Service · 예외 처리 골격을 재사용할 수 있다. 다만 JPA와 MyBatis가 한 모듈에 섞이고 공유 `build.gradle`을 고친다. CLAUDE.md 컨벤션은 Repository(JPA) 계층만 설명한다.
   - `specs/starters/java`: 빈 골격이라 충돌이 없다. README는 Day 4-2 spec 실습용이고 테이블을 `@Entity`로 만들라고 정하고 있다.
   - 새 모듈: 독립적이지만 골격을 만드는 데 시간이 든다.
3. **Mapper 테스트의 테이블 준비 방식.** 현재 테스트 H2 스키마는 JPA `ddl-auto`가 만든다. MyBatis 슬라이스 테스트(`@MybatisTest`)는 JPA를 띄우지 않으므로 테스트용 스키마 SQL을 따로 둘지, `@SpringBootTest`로 전체 컨텍스트를 띄울지 정해야 한다.
4. **"API 동작에 대한 정확한 구분"의 뜻.** 이 문서는 "정상 200 / 없는 자원 404 / 잘못된 입력 400을 테스트로 구분"으로 해석했다. 다른 뜻(예: 조회 · 변경 API의 구분, HTTP 메서드 구분)이면 범위와 완성 기준을 고쳐야 한다.
5. **MyBatis 스타터 버전.** Spring Boot 3.3과 호환되는 `mybatis-spring-boot-starter` 버전은 설계 승인 후 공식 문서로 확인한다. 이 문서에서는 정하지 않았다.

## 5. 단계

경로의 `<도메인>`, `<Name>`, `<경로>`는 미해결 질문 1이 정해지면 채운다. 미해결 질문 2의 답이 `modern/api`가 아니면 경로 앞부분이 바뀐다.

### 단계 1 — MyBatis 설정과 Mapper

- 할 일: MyBatis 의존성 추가, mapper XML 위치와 `map-underscore-to-camel-case` 설정, Mapper 인터페이스와 XML 작성, Mapper 테스트 작성.
- 만들거나 고칠 파일:
  - `modern/api/build.gradle` (의존성 추가)
  - `modern/api/src/main/resources/application.yml`, `modern/api/src/test/resources/application-test.yml` (MyBatis 설정)
  - `modern/api/src/main/java/com/example/<도메인>/<Name>Mapper.java`
  - `modern/api/src/main/resources/mapper/<Name>Mapper.xml`
  - `modern/api/src/test/java/com/example/<도메인>/<Name>MapperTest.java` (필요 시 테스트용 스키마 SQL)
- 확인 방법:
  - `(cd modern/api && ./gradlew test --tests '*MapperTest')` 결과에 Mapper 테스트가 PASSED로 출력된다.
  - `grep -n '\${' modern/api/src/main/resources/mapper/*.xml` 출력이 비어 있다.

### 단계 2 — Service와 Controller

- 할 일: Mapper를 주입받는 `@Service`(`@Transactional(readOnly = true)`), 결과가 없으면 `NotFoundException`을 던지는 로직, record 응답 DTO, GET 엔드포인트 1개를 가진 Controller, `@WebMvcTest` Controller 테스트와 Mockito Service 테스트 작성.
- 만들거나 고칠 파일:
  - `modern/api/src/main/java/com/example/<도메인>/<Name>Service.java`
  - `modern/api/src/main/java/com/example/<도메인>/<Name>Controller.java`
  - `modern/api/src/main/java/com/example/<도메인>/<Name>Response.java`
  - `modern/api/src/test/java/com/example/<도메인>/<Name>ControllerTest.java`
  - `modern/api/src/test/java/com/example/<도메인>/<Name>ServiceTest.java`
- 확인 방법:
  - `(cd modern/api && ./gradlew test --tests '*<Name>ControllerTest' --tests '*<Name>ServiceTest')` 결과에서 200 · 404 · 400 케이스가 각각 PASSED로 출력된다.
  - `grep -n 'Mapper' modern/api/src/main/java/com/example/<도메인>/<Name>Controller.java` 출력이 비어 있다.

### 단계 3 — 전체 검증

- 할 일: 전체 테스트 실행, 실제 서버에서 호출 확인, 컨벤션 점검.
- 만들거나 고칠 파일: 없음 (점검에서 위반이 나오면 단계 1~2의 파일을 고친다).
- 확인 방법:
  - `(cd modern/api && ./gradlew build)`가 `BUILD SUCCESSFUL`로 끝난다.
  - 사용자가 직접 실행: MariaDB 컨테이너를 띄우고 `(cd modern/api && ./gradlew bootRun)` 후 `curl -i http://localhost:8080/api/<경로>/<있는 ID>`가 `200`과 JSON을, `curl -i http://localhost:8080/api/<경로>/<없는 ID>`가 `404`와 `ErrorResponse` 본문을 돌려준다. (`.claude/settings.json`에서 `curl`은 deny, `bootRun`은 ask이므로 Claude가 아니라 사용자가 실행한다.)
  - `/convention-check` 결과에 위반 0건이 보고된다.
  - `git diff --stat`에 기존 컨트롤러 · DTO 파일(`item/`, `assignment/`)이 나오지 않는다.

## 6. 완성 기준 (Day 4-1에서 점검)

- [ ] `(cd modern/api && ./gradlew test)`의 종료 코드가 0이고, 새 Mapper · Service · Controller 테스트가 모두 PASSED다.
- [ ] 새 Controller는 Service만 주입받는다. Controller 파일에 `Mapper` 타입 import가 0건이고, Mapper 호출은 Service에만 있다.
- [ ] 새 SQL은 Mapper XML에만 있고, `#{}` 바인딩만 사용한다(`${` 0건). Java 코드에서 SQL 문자열을 만들지 않는다.
- [ ] Service 클래스 또는 조회 메서드에 `@Transactional(readOnly = true)`가 있고, 응답 타입이 record DTO다.
- [ ] Controller 테스트에서 있는 ID는 200과 DTO 필드, 없는 ID는 404와 `ErrorResponse`, 형식이 잘못된 ID는 400을 각각 검증하는 케이스가 있고 모두 통과한다.
- [ ] 기존 API의 URL · 응답 필드가 바뀌지 않았다: `git diff`에 `item/`, `assignment/` 패키지의 기존 파일 변경이 0건이다.
