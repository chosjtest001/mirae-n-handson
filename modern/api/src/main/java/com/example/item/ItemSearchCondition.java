package com.example.item;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

/**
 * 문항 검색 조건. 레거시 search.php 의 buildSearchQuery 가 파라미터를 다루는 방식을 그대로 옮겼다.
 * 규칙 ID 는 docs/item-bank/BUSINESS-RULES.md 를 따른다.
 *
 * @param keywordPattern LIKE 패턴({@code %키워드%}). 키워드가 없으면 null
 * @param unitCode       단원 코드. 없으면 null
 * @param level          난이도. null 이면 난이도 5 를 뺀 전체({@code level < 5})
 * @param tag            태그 이름. 없으면 null
 * @param pageable       페이지(0부터) · 크기 20 · 정렬
 */
record ItemSearchCondition(String keywordPattern, String unitCode, Integer level, String tag, Pageable pageable) {

    static final int PAGE_SIZE = 20;
    static final int MAX_PAGE = 999;
    static final int MAX_KEYWORD_LENGTH = 100;
    static final int MAX_TAG_LENGTH = 50;

    /** PHP trim() 이 지우는 문자: 공백, \t, \n, \r, \0, \x0B. */
    private static final String PHP_TRIM_CHARS = " \t\n\r\0\u000B";
    /** PHP 숫자 문자열 앞에 허용되는 공백. */
    private static final String PHP_LEADING_WHITESPACE = " \t\n\r\u000B\f";
    /** PHP 숫자 문자열의 앞부분(정수 · 소수 · 지수). */
    private static final Pattern PHP_NUMERIC_PREFIX = Pattern.compile("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern PHP_INTEGER = Pattern.compile("^[+-]?\\d+$");
    /** preg_match('/^[1-5]$/'). 앞뒤 공백은 이미 지운 값에 쓴다. */
    private static final Pattern LEVEL_IN_RANGE = Pattern.compile("\\A[1-5]\\z");
    /** preg_match('/^[0-9]+$/') — PCRE 의 $ 는 끝의 줄바꿈 하나를 허용한다. */
    private static final Pattern PAGE_DIGITS = Pattern.compile("\\A[0-9]+\\n?\\z");

    /** 기본 정렬: 난이도 내림차순, 같은 난이도면 id 오름차순 (BR-19, BR-20). */
    static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("level"), Sort.Order.asc("id"));

    /** 요청 파라미터 → 검색 조건. */
    static ItemSearchCondition from(MultiValueMap<String, String> params) {
        String keyword = phpTrim(first(params, "q", ""));
        String unit = phpTrim(first(params, "unit", ""));
        String levelRaw = phpTrim(first(params, "level", ""));
        String tag = phpTrim(first(params, "tag", ""));
        String sort = phpTrim(first(params, "sort", "")).toLowerCase(Locale.ROOT);
        String dir = phpTrim(first(params, "dir", "")).toLowerCase(Locale.ROOT);
        String pageRaw = first(params, "page", "1");

        // BR-05 ~ BR-07: 100자까지만, % 와 _ 는 이스케이프하지 않는다
        keyword = truncate(keyword, MAX_KEYWORD_LENGTH);
        String keywordPattern = keyword.isEmpty() ? null : "%" + keyword + "%";

        // BR-09 ~ BR-11: 형식 · 존재 여부와 무관하게 입력값 그대로 비교한다
        String unitCode = unit.isEmpty() ? null : unit;

        // BR-12 ~ BR-14: 빈값이면 level < 5, 그 밖에는 PHP (int) 변환 값과 비교
        Integer level = null;
        if (!levelRaw.isEmpty()) {
            level = LEVEL_IN_RANGE.matcher(levelRaw).matches()
                ? Integer.valueOf(levelRaw)
                : saturateToInt(phpIntval(levelRaw));
        }

        // BR-15 ~ BR-18: 50자까지만, 정확 일치
        String tagName = tag.isEmpty() ? null : truncate(tag, MAX_TAG_LENGTH);

        return new ItemSearchCondition(keywordPattern, unitCode, level, tagName,
            PageRequest.of(page(pageRaw) - 1, PAGE_SIZE, sort(sort, dir)));
    }

