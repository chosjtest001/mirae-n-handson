# 실 전체 적용 규칙 후보 (chosj / 주 스택: spring)

대상: `.claude/settings.json`의 permissions · hooks, `hooks/block-dangerous.mjs`, `hooks/secret-scan.mjs`, `scripts/hook-node.sh`
기준일: 2026-10-13. 이번 보완은 문서에만 반영했으며, 아래 보완 후보를 실제 설정 · Hook · 다른 규칙 문서에 적용하지 않았다.

스택 코드 표기: spring(gradlew), node(npm), docker(docker compose), github(MCP). python(pip), php(composer), terraform은 해당 도구를 사용하는 프로젝트의 조건부 정책이다. DB 종류와 Spring 사용 여부는 별도로 판단한다.

## 현재 공유 설정 · Hook에서 확인한 규칙

아래 표는 `.claude/settings.json`과 두 Hook의 현재 선언을 정리한 것이다. `.claude/settings.local.json` · 사용자/관리 정책과 실행 버전까지 합친 최종 유효 권한이나 실제 차단 성공을 검증한 결과는 아니다. 쉼표로 묶은 항목은 각각 별도의 규칙이다.

| 규칙 | 종류(deny / ask / allow / Hook) | 적용 범위 | 현재 의도와 한계 |
|---|---|---|---|
| `Bash(rm -rf *)` | deny | 공통 | 작업 디렉터리 · 홈 전체 삭제 방지. 모든 삭제 명령을 식별하는 규칙은 아니다 |
| `Bash(sudo *)` | deny | 공통 | 권한 상승으로 시스템 설정 · 패키지를 변경하는 사고 방지 |
| `Bash(git push --force *)`, `Bash(git push -f *)` | deny | 공통 | 해당 형태의 원격 이력 덮어쓰기 방지. 옵션 위치 · 실행 형태가 다른 경우는 별도 확인 필요 |
| `Bash(git reset --hard *)` | deny | 공통 | 커밋하지 않은 작업 내용 유실 방지 |
| `Bash(*prod-db*)`, `Bash(*prod.db*)` | deny | 공통 | 명령 문자열에 지정 표기가 있으면 차단. 실제 운영 주소 식별은 아니며 문서 검색도 막을 수 있다 |
| `Bash(curl *)`, `Bash(wget *)` | deny | 공통 | 해당 명령의 전송 · 다운로드를 차단. 로컬 헬스체크도 막으며 다른 도구의 통신까지 통제하지 않는다 |
| `Read(./.env)`, `Read(./.env.*)`, `Read(./secrets/**)` | deny | 공통 | 해당 경로의 비밀 파일 열람 제한. `.env.example`도 포함되며 중첩 경로 · 실행 위치 확인 필요 |
| `Read(**/*.pem)`, `Read(~/.ssh/**)`, `Read(~/.aws/**)` | deny | 공통 | 개인키 · 클라우드 자격 증명 노출 방지 |
| `Edit(./.env)`, `Edit(./.env.*)`, `Edit(./secrets/**)` | deny | 공통 | 해당 파일의 임의 변경 · 비밀값 기록 제한 |
| `mcp__github__delete_file`, `mcp__github__merge_pull_request` | deny | github MCP | 원격 파일 삭제 · 사람의 승인 없는 머지 방지 |
| `Bash(git push *)` | ask | 공통 | 원격 반영 대상 브랜치 · 범위 확인 |
| `mcp__github__create_pull_request`, `mcp__github__push_files` | ask | github MCP | 외부 저장소에 코드가 반영되는 동작 확인 |
| `mcp__dbhub`, `mcp__dbhub-mssql` | ask | 해당 DB MCP 연결 시 | 서버 전체 도구에 확인 적용. 조회/변경 구분은 실제 도구와 DB 계정 권한으로 검토 |
| `Bash(git status *)`, `Bash(git diff *)`, `Bash(git log *)` | allow | 공통 | 작업 상태 · 변경 · 이력 조회 |
| `Bash(git add *)`, `Bash(git commit *)` | allow | 공통 | 로컬 Git 작업 허용. 현재 secret-scan이 최종 커밋의 시크릿 부재를 보장하지는 않는다 |
| `mcp__github__get_file_contents`, `mcp__github__list_pull_requests`, `mcp__github__pull_request_read`, `mcp__github__list_issues` | allow | github MCP | 읽기 전용 조회 |
| `mcp__item-bank__search_items` | allow | item-bank MCP | 읽기 전용 문항 검색 |
| `WebFetch(domain:developer.mozilla.org)` | allow | node | 지정 공식 문서 열람 |
| `block-dangerous.mjs`: 일부 `rm -rf` 옵션 변형 탐지 | Hook | 공통 | `-fr`, `-r -f`, `--recursive --force` 등의 추가 탐지. 모든 인용 · 실행 형태를 해석하지는 않는다 |
| `block-dangerous.mjs`: `DROP TABLE` / `DROP DATABASE` 탐지 | Hook | 공통 | 해당 문자열 탐지. SQL 주석 변형을 놓치거나 문서용 문자열을 차단할 수 있다 |
| `block-dangerous.mjs`: `prod-db` / `prod.db` 탐지 | Hook | 공통 | deny와 겹치는 보조 방어. 대소문자 · 실제 주소 · 모든 우회를 다루는 보호는 아니다 |
| `block-dangerous.mjs`: `chmod 777` 탐지 | Hook | 공통 | 해당 권한 변경 형태 탐지. 다른 모드 표현까지 통제하지는 않는다 |
| `secret-scan.mjs`: Bash 실행 전 현재 staged diff의 AKIA 형식 AWS Access Key ID · `password = "..."` · PEM 개인키 탐지 | Hook | 공통 | `git commit` 문자열을 포함할 때만 검사. 최종 커밋 대상 및 실행 저장소와 불일치 가능 |
| `Bash(./gradlew test *)`, `Bash(./gradlew check *)`, `Bash(./gradlew build *)` | allow | spring | 현재 API 테스트는 test 프로필의 H2 사용. 다운로드 · 산출물 · 사용자 정의 task와 추가 인자 영향 확인 필요 |
| `Bash(./gradlew bootRun *)` | ask | spring | 포트 점유 · 설정된 DB 및 외부 서비스 연결 확인 |
| `WebFetch(domain:docs.spring.io)`, `WebFetch(domain:docs.gradle.org)` | allow | spring | 지정 공식 문서 열람 |
| `Bash(npm test *)`, `Bash(npm run test *)`, `Bash(npm run lint *)`, `Bash(npm run typecheck *)`, `Bash(npm run build *)` | allow | node | Web은 mock HTTP로 검증하지만 characterization은 실제 HTTP 요청. 같은 명령이라도 모듈 · 대상 주소 확인 필요 |
| `Bash(npm install *)`, `Bash(npm i *)`, `Bash(npm ci *)` | ask | node | 다운로드 · 설치 스크립트 확인. lockfile 고정만으로 실행 위험이 없어지지 않는다 |
| `Bash(pip install *)` | ask | python 사용 시 | 다운로드 · 패키지 설치 영향 확인 |
| `Bash(docker compose down *)`, `Bash(docker compose --profile *)` | ask | docker | 서비스 중단 · 선택 프로필의 기동 대상 확인 |

