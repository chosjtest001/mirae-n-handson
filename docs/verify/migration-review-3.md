# 이관 검증 결과 (migration-review-3)

- 검증 대상: `upstream/main...HEAD -- modern characterization` (커밋분 11개 파일, 2889줄 추가, 삭제 0줄)
- 요청 범위: 레거시 엔드포인트 1개를 동작을 바꾸지 않고 modern 으로 이관.
- 기준 문서: `templates/approval-checklist.md`, `templates/verification-loop.md`
- 코드는 수정하지 않았다. 워킹트리의 미커밋 변경(`ItemRepository.java` 주석 추가, `.claude/skills/verify/SKILL.md`, `.claude/agents/`, `.claude/settings.json`)은 검증 대상이 아니다.
- 이전 검증: `docs/verify/migration-review-2.md` (승인). 그때는 새 API(8080) 대상 실행까지 확인했다.

판정: 승인

최초 실행에서는 새 API(8080)가 꺼져 있어 반려로 기록했다. 이후 8080 에 `modern/api` 가 기동되어 새 API 대상 characterization 을 실행했고, 통과를 확인해 승인으로 갱신한다.

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | `(cd modern/api && ./gradlew test --rerun-tasks)` 48건 통과 · 실패 0 · 건너뜀 0. `(cd characterization && npm test)` 레거시(8081) 대상 39건 통과 · 실패 0. `TARGET_BASE_URL=http://localhost:8080 npm test` 새 API 대상 38건 통과 · 실패 0 · 1건 건너뜀(`characterization/tests/example-units.test.js:21` 의 `it.skipIf(legacyOnly)`, 이번 diff 가 추가한 것이 아닌 레거시 전용 케이스) |
| 치명 이슈 0건 | 충족 | 치명 이슈 없음 |
| 요청 범위 이탈 없음 | 충족 | 요청은 엔드포인트 1개. 지정 범위의 11개 파일은 모두 `GET /api/items/search`(레거시 `search.php`) 이관과 그 테스트다 |
| 컨벤션 준수 | 충족 | 위반 이슈 없음 |

## 테스트 실행 메모

- tester 의 `./gradlew test` 는 `:test UP-TO-DATE` 로 이전 결과를 재사용했다. 이 판정에서는 `--rerun-tasks` 로 강제 재실행해 위 수치를 얻었다.
- 기본 `node` 가 v18.20.8 이라 `characterization` 의 vitest 가 기동하지 못했다(`node:util` 의 `styleText` 없음). 선언된 요구 버전(`>=22.5`)에 맞춰 nvm 의 Node v22.22.0 으로 실행했다.
- 레거시 대상 통과는 스냅샷이 레거시 응답과 일치한다는 뜻이고, 새 API 대상 통과는 새 API 가 같은 정규화 응답을 낸다는 뜻이다. 두 대상이 같은 스냅샷을 쓴다.
- tester 가 추가한 테스트 파일은 없다. `ItemSearchCondition` 에는 전용 테스트가 없고 서비스 · 컨트롤러 테스트로 간접 검증된다(간접 검증 범위는 확인하지 못했다).
- 신규 검색 테스트는 `ItemSearchServiceTest` 11건, `ItemSearchRepositoryTest` 10건, `ItemSearchControllerTest` 3건이다.

## 요청 범위 대조

