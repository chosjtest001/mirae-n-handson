package com.example.item;

import java.util.List;

/**
 * 문항 검색 응답.
 * {@code count} 는 현재 페이지 행 수가 아니라 조건에 맞는 전체 건수다.
 * {@code message} 는 전체 건수가 0 일 때만 {@link #NO_RESULT_MESSAGE}, 그 밖에는 null 이다.
 */
public record ItemSearchResponse(List<ItemSearchRow> items, long count, String message) {

    /** 레거시 search.php 의 {@code <p id="message">} 문구와 같다. */
    static final String NO_RESULT_MESSAGE = "검색 결과가 없습니다";

    static ItemSearchResponse of(List<ItemSearchRow> items, long count) {
        return new ItemSearchResponse(items, count, count == 0 ? NO_RESULT_MESSAGE : null);
    }
}