## 보완 후보: 아직 실제 설정 · Hook에 미적용

| 보완 내용 | 종류 · 범위 | 적용 이유와 기준 |
|---|---|---|
| 확인한 모듈의 `./gradlew clean` 허용 | allow 후보 · spring | 빌드 산출물 정리. 다른 task가 함께 실행되지 않는지 확인하고 강제 삭제의 대안으로 사용 |
| `--refresh-dependencies`를 clean과 분리 | ask 후보 · spring | 의존성 정보를 외부 저장소에서 재확인하는 옵션. 산출물 정리용으로 설명하지 않는다 |
| 로컬 `bootJar`와 `publish` · Docker push · AWS 배포 · kubectl 적용 구분 | allow/ask 후보 · 해당 스택 | 현재 Gradle에는 bootJar 자동 배포 연결이 없다. 외부 반영 명령은 대상 환경과 승인 범위를 확인 |
| 공개 설정 검토 · 비밀값 열람 · 설정 변경 구분 | 권한 후보 · spring | `application-prod*`라는 이름만으로 전부 Read/Edit deny하지 않는다. 비밀값이 있는 경로는 보호하고, 값 없는 예제와 일반 설정 변경 정책을 분리 |
| Spring 설정 수정의 영향 확인 | ask 후보 · spring | 공통 · dev/stg/prod/prd 설정, `.yml`/`.yaml`/`.properties`와 외부 주입을 함께 확인. DB · 커넥션 풀 · OSIV 변경은 영향 범위를 제시 |
| 운영 접속 보호를 실제 실행 환경에 연결 | 권한/Hook 보완 후보 · spring/DB | bootRun · java -jar의 인자뿐 아니라 환경변수 · JVM 옵션 · 기본 프로파일과 실제 DB/S3/Redis 대상을 확인. 승인 없는 운영 변경을 막고 DB/IAM 권한과 함께 통제 |
| 모듈별 검증 명령과 연결 대상 확인 | 권한 후보 · spring/node | modern/api의 H2, modern/web의 mock HTTP, characterization의 `TARGET_BASE_URL`을 구분. 추가 task · 설치/실행 스크립트를 확인한 범위만 허용 |
| secret-scan의 설정 형식 확대 | Hook 후보 · 공통 | YAML/JSON · 따옴표 없는 properties, datasource 비밀번호, JWT 서명키, JDBC 내 인증정보, AWS secretAccessKey · 임시 자격 증명 등을 검사 |
| 검사 대상과 최종 커밋 내용 일치 | Hook/커밋 검사 보완 후보 · 공통 | `commit -a`, 파일 지정, `git add && git commit`, `git -C/-c`, 다른 저장소로 이동, 공백 · 별칭을 고려. 실제 커밋 대상 검사와 pre-commit/CI 보완을 검토 |
| 검사 불능 시 해당 작업 차단 | Hook 후보 · 공통 | JSON 파싱 실패 · Git 검사 실패 · 실행 오류 · timeout을 성공으로 처리하지 않는다. Claude의 실패 처리와 차단 종료코드를 확인 |
| 차단 로그의 비밀값 · 명령 원문 출력 금지 | Hook 후보 · 공통 | block-dangerous는 현재 명령 전체를 stderr에 출력한다. secret-scan처럼 규칙명 · 건수 · 이유만 출력하도록 보완 |
| 서버 로그의 개인정보 · 토큰 노출 예방 | 검사 후보 · spring | 실행 후 Hook만으로 이미 대화에 유입된 값을 회수할 수 없다. 애플리케이션 로그 마스킹과 검증을 우선하고 실행 출력의 보호 방법을 별도 확인 |

