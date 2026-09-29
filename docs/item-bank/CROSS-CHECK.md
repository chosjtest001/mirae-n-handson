# legacy/item-bank-php 비즈니스 규칙 추출 (교차 검증용)

- 읽은 파일: `search.php`, `register.php`, `units.php`, `index.php`, `inc/db.php`
- 읽지 않은 곳: `vendor/`(외부 라이브러리), `docs/` 전체, `inc/layout.php`(화면 골격이라 범위 밖), DB 스키마·뷰 정의(`item-bank-php` 폴더 밖)
- 기준: 주석과 코드가 다르면 코드를 따른다. 코드에서 확인되지 않는 내용은 규칙에 넣지 않고 마지막 절에 따로 적었다.
- 경로는 모두 `legacy/item-bank-php/` 기준이다.

## 1. 검색 조건 조합 (`search.php`)

### CX-01 조건은 AND 로 결합한다
- 규칙: 키워드 · 단원 · 난이도 · 태그 조건은 모두 `AND` 로 이어 붙어, 지정한 조건을 전부 만족하는 문항만 조회된다.
- 근거: `search.php:44`, `search.php:98`, `search.php:118`
- 코드:
  ```php
  $where    = " WHERE 1=1";
  $where .= " AND (title LIKE ? OR stem LIKE ?)";
  $where .= " AND unit_code = ?";
  ```

### CX-02 각 파라미터는 배열로 들어와도 첫 값만 쓴다
- 규칙: `q`, `unit`, `level`, `tag`, `sort`, `dir`, `page` 가 배열이면 첫 값만 사용하므로 단원 · 태그 · 난이도의 다중 선택은 지원하지 않는다.
- 근거: `search.php:54-56`, `search.php:58-60`
- 코드:
  ```php
  if (isset($params['q'])) {
      $q = is_array($params['q']) ? (string)reset($params['q']) : (string)$params['q'];
  ```

### CX-03 키워드는 제목 또는 지문에 부분 일치하며 100자로 잘린다
- 규칙: 키워드는 앞뒤 공백을 제거하고 100자를 넘으면 앞 100자만 쓰며(경고 표시), 비어 있지 않으면 `title` 또는 `stem` 에 `%키워드%` LIKE 로 찾는다. `%`, `_` 는 이스케이프하지 않아 와일드카드로 동작한다.
- 근거: `search.php:85-89`, `search.php:94-98`
- 코드:
  ```php
  $like = '%' . $q . '%';
  $where .= " AND (title LIKE ? OR stem LIKE ?)";
  ```

### CX-04 단원은 코드 정확 일치이며 형식 오류도 거부하지 않는다
- 규칙: `unit` 은 `unit_code = ?` 로 대소문자 변환 없이 입력값 그대로 정확 일치 비교한다. 형식(`^[A-Za-z][0-9]{1,2}-[0-9]{1,2}$`) 위반, 소문자, 미등록 코드는 경고만 하고 조회는 그대로 수행한다.
- 근거: `search.php:111-120`, `search.php:141-143`
- 코드:
  ```php
  $warnings[] = '단원 코드는 대문자로 입력하세요. (입력값 그대로 조회합니다)';
  $where .= " AND unit_code = ?";
  $values[] = $unit;
  ```

### CX-05 난이도를 지정하지 않으면 난이도 5 문항이 제외된다
- 규칙: `level` 이 비어 있으면 `level < 5` 조건이 붙어 난이도 5 문항은 검색되지 않는다. 주석은 "전체 난이도 검색 (1~5 모두 포함)" 이라고 쓰여 있으나 코드는 5 를 제외한다.
- 근거: `search.php:213-216`
- 코드:
  ```php
  // 난이도 값이 비어 있으면 전체 난이도 검색 (1~5 모두 포함)
  if ($level == '') {
      $where .= " AND level < 5";
  ```

### CX-06 난이도 1~5 를 지정하면 정확 일치로 조회한다
- 규칙: `level` 이 `^[1-5]$` 이면 `level = ?` 정수 비교를 한다. 이때는 5 도 조회된다.
- 근거: `search.php:217-221`
- 코드:
  ```php
  else if (preg_match('/^[1-5]$/', $level)) {
      $where .= " AND level = ?";
      $values[] = (int)$level;
  ```