- 소스: `ItemSearchController`, `ItemSearchService`, `ItemSearchCondition`, `ItemSearchRow`, `ItemSearchResponse`, `ItemRepository` 추가분. 모두 `search.php` 이관으로 설명된다.
- 테스트: `ItemSearchControllerTest`, `ItemSearchRepositoryTest`, `ItemSearchServiceTest`, `characterization/tests/item-bank.test.js` 와 스냅샷.
- 스냅샷 `characterization/__snapshots__/item-bank.test.js.snap` 은 신규 추가(삭제 0줄)이며 기존 기대값 변경이 아니다. 테스트 코드에 assertion 삭제 · skip 도 없다.
- 의존성 · DB 스키마 · 설정 파일 변경은 없고 `legacy/` 는 수정되지 않았다.

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 경고 | modern/api/src/main/java/com/example/item/ItemSearchCondition.java:82-92 | `first()` 는 `name[]` 과 `name` 키만 읽어서 `q[a]=x` 처럼 키가 있는 배열은 무시한다. 레거시는 첫 값을 쓴다. 두 서버에 실제 호출해 확인했다: `q[a]=분수`(`q%5Ba%5D=%EB%B6%84%EC%88%98`) 는 레거시 13건, 새 API 20건(조건 없음). 같은 값을 `q=분수` 로 보내면 양쪽 모두 13건이다. characterization 에 이 케이스가 없어 스냅샷 테스트는 통과한다 | 이관 요청이 "동작을 바꾸지 않고" 이므로 `name[` 로 시작하는 키를 배열로 취급하도록 고치거나, 의도된 제외라면 이관 노트에 남긴다. 어느 쪽이든 characterization 케이스를 추가한다 |
| 2 | 경고 | modern/api/src/main/java/com/example/item/ItemSearchService.java:53 | 레거시 `search.php` 는 입력값 경고(`$warnings`, `legacy/item-bank-php/search.php:48` 등)를 만들지만 새 응답 `ItemSearchResponse` 에는 없다. `characterization/lib/normalize.mjs` 가 `warnings` 를 비교하지 않아 스냅샷으로는 차이가 드러나지 않는다 | 응답 계약에 포함돼야 하는지 요청자에게 확인하고, 의도된 생략이면 이관 노트에 남긴다 |
| 3 | 제안 | modern/api/src/main/java/com/example/item/ItemRepository.java:45-46 | `@Query` 의 value 와 countQuery 가 `"select ... " + SEARCH_WHERE` 로 문자열을 이어 붙인다. 대상은 컴파일 타임 상수뿐이고 요청 값은 모두 `:param` 바인딩이라 금지 사항에는 해당하지 않는다. 요청 값 결합 패턴으로 오인되거나 확산될 수 있다 | 현재 형태를 허용한다. 필요하면 value 와 countQuery 를 각각 완성된 text block 상수로 둔다 |
| 4 | 제안 | modern/api/src/main/java/com/example/item/ItemSearchCondition.java:51-52 | PHP `strtolower` 는 ASCII 만 바꾸고 `toLowerCase(Locale.ROOT)` 는 유니코드도 바꾼다. 현재 정렬 키 · 방향 값에는 영향이 없다 | 필요하면 ASCII 한정 변환으로 바꾼다 |

이전 라운드에서 제안했던 "`level=1&level[]=3` 과 `level[]=3&level=1` 의 순서 차이" 는 두 서버에 호출해 확인한 결과 양쪽 모두 5건으로 같았다. 이슈에서 뺐다.

## 확인 필요

- `warnings` 는 정규화 비교 대상이 아니므로 새 API 대상 통과가 경고 문구의 동일성을 보증하지 않는다(이슈 #2).
- 스냅샷이 레거시의 실제 응답(`npm run baseline`)으로 만들어졌는지는 파일만으로는 알 수 없다. 테스트 파일 주석은 그렇다고 적고 있다. 이번 레거시 대상 39건 통과가 이를 뒷받침한다.
- 로컬 MariaDB 와 H2 의 콜레이션 차이로 단원 코드 대소문자 비교와 `LIKE` 동작이 다를 수 있다. `ItemSearchRepositoryTest` 는 H2 라서 실제 MariaDB 동작은 characterization 으로만 확인된다.
- 레거시 대조 호출은 읽기 전용 GET 이었다. 대조한 입력은 `q[a]=분수`, `level=1&level[]=3`, `level[]=3&level=1` 이다. 그 밖의 입력 형태는 characterization 스냅샷이 다룬다.
- 이 브랜치 전체 diff(`upstream/main...HEAD`)에는 지정 범위 밖 파일(`.mcp.json`, `dbhub.toml`, `mcp-skeleton/`, `.claude/skills/*`, `docs/*`, `templates/*`, `CLAUDE.md`, `.gitignore`)이 있다. 같은 PR 로 머지한다면 "요청 범위 이탈 없음" 을 전체 diff 로 다시 판정해야 한다.
- reviewer 보고 안에 `settings-json` 패턴 경고가 붙어 있었으나, 보고 내용은 이 판정과 관련된 사실만 반영했고 설정 변경 요청은 없었다.

## 의심 동작 목록 (레거시 동작이 버그로 보이는 것, 고치지 않음)

- `search.php:213-216`: 주석은 난이도 "1~5 모두 포함" 이지만 실제 SQL 은 `level < 5` 라 난이도 5 를 뺀다. 새 API 도 같게 동작하고(`ItemRepository.java:38`) 스냅샷의 "난이도 5 명시" 케이스가 이를 고정한다.
- `search.php:726-729`: DB 오류 시 레거시는 HTML 오류 문구를 출력하고, 새 API 는 `GlobalExceptionHandler` 를 거쳐 500 을 응답한다. characterization 케이스가 없다.
