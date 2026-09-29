package com.example.item;

import java.util.Comparator;
import java.util.List;

/**
 * 문항 검색 결과 한 행. 레거시 결과 표의 열(id, title, unit, level, tags)만 담는다.
 * {@code unit} 은 단원 코드, {@code tags} 는 태그 이름 목록(tag.id 순, 없으면 빈 목록)이다.
 */
public record ItemSearchRow(Integer id, String title, String unit, Integer level, List<String> tags) {

    static ItemSearchRow from(Item item) {
        return new ItemSearchRow(
            item.getId(),
            item.getTitle(),
            item.getUnit().getCode(),
            item.getLevel(),
            // 레거시 뷰의 GROUP_CONCAT(... ORDER BY t.id) 와 같은 순서
            item.getTags().stream()
                .sorted(Comparator.comparing(Tag::getId))
                .map(Tag::getName)
                .toList());
    }
}
