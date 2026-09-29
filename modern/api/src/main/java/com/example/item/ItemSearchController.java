package com.example.item;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문항 검색 API — 레거시 {@code search.php} 의 이관.
 * 파라미터 이름({@code q, unit, level, tag, sort, dir, page})은 레거시와 같다.
 * {@code level[]} 처럼 배열 형태로 오는 이름도 받아야 하므로 파라미터 전체를 그대로 서비스에 넘긴다.
 */
@RestController
@RequestMapping("/api/items")
public class ItemSearchController {

    private final ItemSearchService itemSearchService;

    public ItemSearchController(ItemSearchService itemSearchService) {
        this.itemSearchService = itemSearchService;
    }

    /** {@code GET /api/items/search} — 공개 문항 검색. 조건에 맞는 문항이 없어도 200 과 0건으로 응답한다. */
    @GetMapping("/search")
    public ItemSearchResponse search(@RequestParam MultiValueMap<String, String> params) {
        return itemSearchService.search(params);
    }
}
