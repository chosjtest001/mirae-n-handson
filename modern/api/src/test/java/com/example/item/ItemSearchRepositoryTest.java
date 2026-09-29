package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/**
 * 문항 검색 쿼리 — H2(MariaDB 모드).
 * 대소문자 구분 · 한글 정렬처럼 MariaDB 콜레이션에 달린 동작은 여기서 확인하지 않는다(동작 보존 테스트 대상).
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ItemSearchRepositoryTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, ItemSearchCondition.DEFAULT_SORT);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    private Integer fractionWord;

    @BeforeEach
    void seed() {
        Unit fraction = entityManager.persist(ItemFixtures.unit(1, "M5-1", "분수의 덧셈과 뺄셈", 5));
        Unit decimal = entityManager.persist(ItemFixtures.unit(3, "M5-3", "소수의 곱셈", 5));
        Tag calc = entityManager.persist(ItemFixtures.tag(1, "계산"));
        Tag word = entityManager.persist(ItemFixtures.tag(2, "문장제"));

        entityManager.persist(ItemFixtures.item(null, fraction, "분모가 같은 분수의 덧셈", 2, ItemStatus.ACTIVE, calc));
        fractionWord = entityManager.persist(
            ItemFixtures.item(null, fraction, "분수 문장제", 4, ItemStatus.ACTIVE, word, calc)).getId();
        entityManager.persist(ItemFixtures.item(null, fraction, "검수 중 분수", 3, ItemStatus.REVIEWING, calc));
        entityManager.persist(ItemFixtures.item(null, fraction, "삭제된 분수", 2, ItemStatus.DELETED, calc));
        entityManager.persist(ItemFixtures.item(null, fraction, "난이도 5 분수", 5, ItemStatus.ACTIVE, word));
        entityManager.persist(ItemFixtures.item(null, decimal, "소수 곱셈", 1, ItemStatus.ACTIVE));
        entityManager.flush();
        entityManager.clear();
    }

    private List<String> titles(Page<Integer> page) {
        return itemRepository.findAllById(page.getContent()).stream()
            .sorted((a, b) -> Integer.compare(page.getContent().indexOf(a.getId()), page.getContent().indexOf(b.getId())))
            .map(Item::getTitle)
            .toList();
    }

    @Test
    @DisplayName("searchIds: 조건 없음 — 공개 문항만, 난이도 5 제외, level DESC · id ASC")
    void noConditionExcludesLevel5AndNonPublic() {
        Page<Integer> page = itemRepository.searchIds(ItemStatus.ACTIVE, null, null, null, null, FIRST_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(titles(page)).containsExactly("분수 문장제", "분모가 같은 분수의 덧셈", "소수 곱셈");
    }

    @Test
    @DisplayName("searchIds: 난이도 5 를 명시하면 5 포함")
    void explicitLevel5() {
        Page<Integer> page = itemRepository.searchIds(ItemStatus.ACTIVE, null, null, 5, null, FIRST_PAGE);

        assertThat(titles(page)).containsExactly("난이도 5 분수");
    }

    @Test
    @DisplayName("searchIds: 키워드(제목 · 지문 LIKE) · 단원 · 태그를 AND 로 결합")
    void conditionsAreAnded() {
        Page<Integer> page = itemRepository.searchIds(ItemStatus.ACTIVE, "%분수%", "M5-1", null, "문장제", FIRST_PAGE);

        assertThat(titles(page)).containsExactly("분수 문장제");
    }

    @Test
    @DisplayName("searchIds: 키워드는 지문도 찾고, % 는 와일드카드로 동작")
    void keywordMatchesStemAndWildcard() {
        Page<Integer> byStem = itemRepository.searchIds(ItemStatus.ACTIVE, "%곱셈 문제 본문%", null, null, null, FIRST_PAGE);
        Page<Integer> wildcard = itemRepository.searchIds(ItemStatus.ACTIVE, "%%%", null, null, null, FIRST_PAGE);

        assertThat(titles(byStem)).containsExactly("소수 곱셈");
        assertThat(wildcard.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchIds: 없는 단원 · 없는 태그 · 범위 밖 난이도는 0건")
    void unknownValuesReturnEmpty() {
        assertThat(itemRepository.searchIds(ItemStatus.ACTIVE, null, "Z9-9", null, null, FIRST_PAGE).getTotalElements())
            .isZero();
        assertThat(itemRepository.searchIds(ItemStatus.ACTIVE, null, null, null, "없는태그", FIRST_PAGE).getTotalElements())
            .isZero();
        assertThat(itemRepository.searchIds(ItemStatus.ACTIVE, null, null, 6, null, FIRST_PAGE).getTotalElements())
            .isZero();
    }

    @Test
    @DisplayName("searchIds: 범위 밖 페이지는 행이 없고 전체 건수는 유지")
    void pageBeyondRangeKeepsTotal() {
        Page<Integer> page = itemRepository.searchIds(
            ItemStatus.ACTIVE, null, null, null, null, PageRequest.of(1, 2, ItemSearchCondition.DEFAULT_SORT));
        Page<Integer> far = itemRepository.searchIds(
            ItemStatus.ACTIVE, null, null, null, null, PageRequest.of(998, 20, ItemSearchCondition.DEFAULT_SORT));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(far.getContent()).isEmpty();
        assertThat(far.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchIds: sort=unit desc — 단원 코드 DESC, 같은 단원은 level DESC · id ASC")
    void sortByUnitDesc() {
        Pageable pageable = PageRequest.of(0, 20, ItemSearchCondition.sort("unit", "desc"));

        Page<Integer> page = itemRepository.searchIds(ItemStatus.ACTIVE, null, null, null, null, pageable);

        assertThat(titles(page)).containsExactly("소수 곱셈", "분수 문장제", "분모가 같은 분수의 덧셈");
    }

    @Test
    @DisplayName("searchIds: sort=created asc 도 실행된다")
    void sortByCreated() {
        Pageable pageable = PageRequest.of(0, 20, ItemSearchCondition.sort("created", "asc"));

        assertThat(itemRepository.searchIds(ItemStatus.ACTIVE, null, null, null, null, pageable).getTotalElements())
            .isEqualTo(3);
    }

    @Test
    @DisplayName("findWithDetailsByIdIn: 단원 · 태그를 함께 로드, 태그는 id 순")
    void findWithDetailsByIdInLoadsAssociations() {
        List<Item> items = itemRepository.findWithDetailsByIdIn(List.of(fractionWord));

        assertThat(items).hasSize(1);
        Item item = items.get(0);
        assertThat(Hibernate.isInitialized(item.getUnit())).isTrue();
        assertThat(Hibernate.isInitialized(item.getTags())).isTrue();
        assertThat(ItemSearchRow.from(item).tags()).containsExactly("계산", "문장제");
    }

    @Test
    @DisplayName("searchIds: 정렬 기준 id DESC 는 보조 정렬 없이 실행된다")
    void sortByIdDesc() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("id")));

        assertThat(itemRepository.searchIds(ItemStatus.ACTIVE, null, null, null, null, pageable).getContent())
            .isSortedAccordingTo((a, b) -> Integer.compare(b, a));
    }
}
