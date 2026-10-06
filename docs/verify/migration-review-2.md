# 이관 검증 결과 (migration-review-2)

- 검증 대상: `upstream/main...HEAD` (최신 커밋 9b94df7 포함, 변경 파일 20개)
- 요청 범위: 레거시 엔드포인트 1개를 동작을 바꾸지 않고 modern 으로 이관.
- 기준 문서: `templates/approval-checklist.md`, `templates/verification-loop.md`
- 코드는 수정하지 않았다.
- 이전 검증: `docs/verify/migration-review-1.md` (반려). 이번은 그 지적(#1 ~ #3)의 반영 여부를 포함해 다시 본다.

판정: 승인

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | `(cd modern/api && ./gradlew cleanTest test)` 48건 통과 · 실패 0 · 건너뜀 0. `(cd characterization && npm test)` 레거시(8081) 대상 39건 통과 · 실패 0. `TARGET_BASE_URL=http://localhost:8080 npm test` 새 API 대상 38건 통과 · 실패 0 · 1건 건너뜀(`characterization/tests/example-units.test.js:21` 의 `it.skipIf(legacyOnly)`, 이번 diff 가 추가한 것이 아니고 레거시 전용 케이스) |
| 치명 이슈 0건 | 충족 | 치명 이슈 없음 |
| 요청 범위 이탈 없음 | 충족 | 요청은 엔드포인트 1개. 소스는 `GET /api/items/search`(레거시 `search.php`) 한 개만 추가된다. 이전 반려 사유였던 `/api/units/item-counts` 와 `.swp` 파일은 diff 에서 사라졌다(`git ls-files` 에 swp 없음) |
| 컨벤션 준수 | 충족 | 위반 없음. 제안 #1 참고 |

## 요청 범위 대조

- 이관 코드: `ItemSearchController`, `ItemSearchService`, `ItemSearchCondition`, `ItemSearchRow`, `ItemSearchResponse`, `ItemRepository` 추가분과 테스트 3개(`ItemSearchControllerTest`, `ItemSearchRepositoryTest`, `ItemSearchServiceTest`). 모두 `search.php` 이관으로 설명된다.
- 동작 보존 테스트: `characterization/tests/item-bank.test.js` 와 스냅샷. 이관 검증용이다.
- 이관 근거 문서: `docs/item-bank/*`. 이관을 위한 분석 산출물이다.
- 그 밖: `CLAUDE.md`, `.claude/skills/*`, `.gitignore`(`*.swp` 한 줄)는 이 이관의 작업 환경이다. 이전 라운드에서 같은 구성으로 확인했다. 의존성 · DB 스키마 · 설정 파일(`application.yml`, 빌드 파일) 변경은 없다.

## 체크리스트 대조 요약

- 테스트: 실제 실행했고 실패 0건. 기존 테스트의 삭제 · `@Disabled` · 스냅샷 기대값 수정은 없다(`characterization` 스냅샷은 이번 범위에서 신규 추가분).
- 치명: 하드코딩된 비밀값 없음. 쿼리는 `ItemRepository.java` 의 `SEARCH_WHERE` 가 `:status` 등 파라미터 바인딩을 쓰며, 요청 값으로 쿼리나 정렬 식을 이어 붙이지 않는다(정렬은 `ItemSearchCondition.sort` 의 고정 `Sort`). 데이터를 지우거나 덮어쓰는 코드 없음(`@Transactional(readOnly = true)`).
- 컨벤션: 컨트롤러에 Repository · SQL · `try-catch` 없음. `System.out` · `printStackTrace` · `console.log` 없음. 로그는 SLF4J(`ItemSearchService.java` 의 `log.debug`)이며 검색어 · 식별자 원문을 남기지 않는다. 엔티티를 직접 반환하지 않고 `ItemSearchRow` record 로 변환하며, 변환은 서비스 트랜잭션 안에서 한다. 연관 데이터는 `@EntityGraph` 로 가져온다. 생성자 주입, 인터페이스 없는 `@Service` 를 지켰다.

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 제안 | modern/api/src/main/java/com/example/item/ItemSearchController.java:26 | 파라미터 전체를 `MultiValueMap` 으로 서비스에 넘기고 검증 · 정규화는 `ItemSearchCondition.from` 에서 한다. `level[]` · 중복 파라미터 처리를 보존하려는 의도는 클래스 주석에 있으나 CLAUDE.md 의 "컨트롤러는 요청 검증" 규칙과는 거리가 있다 | 규칙 예외라면 근거를 CLAUDE.md 나 주석에 남긴다. 동작은 바꾸지 않는다 |

## 이전 지적 반영 확인

| 이전 # | 내용 | 상태 |
|---|---|---|
| 1, 2 | `/api/units/item-counts` 와 관련 파일 분리 | 해결. diff 에 해당 파일 없음 |
| 3 | `docs/item-bank/.BUSINESS-RULES.md.swp` 제거 | 해결. 추적 파일에 swp 없음 |
| 4 | 컨트롤러의 `MultiValueMap` 전달 | 제안 수준으로 유지(위 #1) |

## 확인 필요

- 근거 줄번호를 댈 수 없어 이슈로 올리지 않은 지적은 없다.
- 실행 환경 메모: `characterization` 은 Node 22 가 필요하다. 기본 Node 18 에서는 vitest 가 `node:util` 의 `styleText` 오류로 시작하지 못해, nvm 의 v22.22.0 으로 실행했다. 레거시(8081)와 새 API(8080)는 모두 기동되어 있었다.
- 이 문서에 적은 의심 동작(레거시 버그로 보이는 것)은 이번 검증에서 새로 발견하지 않았다. 의심 동작 목록은 `docs/item-bank/CROSS-CHECK.md` 를 따른다.
