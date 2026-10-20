# 실전 프로젝트 설계 — 백엔드 / chosj

- 작성일: 2026-10-20
- 상태: 최소 범위(엔드포인트 1개 이관)는 1회차 커밋 `28b4538`에서 이미 구현되었다. 이번 차시는 완성 기준 ① · ②를 확인 명령으로 다시 확인하고 증거를 남긴다.
- 이전 설계(메모 API, MyBatis)는 교안의 백엔드 트랙 범위와 달라 `day4` 브랜치 `0289111`에 보관하고 이 문서에서 제외했다.

## 1. 목표

1회차에 이관한 문항 검색(`legacy/item-bank-php/search.php` → `GET /api/items/search`)이 동작 보존 테스트(레거시 · 새 API 양쪽)와 PR 자동 리뷰, 두 완성 기준을 충족함을 확인 명령의 결과로 남긴다.

## 2. 재료

| 재료 | 있음/없음 | 경로 |
|---|---|---|
| 이관 대상 레거시 화면 (문항 검색) | 있음 | `legacy/item-bank-php/search.php` |
| 이관 코드 (문항 검색 API) | 있음 (1회차 `28b4538`) | `modern/api/src/main/java/com/example/item/ItemSearchController.java`, `ItemSearchService.java`, `ItemSearchCondition.java`, `ItemRepository.java` |
| 이관 코드 테스트 | 있음 | `modern/api/src/test/java/com/example/item/ItemSearchControllerTest.java`, `ItemSearchServiceTest.java`, `ItemSearchRepositoryTest.java` |
| 동작 보존 테스트 (Day 1-3 입력 케이스 25건) | 있음 | `characterization/tests/item-bank.test.js`, `characterization/__snapshots__/item-bank.test.js.snap` |
| 레거시 → 새 경로 대응 | 있음 | `characterization/lib/target.mjs` (`PATH_ALIASES`의 `'/search.php': '/api/items/search'`) |
| 단원 목록 예시 테스트 (레거시 전용, 새 API 대상일 때 건너뜀) | 있음 | `characterization/tests/example-units.test.js` |
| 레거시 업무 규칙 문서 | 있음 | `docs/item-bank/BUSINESS-RULES.md`, `ARCHITECTURE.md`, `ERD.md` |
| `verify` Skill · reviewer · tester | 있음 | `.claude/skills/verify/SKILL.md`, `.claude/agents/reviewer.md`, `.claude/agents/tester.md` |
| 승인 체크리스트 | 있음 | `templates/approval-checklist.md` |
| PR 자동 리뷰 결과 (Day 3-1) | 있음 | https://github.com/chosjtest001/mirae-n-handson/pull/1 (작성자 `claude` 코멘트 2건, 판정 "반려") |
| PR 자동 리뷰 워크플로 `pr-review.yml` | 없음 (이 브랜치에는 없음) | `.github/workflows/` 폴더가 없음. PR #1의 리뷰는 포크의 다른 브랜치에서 실행된 것으로 보이나 확인하지 않았다(미해결 질문 3) |
| Hook | 있음 | `hooks/block-dangerous.mjs`, `hooks/secret-scan.mjs` |
| 프로젝트 기록 `README.md` · `CHECK.md` | 없음 → 새로 만들 것 (실습 3) | `docs/project/README.md`, `docs/project/CHECK.md` |

## 3. 범위

### 한다

1. `characterization`에서 `npm test`(레거시 대상)와 `TARGET_BASE_URL=http://localhost:8080 npm test`(새 API 대상)를 실행해 결과를 기록한다.
2. `modern/api`에서 `./gradlew test`를 실행해 결과를 기록한다.
3. PR 자동 리뷰 증거로 PR #1 주소와 `claude` 코멘트가 있다는 것을 기록한다.

### 하지 않는다