    /**
     * BR-03: 같은 이름이 여러 번 오면 PHP $_GET 과 같게 고른다.
     * {@code name[]} 형태는 배열이 되어 첫 값을, 같은 {@code name} 이 반복되면 마지막 값을 쓴다.
     * 두 형태가 섞여 오면 배열 쪽을 쓴다.
     */
    private static String first(MultiValueMap<String, String> params, String name, String defaultValue) {
        List<String> arrayValues = params.get(name + "[]");
        if (arrayValues != null && !arrayValues.isEmpty()) {
            return nullToEmpty(arrayValues.get(0));
        }
        List<String> values = params.get(name);
        if (values != null && !values.isEmpty()) {
            return nullToEmpty(values.get(values.size() - 1));
        }
        return defaultValue;
    }

    /** BR-19 ~ BR-23: 정렬 기준 · 방향 → 고정된 정렬. 요청 문자열로 정렬 식을 만들지 않는다. */
    static Sort sort(String sort, String dir) {
        boolean desc = "desc".equals(dir);
        boolean asc = "asc".equals(dir);
        return switch (sort) {
            case "id" -> Sort.by(desc ? Sort.Order.desc("id") : Sort.Order.asc("id"));
            case "title" -> Sort.by(
                desc ? Sort.Order.desc("title") : Sort.Order.asc("title"),
                Sort.Order.asc("id"));
            case "unit" -> Sort.by(
                desc ? Sort.Order.desc("unit.code") : Sort.Order.asc("unit.code"),
                Sort.Order.desc("level"),
                Sort.Order.asc("id"));
            case "level" -> Sort.by(
                asc ? Sort.Order.asc("level") : Sort.Order.desc("level"),
                Sort.Order.asc("id"));
            case "created" -> Sort.by(
                asc ? Sort.Order.asc("createdAt") : Sort.Order.desc("createdAt"),
                Sort.Order.asc("id"));
            default -> DEFAULT_SORT;
        };
    }

    /** BR-26: 숫자가 아니면 1, 1 미만이면 1, 999 를 넘으면 999. */
    static int page(String pageRaw) {
        long page = 1;
        if (PAGE_DIGITS.matcher(pageRaw).matches()) {
            page = phpIntval(pageRaw);
        }
        if (page < 1) {
            page = 1;
        }
        if (page > MAX_PAGE) {
            page = MAX_PAGE;
        }
        return (int) page;
    }

    /** PHP 7.4 의 (int) 문자열 변환. 앞 숫자 부분만 읽고, 없으면 0, 범위를 넘으면 long 최대 · 최소로 맞춘다. */
    static long phpIntval(String value) {
        int start = 0;
        while (start < value.length() && PHP_LEADING_WHITESPACE.indexOf(value.charAt(start)) >= 0) {
            start++;
        }
        Matcher matcher = PHP_NUMERIC_PREFIX.matcher(value.substring(start));
        if (!matcher.find()) {
            return 0;
        }
        String number = matcher.group();
        if (PHP_INTEGER.matcher(number).matches()) {
            BigInteger parsed = new BigInteger(number.startsWith("+") ? number.substring(1) : number);
            return parsed.max(BigInteger.valueOf(Long.MIN_VALUE)).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
        }
        double parsed = Double.parseDouble(number);
        if (Double.isInfinite(parsed) || Double.isNaN(parsed)) {
            return 0;
        }
        // Java 의 double → long 변환은 0 쪽으로 버리고 범위를 넘으면 최대 · 최소로 맞춘다
        return (long) parsed;
    }

    /** level 컬럼(TINYINT)과 비교하므로 int 범위 밖의 값은 끝값으로 맞춰도 결과(0건)가 같다. */
    private static int saturateToInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    /** PHP trim() 과 같다. */
    static String phpTrim(String value) {
        int begin = 0;
        int end = value.length();
        while (begin < end && PHP_TRIM_CHARS.indexOf(value.charAt(begin)) >= 0) {
            begin++;
        }
        while (end > begin && PHP_TRIM_CHARS.indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(begin, end);
    }

    /** mb_substr($value, 0, $max, 'UTF-8') 와 같이 문자(코드 포인트) 단위로 자른다. */
    static String truncate(String value, int max) {
        if (value.codePointCount(0, value.length()) <= max) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, max));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