## 예외 설계와 스택별 분리

| 항목 | 수정된 판단 · 권장 처리 |
|---|---|
| deny · ask · allow 충돌 | 평가 순서는 deny → ask → allow이며 구체적인 allow도 넓은 deny/ask를 이기지 못한다. 예외는 원래 차단 범위와 Hook 판단을 함께 재설계한 뒤 검증 |
| curl/wget과 로컬 헬스체크 | 현재 deny를 유지하면 localhost도 차단된다. 승인된 로컬 진단을 허용하려면 deny 범위를 조정하고 목적지 · 옵션 · 리다이렉트 · 전송 내용을 검증. localhost allow 추가만으로 해결하지 않는다 |
| `.env.example` 등 값 없는 예제 | `.env.*` deny에 걸리므로 allow 추가만으로 예외가 되지 않는다. 실제 비밀값 부재를 확인하고 예제와 비밀 파일의 경로 · deny 범위를 분리 |
| 삭제 · 재설치 절차 | 모듈이 확인된 clean 같은 정리 명령을 우선한다. npm ci는 기존 node_modules를 교체하고 설치 스크립트도 실행할 수 있으므로 기존 ask를 자동 완화하지 않는다. 삭제 예외는 대상 경로와 Hook도 함께 검토 |
| secret-scan 오탐 예외 | 테스트 경로 전체를 제외하지 않는다. 검증된 더미값 · 개별 예외만 좁게 허용하고 실제 키가 테스트 파일에 들어가도 탐지되는지 확인 |
| 인자 없는 Gradle 호출 | 공식 규칙상 후행 ` *`가 유일한 wildcard이면 인자 없는 명령도 매칭된다. `test*`/`build*`로 넓히는 제안은 제거. 설치된 Claude 버전에서 확인 |
| Git 원격 반영 | 현재 push ask와 force deny는 유지. 브랜치 보호만으로 모든 push를 자동 허용하지 않고, 사용자 승인 대상 · 범위와 명령 변형을 확인 |
| DB MCP · MSSQL | 실제 연결한 MCP 서버에 적용. Spring과 MSSQL은 배타적인 스택이 아니며 이 저장소에도 `legacy/grade-mssql`이 있다 |
| Flyway/Liquibase | 현재 modern/api에는 미도입. 사용하는 프로젝트에서 clean은 파괴 작업, migrate/update는 스키마 변경 작업으로 분류해 승인 · 환경 정책을 별도 설계 |
| python · php · terraform | 사용하는 프로젝트의 별도 정책으로 분리. pip/composer 설치와 terraform apply/destroy의 공급망 · 인프라 영향을 확인하며 Spring 규칙의 현재 누락으로 표현하지 않는다 |
| 공식 문서 도메인 | 실제 사용하는 스택별로 관리. 지정 도메인 allow가 다른 도메인의 WebFetch를 일괄 deny한다는 의미는 아니다 |
| Read/Edit 보호의 범위 | 인식 가능한 파일 도구/명령에는 적용되지만 임의 Python/Node 파일 I/O나 간접 검색 전체를 막지는 못한다. 모든 프로세스의 접근 통제가 필요하면 sandbox 등 실행환경 보호도 함께 검증 |

