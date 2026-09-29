// 동작 보존 테스트 — 문항 은행(item-bank)의 문항 검색 화면 search.php
//
// 규칙 ID(BR-xx)는 docs/item-bank/BUSINESS-RULES.md 를 따른다.
// 입력 값은 db/mariadb/init/02-seed.sql 의 시드 데이터를 기준으로 골랐다.
//   - 공개(A) 이면서 난이도 5 미만인 문항은 정확히 20건(한 페이지 크기와 같다)
//   - 단원 코드 M5-1 ~ M6-2, 태그 계산 · 문장제 · 개념 · 오답률높음 · 서술형 · 도형
//
// 규칙
// - 대상 주소는 lib/target.mjs 가 정한다(환경 변수 TARGET_BASE_URL, 없으면 레거시 기본 주소).
// - 응답은 fetchNormalized 로 {status, rows, count, message} 모양으로 바꾼 뒤 스냅샷과 비교한다.
// - 기대값을 손으로 적지 않는다. 지금 레거시의 실제 응답이 기대값이다(npm run baseline -- item-bank).
// - 레거시와 새 API 양쪽에서 같은 결과가 나와야 하므로 건너뛰기를 두지 않는다.
import { describe, expect, it } from 'vitest';
import { fetchNormalized } from '../lib/target.mjs';

const MODULE = 'item-bank';
const PATH = '/search.php';

const search = (params) => fetchNormalized(MODULE, PATH, params);

// 키워드 경계값: '%' 98자 + '분수' = 100자. 101자는 끝에 'Z' 를 붙인다.
// 100자로 잘리면(BR-05) 두 요청의 결과가 같고, 잘리지 않으면 101자 쪽이 0건이 되어 차이가 드러난다.
const Q_100 = '%'.repeat(98) + '분수';
const Q_101 = Q_100 + 'Z';

// 태그 경계값: 50자 / 51자 (tag.name VARCHAR(50))
const TAG_50 = '태'.repeat(50);
const TAG_51 = '태'.repeat(51);

// 아주 긴 키워드. 요청 줄 길이 제한(Apache 기본 8190바이트)에 걸리지 않도록 500자로 둔다.
const Q_LONG = '가'.repeat(500);

describe('item-bank · 문항 검색(search.php)', () => {
  it('[정상 입력] 조건 없는 기본 검색 — 공개 문항만, 난이도 5 제외, 기본 정렬 (BR-01, BR-12, BR-19, BR-25)', async () => {
    expect(await search({})).toMatchSnapshot();
  });

  it('[정상 입력] 키워드 · 단원 · 난이도 복합 조건 AND 결합 (BR-04, BR-06, BR-09, BR-11, BR-13)', async () => {
    expect(await search({ q: '분수', unit: 'M5-1', level: '3' })).toMatchSnapshot();
  });

  it('[정상 입력] 태그 필터 + 제목 내림차순, 동률 보조 정렬 id ASC (BR-15, BR-21, BR-22, BR-29)', async () => {
    expect(await search({ tag: '계산', sort: 'title', dir: 'desc' })).toMatchSnapshot();
  });

  it('[경계값] 난이도 5 명시 — 빈값과 달리 5 포함, 검수 중 문항 제외 (BR-12, BR-13, BR-01)', async () => {
    expect(await search({ level: '5' })).toMatchSnapshot();
  });

  it('[경계값] 난이도 6 — 1~5 범위 바로 위, 경고 후 그대로 조회 (BR-13, BR-14)', async () => {
    expect(await search({ level: '6' })).toMatchSnapshot();
  });

  it('[경계값] 난이도 0 — 1~5 범위 바로 아래 (BR-14)', async () => {
    expect(await search({ level: '0' })).toMatchSnapshot();
  });

  it('[경계값] 전체 20건일 때 2페이지 — 건수는 있고 행은 빈 페이지 (BR-25, BR-27, BR-28)', async () => {
    expect(await search({ page: '2' })).toMatchSnapshot();
  });

  it('[경계값] 페이지 999 — 최대 페이지 그대로 (BR-26)', async () => {
    expect(await search({ page: '999' })).toMatchSnapshot();
  });

  it('[경계값] 페이지 1000 — 999로 보정 (BR-26)', async () => {
    expect(await search({ page: '1000' })).toMatchSnapshot();
  });

  it('[경계값] 키워드 100자 — 잘리지 않는 최대 길이 (BR-05, BR-06, BR-07)', async () => {
    expect(await search({ q: Q_100 })).toMatchSnapshot();
  });

  it('[경계값] 키워드 101자 — 100자로 잘려 100자 요청과 같은 결과 (BR-05, BR-06, BR-07)', async () => {
    expect(await search({ q: Q_101 })).toMatchSnapshot();
  });

  it('[경계값] 키워드 1자 — 경고만 하고 검색 수행 (BR-08, BR-06)', async () => {
    expect(await search({ q: '비' })).toMatchSnapshot();
  });

  it('[경계값] 태그 50자 — 잘리지 않는 최대 길이 (BR-16, BR-18)', async () => {
    expect(await search({ tag: TAG_50 })).toMatchSnapshot();
  });

  it('[경계값] 태그 51자 — 50자로 잘림 (BR-16, BR-18)', async () => {
    expect(await search({ tag: TAG_51 })).toMatchSnapshot();
  });

  it('[빈 값 · 누락] 난이도 빈 문자열 명시 — 파라미터 누락과 같은 경로 (BR-12)', async () => {
    expect(await search({ level: '' })).toMatchSnapshot();
  });

  it('[빈 값 · 누락] 공백뿐인 키워드 · 단원 · 태그 — trim 후 조건 없음 (BR-05, BR-09, BR-15)', async () => {
    expect(await search({ q: '  ', unit: ' ', tag: ' ' })).toMatchSnapshot();
  });

  it('[이상한 값] 음수 페이지 — 1페이지로 보정 (BR-26)', async () => {
    expect(await search({ page: '-1' })).toMatchSnapshot();
  });

  it('[이상한 값] 아주 긴 키워드 500자 — 100자로 잘림 (BR-05)', async () => {
    expect(await search({ q: Q_LONG })).toMatchSnapshot();
  });

  it('[이상한 값] 형식은 맞지만 없는 단원 코드 Z9-9 (BR-10, BR-11)', async () => {
    expect(await search({ unit: 'Z9-9' })).toMatchSnapshot();
  });

  it('[이상한 값] 등록되지 않은 태그 (BR-18)', async () => {
    expect(await search({ tag: '없는태그' })).toMatchSnapshot();
  });

  it('[이상한 값] 숫자로 시작하는 난이도 3a — 정수 변환 (BR-14)', async () => {
    expect(await search({ level: '3a' })).toMatchSnapshot();
  });

  it('[이상한 값] 키워드 % 한 글자 — 와일드카드로 처리 (BR-07, BR-08)', async () => {
    expect(await search({ q: '%' })).toMatchSnapshot();
  });

  it('[이상한 값] 소문자 단원 코드 m5-1 — 콜레이션에 따른 대소문자 비교 (BR-09, BR-10)', async () => {
    expect(await search({ unit: 'm5-1' })).toMatchSnapshot();
  });

  it('[이상한 값] 알 수 없는 정렬 기준 · 방향 — 기본 정렬 사용 (BR-20, BR-21)', async () => {
    expect(await search({ sort: 'foo', dir: 'up' })).toMatchSnapshot();
  });

  it('[이상한 값] 배열 파라미터 level[] — 첫 번째 값만 사용 (BR-03)', async () => {
    expect(await search({ 'level[]': ['3', '5'] })).toMatchSnapshot();
  });
});
