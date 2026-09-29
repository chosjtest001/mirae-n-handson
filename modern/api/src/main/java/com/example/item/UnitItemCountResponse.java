package com.example.item;

/** {@code GET /api/units/item-counts} 응답 항목 — 단원별 공개 문항 수. */
public record UnitItemCountResponse(Integer id, String code, String name, Integer grade, Long activeItemCount) {
}