### CX-07 범위 밖 난이도는 거부하지 않고 정수로 변환해 비교한다
- 규칙: `level` 이 1~5 형식이 아니면 경고만 표시하고 `(int)` 변환한 값으로 `level = ?` 비교를 한다. 숫자가 아닌 문자열은 0 이 되어 대개 0건이다.
- 근거: `search.php:223-229`
- 코드:
  ```php
  $warnings[] = '난이도는 1~5 사이여야 합니다.';
  $where .= " AND level = ?";
  $values[] = (int)$level;
  ```

### CX-08 태그는 이름 정확 일치이며 하나만 지정한다
- 규칙: `tag` 는 50자로 자르고(경고 표시), `item_tag` · `tag` 를 조인한 `EXISTS` 로 태그 이름이 정확히 같은 문항만 찾는다. 부분 일치와 여러 태그 동시 조건은 없다. 미등록 태그는 경고만 하고 조회한다.
- 근거: `search.php:235-249`, `search.php:258-260`
- 코드:
  ```php
  $where .= " AND EXISTS (SELECT 1 FROM item_tag it JOIN tag t ON t.id = it.tag_id"
          . " WHERE it.item_id = v_item_public.id AND t.name = ?)";
  ```

## 2. 목록의 기본 정렬 · 제외 조건 · 페이지 (`search.php`)

### CX-09 검색은 공개 문항 뷰만 조회한다
- 규칙: 검색과 건수 조회는 `item` 테이블이 아니라 `v_item_public` 뷰를 대상으로 한다. 어떤 문항이 제외되는지는 뷰 정의에 달려 있으며 이 폴더의 코드에서는 확인되지 않는다(마지막 절 참고).
- 근거: `search.php:519-526`
- 코드:
  ```php
  $from   = " FROM v_item_public";
  $sql      = $select . $from . $where . $orderBy . $limit;
  $countSql = "SELECT COUNT(*) AS cnt" . $from . $where;
  ```

### CX-10 기본 정렬은 난이도 내림차순, 같으면 id 오름차순이다
- 규칙: `sort` 가 비어 있으면 `level DESC, id ASC` 로 정렬한다. 알 수 없는 `sort` 값도 경고 후 같은 정렬을 쓴다.
- 근거: `search.php:317-319`, `search.php:322-326`
- 코드:
  ```php
  case '':
      $orderBy = " ORDER BY level DESC, id ASC";
  default:
      $warnings[] = '알 수 없는 정렬 기준입니다. 기본 정렬을 사용합니다.';
  ```

### CX-11 정렬 기준은 5종이며 기준마다 방향 기본값과 보조 정렬이 다르다
- 규칙: `sort` 는 `id`, `title`, `unit`, `level`, `created` 만 허용한다. `dir` 이 `asc`/`desc` 가 아니면 경고 후 비운다. `id`, `title`, `unit` 은 `desc` 일 때만 내림차순이고 그 외는 오름차순이며, `level`, `created` 는 `asc` 일 때만 오름차순이고 그 외는 내림차순이다. `title` 은 `id ASC`, `unit` 은 `level DESC, id ASC`, `level`, `created` 는 `id ASC` 가 보조 정렬이다. `id` 정렬만 보조 정렬이 없다.
- 근거: `search.php:266-273`, `search.php:293-315`
- 코드:
  ```php
  $orderBy = " ORDER BY unit_code ASC, level DESC, id ASC";
  $orderBy = " ORDER BY level DESC, id ASC";
  $orderBy = " ORDER BY created_at DESC, id ASC";
  ```

### CX-12 한 페이지 20건이며 페이지 번호는 1~999 로 보정한다
- 규칙: 페이지 크기는 20 고정이고, 숫자가 아니거나 1 미만이면 1페이지, 999 초과는 999 로 보정하고 경고한다.
- 근거: `search.php:336-349`, `search.php:523`
- 코드:
  ```php
  if ($page > 999) {
      $page = 999;
  $limit  = " LIMIT 20 OFFSET " . (int)$offset;
  ```

## 3. 등록 시 검증 (`register.php`)

### CX-13 제목은 앞뒤 공백 제거 후 5자 이상 200자 이하다
- 규칙: 제목은 `trim` 후 글자 수(`mb_strlen`)가 5 미만이거나 200 초과이면 오류다. 내부 공백은 글자 수에 포함된다.
- 근거: `register.php:41`, `register.php:49-54`
- 코드:
  ```php
  if (mb_strlen($title, 'UTF-8') < 5) {
  if (mb_strlen($title, 'UTF-8') > 200) {
  ```

