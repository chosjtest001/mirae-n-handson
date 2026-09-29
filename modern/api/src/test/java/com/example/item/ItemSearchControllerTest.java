package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;

/** 문항 검색 컨트롤러 슬라이스 — 서비스는 목. */
@WebMvcTest(ItemSearchController.class)
@ActiveProfiles("test")
class ItemSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemSearchService itemSearchService;

    @Test
    @DisplayName("GET /api/items/search → 200, {items, count, message} 와 행의 id · title · unit · level · tags 만")
    void searchReturnsNormalizedShape() throws Exception {
        when(itemSearchService.search(any())).thenReturn(ItemSearchResponse.of(
            List.of(new ItemSearchRow(12, "대분수의 덧셈", "M5-1", 3, List.of("계산", "오답률높음"))), 1));

        mockMvc.perform(get("/api/items/search").param("q", "분수").param("unit", "M5-1").param("level", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.message").value(nullValue()))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(12))
            .andExpect(jsonPath("$.items[0].title").value("대분수의 덧셈"))
            .andExpect(jsonPath("$.items[0].unit").value("M5-1"))
            .andExpect(jsonPath("$.items[0].level").value(3))
            .andExpect(jsonPath("$.items[0].tags[1]").value("오답률높음"))
            .andExpect(jsonPath("$.items[0].stem").doesNotExist())
            .andExpect(jsonPath("$.items[0].status").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/items/search 0건 → 404 가 아니라 200, message 는 '검색 결과가 없습니다'")
    void emptyResultIsOkWithMessage() throws Exception {
        when(itemSearchService.search(any())).thenReturn(ItemSearchResponse.of(List.of(), 0));

        mockMvc.perform(get("/api/items/search").param("unit", "Z9-9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.message").value("검색 결과가 없습니다"));
    }

    @Test
    @DisplayName("GET /api/items/search level[] 배열 파라미터 → 이름 그대로 서비스에 전달")
    @SuppressWarnings("unchecked")
    void arrayParameterIsPassedThrough() throws Exception {
        when(itemSearchService.search(any())).thenReturn(ItemSearchResponse.of(List.of(), 0));

        mockMvc.perform(get("/api/items/search").param("level[]", "3", "5"))
            .andExpect(status().isOk());

        ArgumentCaptor<MultiValueMap<String, String>> captor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(itemSearchService).search(captor.capture());
        assertThat(captor.getValue().get("level[]")).containsExactly("3", "5");
    }
}
