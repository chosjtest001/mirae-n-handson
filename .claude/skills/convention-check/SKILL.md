---
name: convention-check
description: 변경된 코드가 CLAUDE.md의 코딩 컨벤션과 금지 사항을 지키는지 점검하고 위반 목록을 파일:줄번호와 함께 보고한다. "컨벤션 점검", "커밋 전 점검", "리뷰 전에 확인", "규칙 위반 있는지 봐 줘" 같은 요청을 받거나 커밋·PR 직전에 변경 사항을 검사해야 할 때 사용한다.
argument-hint: "[점검할 경로 ...]"
allowed-tools:
  - Read
  - Grep
  - Glob
  - Bash(git diff *)
  - Bash(git status *)
---

# 컨벤션 점검

## 원칙

- 코드를 직접 고치지 않는다. 위반 위치와 수정 방향만 보고한다.
- 근거 라인을 댈 수 없는 지적은 하지 않고 "확인 필요"로 남긴다.

## 1. 점검 대상 정하기

- 인자로 경로를 받았으면 그 경로 아래의 파일만 점검한다: $ARGUMENTS
- 인자가 없으면 `git status --porcelain`과 `git diff --name-only HEAD`로 변경 파일을 모은다. `??`로 표시된 아직 add 하지 않은 새 파일도 포함한다.
- 삭제된 파일과 바이너리 파일은 제외한다. 대상이 없으면 "점검 대상 없음"만 출력하고 끝낸다.
- 변경 파일은 `git diff HEAD -- <파일>`로 바뀐 줄을 먼저 보고, 판정에 필요하면 파일 전체를 Read로 읽는다.

## 2. 점검 항목

적용 범위가 맞는 파일에만 점검한다. api는 `modern/api/`, web은 `modern/web/`이다.

| # | 항목 | 범위 | 위반 판정 기준 |
|---|---|---|---|
| 1 | 보호 경로 수정 금지 | 전체 | `legacy/`, `vendor-prs/*.patch`, `incident-logs/` 아래 파일이 변경 목록에 있음 |
| 2 | 컨트롤러의 Repository 주입·SQL 실행 금지 | api | `*Controller`의 필드나 생성자 인자에 `*Repository`, `JdbcTemplate`, `EntityManager`가 있음 |
| 3 | JPA 엔티티 직접 반환 금지 | api | 컨트롤러 메서드 반환 타입(`List<>`, `ResponseEntity<>` 안쪽 포함)이 `@Entity` 클래스임 |
| 4 | 컨트롤러 try-catch 오류 응답·빈 catch 금지 | api | 컨트롤러 안에 `try` 블록이 있음, 또는 어느 파일이든 본문이 비어 있는 `catch` 블록이 있음 |
| 5 | 표준 출력 로그 금지 | api | `System.out.println`, `System.err.println`, `printStackTrace()` 호출이 있음 |
| 6 | 쿼리 문자열 연결 금지 | api | `@Query`, `createQuery`, `createNativeQuery`, SQL 문자열에 변수를 `+`나 `String.format`으로 붙임 |
| 7 | 비밀 값 하드코딩 금지 | 전체 | `password`, `secret`, `token`, `apiKey` 같은 이름에 문자열 리터럴을 대입함 (`${...}` 플레이스홀더는 제외) |
| 8 | 프론트엔드 금지 문법 | web | 컴포넌트에서 `fetch(`를 직접 호출하거나 `http://`·`https://` 주소를 적음. `any`, `as unknown as`, `@ts-ignore`, `dangerouslySetInnerHTML`, 클래스 컴포넌트, `React.FC`, `defaultProps`, `console.log` 중 하나가 있음 |

- 새로 추가되거나 바뀐 줄에서 생긴 위반만 보고한다. 기존 코드의 위반은 "기존 위반"으로 따로 표시한다.
- 3번처럼 다른 파일을 봐야 판정할 수 있으면 Grep이나 Read로 확인한다. 확인하지 못했으면 "확인 필요"로 남긴다.

## 3. 출력 형식

아래 순서와 형식을 바꾸지 않는다.

```
## 판정 요약
- 점검 대상: N개 파일 (경로 목록)
- 결과: 통과 / 위반 K건 / 확인 필요 M건
- 항목별: 1 통과 · 2 위반 · 3 해당 없음 · ... · 8 통과

## 위반 목록
| 파일:줄번호 | 어긴 규칙 | 수정 방향 |
|---|---|---|
| modern/api/src/.../FooController.java:42 | 2. 컨트롤러의 Repository 주입 금지 | FooService를 주입하고 조회를 Service로 옮긴다 |
```

- 위반이 없으면 표에 "위반 없음" 한 행만 적는다.
- "확인 필요" 항목은 표의 어긴 규칙 칸에 `확인 필요: <항목>`으로 적고, 무엇을 확인해야 하는지 수정 방향 칸에 적는다.
