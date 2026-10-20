# 백엔드 트랙 파이프라인 — 레거시 이관 동작 보존 검증

레거시 문항 검색(`search.php`)과 이관한 `GET /api/items/search`에 Day 1-3 입력 케이스 25건을 똑같이 보내 정규화한 응답을 스냅샷과 비교하고, `/verify` Skill이 reviewer · tester Sub-agent로 변경분의 승인 · 반려를 판정한다.
`(cd characterization && npm test && TARGET_BASE_URL=http://localhost:8080 npm test)` 실행 후 Claude Code에서 `/verify day4-start..HEAD`
이관 1개 엔드포인트만 검증하고(`units.php` 예시 테스트는 새 API 대상에서 건너뜀, `register.php`는 케이스 없음) 레거시 8081 · 새 API 8080 · 시드 MariaDB가 떠 있고 Node 22.5 이상이어야 하므로, 현업에서는 대상 주소와 시드 데이터를 운영과 같은 검증 환경으로 바꾸고 이 명령을 PR 자동 리뷰 단계에 함께 묶어야 한다.