### CX-14 지문은 필수다
- 규칙: `trim` 한 지문이 빈 문자열이면 오류다. 길이 상한 검증은 없다.
- 근거: `register.php:42`, `register.php:56-58`
- 코드:
  ```php
  if ($stem === '') {
      $errors[] = '지문을 입력해야 합니다.';
  ```

### CX-15 단원은 필수이며 등록된 단원 id 여야 한다
- 규칙: `unit_id` 가 `unit` 테이블의 id 목록에 없으면(빈 값 포함) 오류다.
- 근거: `register.php:27-31`, `register.php:60-68`
- 코드:
  ```php
  if ((string)$u['id'] === $unitId) {
      $unitOk = true;
  ```

### CX-16 난이도는 1~5 정수여야 한다
- 규칙: `level` 이 `^[1-5]$` 가 아니면(빈 값 포함) 오류다.
- 근거: `register.php:70-72`
- 코드:
  ```php
  if (!preg_match('/^[1-5]$/', $level)) {
      $errors[] = '난이도는 1~5 사이여야 합니다.';
  ```

### CX-17 태그는 선택 사항이며 목록에 없는 태그 id 는 오류 없이 무시한다
- 규칙: 태그는 필수가 아니다. `tag` 테이블에 없는 id 는 오류 없이 버리고 존재하는 id 만 `item_tag` 에 저장한다.
- 근거: `register.php:74-81`, `register.php:104-113`
- 코드:
  ```php
  if ((string)$t['id'] === (string)$tid) {
      $validTagIds[] = (int)$tid;
  ```

### CX-18 새로 등록한 문항의 상태는 R 이다
- 규칙: 등록 INSERT 는 `status` 를 항상 `'R'` 로 넣는다. 등록 화면에서 상태를 고를 수 없다.
- 근거: `register.php:92-95`
- 코드:
  ```php
  "INSERT INTO item (id, unit_id, title, stem, level, status, created_at, updated_at)
   VALUES (?, ?, ?, ?, ?, 'R', NOW(), NOW())"
  ```

### CX-19 등록 id 는 최대 id + 1 로 직접 채번한다
- 규칙: 자동 증가에 의존하지 않고 트랜잭션 안에서 `MAX(id) + 1` 을 `FOR UPDATE` 로 조회해 새 id 로 쓰고, 문항과 태그 저장이 하나라도 실패하면 롤백한다.
- 근거: `register.php:85-90`, `register.php:119-123`
- 코드:
  ```php
  $res = $conn->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM item FOR UPDATE");
  ```

## 4. 단원 목록의 집계 조건 (`units.php`)

### CX-20 단원별 문항 수는 status 가 A 인 문항만 센다
- 규칙: 단원 목록의 문항 수는 `item.status = 'A'` 만 세며, 단원은 `grade ASC, code ASC` 로 정렬한다.
- 근거: `units.php:15-19`
- 코드:
  ```php
  (SELECT COUNT(*) FROM item i WHERE i.unit_id = u.id AND i.status = 'A') AS item_count
  ORDER BY u.grade ASC, u.code ASC
  ```

## 5. 규칙으로 확정하지 못한 항목

- **수정 검증 없음**: 이 폴더에는 문항 수정 화면 · 처리가 없다. `index.php:9-11` 의 메뉴도 검색 · 등록 · 단원 목록뿐이며, "등록 · 수정 시의 검증" 중 수정은 코드로 확인되지 않아 규칙을 적지 않았다.
- **검색 제외 조건**: `register.php:84` 주석은 검수중(R) 문항이 검색 화면에 나오지 않는다고 하고 `search.php:519` 주석은 공개 문항 뷰라고 한다. 그러나 `search.php` 의 SQL 에는 `status` 조건이 없고 실제 필터는 `v_item_public` 정의(폴더 밖 DB 스키마)에 있다. 이 폴더만으로는 어떤 상태가 제외되는지 코드로 확인할 수 없다. 뷰 정의를 읽어야 확정할 수 있다.
- **태그 중복 id**: `register.php:75-81` 은 같은 태그 id 가 여러 번 들어오면 중복 제거 없이 그대로 INSERT 를 시도한다. 중복 처리는 `item_tag` 제약에 달려 있어 확정하지 않았다.
