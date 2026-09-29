package com.example.item;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

/**
 * 문항 검색 서비스 — 레거시 search.php 의 이관.
 * 레거시 동작(이상해 보이는 것 포함)을 그대로 옮긴다. 없는 단원 · 범위 밖 난이도도 오류가 아니라 0건이다.
 */
@Service
@Transactional(readOnly = true)
public class ItemSearchService {

    private static final Logger log = LoggerFactory.getLogger(ItemSearchService.class);

    private final ItemRepository itemRepository;

    public ItemSearchService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /** 공개({@code status='A'}) 문항 검색. 페이지 크기 20, 전체 건수와 현재 페이지 행을 돌려준다. */
    public ItemSearchResponse search(MultiValueMap<String, String> params) {
        ItemSearchCondition condition = ItemSearchCondition.from(params);

        // 페이징과 태그(컬렉션) fetch join 을 한 쿼리에 섞지 않도록 id 를 먼저 구한다
        Page<Integer> page = itemRepository.searchIds(
            ItemStatus.ACTIVE,
            condition.keywordPattern(),
            condition.unitCode(),
            condition.level(),
            condition.tag(),
            condition.pageable());

        List<Integer> ids = page.getContent();
        List<ItemSearchRow> rows = List.of();
        if (!ids.isEmpty()) {
            Map<Integer, Item> itemsById = itemRepository.findWithDetailsByIdIn(ids).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
            rows = ids.stream()
                .map(itemsById::get)
                .map(ItemSearchRow::from)
                .toList();
        }
        log.debug("item search page {} returned {} of {} items",
            condition.pageable().getPageNumber() + 1, rows.size(), page.getTotalElements());
        return ItemSearchResponse.of(rows, page.getTotalElements());
    }
}