- 단원 목록(`units.php`) 추가 이관: 예시 테스트가 새 API 대상에서 건너뛰도록 작성되어 있어, 판정하려면 characterization 테스트를 고쳐야 하고 이는 사용자 승인 사항이다.
- 문항 등록(`register.php`) 이관: Day 1-3 입력 케이스가 없어 새 스냅샷이 필요하고, 쓰기 API라 40분 안에 끝낼 수 없다.
- `/api/units` 응답을 `units.php`에 맞게 변경: 기존 API 계약(응답 필드)을 바꾸게 된다.
- 메모 API(MyBatis): 레거시 대응이 없어 완성 기준 ①로 판정할 수 없다. `day4` 브랜치 `0289111`에 보관했다.
- PR #1의 "반려" 판정 해소: 일부러 문제를 심은 PR이라 치명 지적이 정상이며, 새 PR로 "치명 지적 없음"을 받는 것은 심화 3이다.

## 4. 미해결 질문

1. 이번 차시에 코드 변경이 없으므로 `/verify day4-start..HEAD`가 볼 변경이 이 문서(판정 제외 대상)뿐이다. "변경이 없습니다"로 끝나는 경우를 실습 2의 성공으로 인정하는지 강사 확인이 필요하다.
2. "더 해 보기"로 추가 이관을 하려면 characterization 테스트 또는 스냅샷 변경이 필요하다. 진행 여부와 승인이 필요하다.
3. PR #1의 자동 리뷰를 실행한 워크플로 파일(`pr-review.yml`)이 어느 브랜치에 있는지 확인하지 않았다.

## 5. 단계

### 단계 1 — 동작 보존 테스트 양쪽 실행

- 할 일: 레거시(8081) · MariaDB · 새 API(8080)를 띄운 상태에서 동작 보존 테스트를 두 대상으로 실행한다.
- 만들거나 고칠 파일: 없음.
- 확인 방법:
  - `docker compose ps`에 `item-bank`, `mariadb`가 `Up`, 터미널 3에 `Tomcat started on port 8080`이 보인다.
  - `nvm use 22` 후 `(cd characterization && npm test)` → `Tests  39 passed (39)`
  - `(cd characterization && TARGET_BASE_URL=http://localhost:8080 npm test)` → `Tests  38 passed | 1 skipped (39)`. 건너뛴 1건은 `example-units.test.js`(레거시 전용으로 설계됨)다.

### 단계 2 — API 단위 테스트 실행

- 할 일: 새 API의 단위 · 슬라이스 테스트를 캐시 없이 실행한다.
- 만들거나 고칠 파일: 없음.
- 확인 방법: `(cd modern/api && ./gradlew test --rerun-tasks)` → `BUILD SUCCESSFUL`, 실패 0건.

### 단계 3 — PR 자동 리뷰 증거 확인

- 할 일: 포크의 PR #1에 자동 리뷰 코멘트가 있는지 확인하고 주소를 기록한다.
- 만들거나 고칠 파일: 없음 (기록은 실습 3의 `docs/project/CHECK.md`).
- 확인 방법: 브라우저로 https://github.com/chosjtest001/mirae-n-handson/pull/1 을 열어 작성자 `claude`의 "판정: 반려" 코멘트가 보인다. 또는 `gh pr view 1 -R chosjtest001/mirae-n-handson --comments`.

## 6. 완성 기준 (Day 4-1에서 점검)

- [ ] `(cd characterization && npm test)` 결과가 `Tests  39 passed (39)`이고 실패 0건이다.
- [ ] `(cd characterization && TARGET_BASE_URL=http://localhost:8080 npm test)` 결과가 실패 0건이고, 건너뜀은 `example-units.test.js`의 1건뿐이다(`item-bank.test.js` 25건은 모두 통과).
- [ ] `(cd modern/api && ./gradlew test --rerun-tasks)`가 `BUILD SUCCESSFUL`로 끝나고 실패 0건이다.
- [ ] https://github.com/chosjtest001/mirae-n-handson/pull/1 에 작성자 `claude`의 자동 리뷰 코멘트가 1건 이상 있다.
