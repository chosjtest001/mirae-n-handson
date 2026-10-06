# 이관 검증 결과 (migration-review-1)

- 검증 대상: `upstream/main...HEAD` (커밋 4개: f9a2681, e26326e, 28b4538, 2d5eb54)
- 요청 범위: 레거시 엔드포인트 1개를 동작을 바꾸지 않고 modern 으로 이관.
- 기준 문서: `templates/approval-checklist.md`, `templates/verification-loop.md`
- 코드는 수정하지 않았다.

판정: 반려

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | `(cd modern/api && ./gradlew cleanTest test)` 50건 통과 · 실패 0 · 건너뜀 0. `(cd characterization && npm test)` 레거시 대상 39건 통과. `TARGET_BASE_URL=http://localhost:8080 npm test` 새 API 대상 38건 통과 · 1건 건너뜀(`example-units.test.js:21` 의 `it.skipIf(legacyOnly)`, 이번 diff 이전부터 있던 코드). 실패 테스트 없음 |
| 치명 이슈 0건 | 충족 | 치명 이슈 없음 |
| 요청 범위 이탈 없음 | 미충족 | 요청은 엔드포인트 1개인데 `GET /api/items/search`(이관)와 `GET /api/units/item-counts`(추가) 2개가 들어 있다. 이슈 #1, #2 |
| 컨벤션 준수 | 충족 | 위반 없음. 제안 #4 참고 |

## 요청 범위 대조

- 이관 대상으로 확인되는 것: `legacy/item-bank-php/search.php` → `GET /api/items/search` (`ItemSearchController`, `ItemSearchService`, `ItemSearchCondition`, `ItemSearchRow`, `ItemSearchResponse`, `ItemRepository` 추가분과 해당 테스트 3개). 이 파일들은 요청으로 설명된다.
- 설명되지 않는 것: `/api/units/item-counts` 관련 5개 파일(소스 4 + 테스트 2 중 `UnitControllerTest`, `UnitRepositoryTest` 변경분). 레거시에 같은 이름의 엔드포인트는 없다(`units.php` 는 `/api/units` 로 이미 대응, `characterization/lib/target.mjs` 의 `PATH_ALIASES`). 내용은 `vendor-prs/pr-1-missing-tests.patch` 의 "단원별 문항 수 통계 API 추가"와 같은 기능이다.

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 경고 | modern/api/src/main/java/com/example/item/UnitController.java:25 | `GET /api/units/item-counts` 는 "이관 1개" 요청에 없는 신규 API 다. 레거시 대응 엔드포인트가 없고 characterization 으로 동작 보존을 확인할 수 없다 | 이 변경 범위에서 분리해 별도 요청 · 별도 PR 로 진행하거나, 포함하려면 요청 범위를 사람이 확정한다 |
| 2 | 경고 | modern/api/src/main/java/com/example/item/UnitRepository.java:15, UnitService.java:30, UnitItemCountResponse.java:3 | #1 의 구현 파일. 쿼리와 DTO 가 함께 추가되어 이관 diff 에 섞였다. 요청 범위 기준을 미충족으로 만드는 원인 | #1 과 같이 분리한다. 분리 시 관련 테스트(`UnitControllerTest`, `UnitRepositoryTest` 의 해당 케이스)도 함께 옮긴다 |
| 3 | 경고 | docs/item-bank/.BUSINESS-RULES.md.swp | 편집기 임시 파일(바이너리 16KB)이 커밋에 들어 있다. 요청과 무관하다 | 저장소에서 제거하고 `.gitignore` 에 `*.swp` 추가를 검토한다 |
| 4 | 제안 | modern/api/src/main/java/com/example/item/ItemSearchController.java:26 | 파라미터 전체를 `MultiValueMap` 으로 서비스에 넘기고 검증 · 정규화는 `ItemSearchCondition.from` 에서 한다. 레거시의 `level[]`, 중복 파라미터 처리를 보존하려는 의도는 주석(9-13행)에 있으며 CLAUDE.md 의 "컨트롤러는 요청 검증" 규칙과는 거리가 있다 | 규칙 예외라면 CLAUDE.md 나 주석에 근거를 남긴다. 동작은 바꾸지 않는다 |

## 의심 동작 (레거시 그대로 이관됨, 이관 중에는 고치지 않음)

CLAUDE.md 완료 기준에 따라 별도 보고한다. 모두 의도적으로 보존된 동작이다.

- `ItemRepository.java:32, 38` — `level` 을 비우면 난이도 5 문항이 결과에서 빠진다(`level < 5`).
- `ItemSearchCondition.java:57`, `ItemRepository.java:32` — 키워드의 `%` 와 `_` 를 이스케이프하지 않아 와일드카드로 동작한다.
- `ItemSearchCondition.java:40` — `page` 값 끝에 줄바꿈 한 개를 허용한다(PHP `$` 동작 재현).
- `ItemSearchCondition.java:60` — 존재하지 않는 단원 · 범위 밖 난이도는 오류가 아니라 0건이다.

## 확인 필요

- `legacy/assignment-thymeleaf/src/main/resources/application.properties` 는 작업 트리에서 수정된 상태지만 검증 범위(커밋 diff)에는 없다. 이번 변경의 일부인지 사람이 확인해 달라. (줄번호 근거 없음)
- `.claude/skills/`, `CLAUDE.md`, `docs/item-bank/*` 는 범위에 포함된 커밋(e26326e 등)의 산출물이다. 이관 요청의 부수 문서로 볼지 사람이 판단해 달라.
- 새 API 대상 characterization 은 8080 서버가 어떤 DB(MariaDB 여부)로 떠 있는지 확인하지 않았다. `./gradlew test` 는 H2 기준이다.

## 실행 환경 메모

- 셸 기본 Node 는 v18.20.8 이라 `characterization` 의 `npm test` 가 시작 오류(`node:util` 의 `styleText` 없음)로 실패한다. nvm 의 v22.22.0 으로 실행했다. CLAUDE.md 의 Node 22 이상 요구와 일치한다.
- `./gradlew test` 는 처음에 캐시(UP-TO-DATE)로 끝나 `cleanTest` 후 다시 실행했다.

## 다음 단계

#1, #2 를 분리(또는 사람이 범위에 포함하기로 확정)한 뒤 다시 검증한다. 이 두 건이 해소되면 나머지 기준은 모두 충족이다.
