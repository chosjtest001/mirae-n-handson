package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ItemSearchServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemSearchService itemSearchService;

    private final Unit unit = ItemFixtures.unit(1, "M5-1", "분수의 덧셈과 뺄셈", 5);

    private static MultiValueMap<String, String> params(String... keyValues) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.add(keyValues[i], keyValues[i + 1]);
        }
        return params;
    }

    private static Page<Integer> page(List<Integer> ids, Pageable pageable, long total) {
        return new PageImpl<>(ids, pageable, total);
    }

    @Test
    @DisplayName("search: 저장소의 id 순서대로 행을 만들고 태그는 id 순, 전체 건수를 count 로")
    void searchKeepsIdOrder() {
        Tag calc = ItemFixtures.tag(1, "계산");
        Tag hard = ItemFixtures.tag(4, "오답률높음");
        when(itemRepository.searchIds(eq("A"), isNull(), isNull(), isNull(), isNull(), any()))
            .thenAnswer(inv -> page(List.of(7, 3), inv.getArgument(5), 25));
        when(itemRepository.findWithDetailsByIdIn(List.of(7, 3))).thenReturn(List.of(
            ItemFixtures.item(3, unit, "난이도 4", 4, ItemStatus.ACTIVE),
            ItemFixtures.item(7, unit, "난이도 4 태그", 4, ItemStatus.ACTIVE, hard, calc)));

        ItemSearchResponse response = itemSearchService.search(params());

        assertThat(response.items()).extracting(ItemSearchRow::id).containsExactly(7, 3);
        assertThat(response.items().get(0).tags()).containsExactly("계산", "오답률높음");
        assertThat(response.items().get(1).tags()).isEmpty();
        assertThat(response.count()).isEqualTo(25);
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("search: 전체 0건이면 message '검색 결과가 없습니다', 상세 조회는 하지 않는다")
    void emptyResultHasMessage() {
        when(itemRepository.searchIds(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> page(List.of(), inv.getArgument(5), 0));

        ItemSearchResponse response = itemSearchService.search(params("unit", "Z9-9"));

        assertThat(response.items()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.message()).isEqualTo("검색 결과가 없습니다");
        verify(itemRepository, never()).findWithDetailsByIdIn(anyCollection());
    }

    @Test
    @DisplayName("search: 전체 건수가 있고 현재 페이지가 비면 count 는 유지, message 는 null")
    void emptyPageKeepsCount() {
        when(itemRepository.searchIds(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> page(List.of(), inv.getArgument(5), 20));

        ItemSearchResponse response = itemSearchService.search(params("page", "2"));

        assertThat(response.count()).isEqualTo(20);
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("search: 키워드 · 단원 · 태그는 trim, 키워드는 %…% 패턴, 난이도는 정수로 저장소에 넘긴다")
    void passesConditionsToRepository() {
        when(itemRepository.searchIds(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> page(List.of(), inv.getArgument(5), 0));

        itemSearchService.search(params("q", " 분수 ", "unit", " m5-1 ", "level", "3", "tag", " 계산 "));

        verify(itemRepository).searchIds(eq("A"), eq("%분수%"), eq("m5-1"), eq(3), eq("계산"), any());
    }

    @Test
    @DisplayName("search: 공백뿐인 키워드 · 단원 · 태그와 빈 난이도는 조건 없음(null)")
    void blankConditionsBecomeNull() {
        when(itemRepository.searchIds(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> page(List.of(), inv.getArgument(5), 0));

        itemSearchService.search(params("q", "  ", "unit", " ", "tag", " ", "level", ""));

        verify(itemRepository).searchIds(eq("A"), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("search: 페이지 번호는 0부터, 크기 20, 999 를 넘으면 999")
    void pageIsClampedTo999() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(itemRepository.searchIds(any(), any(), any(), any(), any(), captor.capture()))
            .thenAnswer(inv -> page(List.of(), inv.getArgument(5), 0));

        itemSearchService.search(params("page", "1000"));

        assertThat(captor.getValue().getPageNumber()).isEqualTo(998);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(ItemSearchCondition.DEFAULT_SORT);
    }

    @Test
    @DisplayName("조건 변환: 키워드 101자는 100자로, 태그 51자는 50자로 자른다(문자 단위)")
    void truncatesKeywordAndTag() {
        String q = "%".repeat(98) + "분수" + "Z";
        String tag = "태".repeat(51);

        ItemSearchCondition condition = ItemSearchCondition.from(params("q", q, "tag", tag));

        assertThat(condition.keywordPattern()).isEqualTo("%" + "%".repeat(98) + "분수" + "%");
        assertThat(condition.tag()).isEqualTo("태".repeat(50));
    }

    @Test
    @DisplayName("조건 변환: 난이도는 PHP (int) 변환 — 3a→3, 6→6, 0→0, abc→0, 너무 큰 값은 int 끝값")
    void levelUsesPhpIntCast() {
        assertThat(ItemSearchCondition.from(params("level", "3a")).level()).isEqualTo(3);
        assertThat(ItemSearchCondition.from(params("level", "6")).level()).isEqualTo(6);
        assertThat(ItemSearchCondition.from(params("level", "0")).level()).isZero();
        assertThat(ItemSearchCondition.from(params("level", "abc")).level()).isZero();
        assertThat(ItemSearchCondition.from(params("level", "1e1")).level()).isEqualTo(10);
        assertThat(ItemSearchCondition.from(params("level", "99999999999999999999")).level())
            .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("조건 변환: level[] 는 첫 값, 같은 이름 반복은 마지막 값(PHP $_GET 과 같음)")
    void arrayParameterUsesFirstValue() {
        assertThat(ItemSearchCondition.from(params("level[]", "3", "level[]", "5")).level()).isEqualTo(3);
        assertThat(ItemSearchCondition.from(params("level", "3", "level", "5")).level()).isEqualTo(5);
    }

    @Test
    @DisplayName("조건 변환: 페이지 — 숫자가 아니면 1, 0 은 1, 999 는 그대로, 아주 큰 수는 999")
    void pageNormalization() {
        assertThat(ItemSearchCondition.page("-1")).isEqualTo(1);
        assertThat(ItemSearchCondition.page("0")).isEqualTo(1);
        assertThat(ItemSearchCondition.page("")).isEqualTo(1);
        assertThat(ItemSearchCondition.page("2x")).isEqualTo(1);
        assertThat(ItemSearchCondition.page("999")).isEqualTo(999);
        assertThat(ItemSearchCondition.page("99999999999999999999")).isEqualTo(999);
    }

    @Test
    @DisplayName("조건 변환: 정렬 — 방향 규칙과 보조 정렬 id ASC, 알 수 없는 값은 기본 정렬")
    void sortMapping() {
        assertThat(ItemSearchCondition.sort("title", "desc"))
            .isEqualTo(Sort.by(Sort.Order.desc("title"), Sort.Order.asc("id")));
        assertThat(ItemSearchCondition.sort("unit", "desc"))
            .isEqualTo(Sort.by(Sort.Order.desc("unit.code"), Sort.Order.desc("level"), Sort.Order.asc("id")));
        assertThat(ItemSearchCondition.sort("level", "up"))
            .isEqualTo(Sort.by(Sort.Order.desc("level"), Sort.Order.asc("id")));
        assertThat(ItemSearchCondition.sort("created", "asc"))
            .isEqualTo(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        assertThat(ItemSearchCondition.sort("id", "desc")).isEqualTo(Sort.by(Sort.Order.desc("id")));
        assertThat(ItemSearchCondition.sort("foo", "up")).isEqualTo(ItemSearchCondition.DEFAULT_SORT);
        assertThat(ItemSearchCondition.from(params("sort", " TITLE ", "dir", "DESC")).pageable().getSort())
            .isEqualTo(Sort.by(Sort.Order.desc("title"), Sort.Order.asc("id")));
    }
}