## 기존 팀 규칙 참조와 보완 기준

- `CLAUDE.md`: 기존 API 계약 보존, 요청 외 변경 금지, 사용자 변경 보존, 테스트 우회 금지와 특성화 테스트 완료 기준을 따른다. 이 문서에 같은 규칙을 중복 작성하지 않는다.
- `templates/secure-coding-checklist.md`: 개인별 SSO/MFA · 임시 인증, 모든 AWS 클라이언트의 고정 키 제거, 외주 개발 Secret · 회수, 프런트 공개 환경변수, S3/CORS · 파일 권한, 실제 배포 프로파일 · 환경별 리소스 점검을 참조한다.
- `templates/approval-checklist.md`: 실행 근거 없는 통과 주장 금지와 변경 범위 · 설정 · DB 스키마 · 의존성 승인 기준을 참조한다.
- 추가 보완 기준: 대량 변경 전 대상 파일 수 · 변경 범위 · API/데이터 계약 영향 · 검증 계획을 제시하고 사용자 승인을 받는다. 상세 작업 기준은 CLAUDE.md의 변경 범위 규칙에 연결할 후보이며 이번에는 해당 파일을 수정하지 않았다.
- 추가 보완 기준: 완료 보고는 소스/설정 확인, 실행한 테스트, 실제 런타임, AWS/배포 검증을 구분하고 미검증 · 기존 실패를 명시한다. 승인 체크리스트의 완료 기준에 연결할 후보이며 이번에는 해당 파일을 수정하지 않았다.
- 여러 단계의 실행 절차는 규칙 표를 늘리기보다 별도 Skill · 실행 가이드로 분리한다.

## 현재 구현 한계와 실제 적용 전 확인

- 두 Hook은 stdin JSON 파싱 실패 시 통과하며, secret-scan은 Git 검사 예외도 통과한다. block-dangerous의 정규식은 일부 문자열 변형을 탐지하는 보조 방어다.
- `prod-db` / `prod.db`는 deny와 Hook에 겹쳐 있다. 중복이라는 이유만으로 제거하지 말고 각 보호 범위와 실제 접속 통제를 확인한다. `chmod 777` 탐지는 Hook에만 있다.
- Hook은 `scripts/hook-node.sh`로 Node를 찾는다. 개발자별 버전 일치보다 지원 버전 · 실행 경로 · 누락/오류 처리 검증이 필요하다.
- Claude 버전과 공유 · 로컬 · 사용자/관리 설정을 확인하고 `/permissions`에서 유효 규칙을 점검한다. `onFailure: "block"`은 Claude Code 2.1.295 이상 지원 사항이므로 버전 확인 후 검토한다.
- 임시 저장소와 비밀값이 아닌 더미 입력으로 정상 허용 · 차단 · 예외 · Hook 오류/timeout · 커밋 대상 불일치를 검증한 뒤 적용한다. 실제 운영 DB · 자격 증명을 테스트에 사용하지 않는다.
- 이 문서는 소스/문서 확인 결과다. 실제 Claude 권한 차단 · Hook 통합 · 서버/DB · AWS/배포 실행을 검증한 결과가 아니다.

## 공식 근거

- [Claude Code 권한 규칙 · 우선순위 · 매칭 · 파일 보호 범위](https://code.claude.com/docs/en/permissions)
- [Claude Code Hook의 차단 · 오류 · timeout 처리](https://code.claude.com/docs/en/hooks)
- [Gradle 의존성 캐시 · refresh-dependencies](https://docs.gradle.org/current/userguide/dependency_caching.html)
- [npm ci와 설치 스크립트 설정](https://docs.npmjs.com/cli/v11/commands/npm-ci/)
