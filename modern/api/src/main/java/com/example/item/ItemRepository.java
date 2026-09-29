package com.example.item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    /** 단건 조회 — 단원 · 태그를 함께 가져온다(OSIV 꺼져 있음). */
    @EntityGraph(attributePaths = {"unit", "tags"})
    Optional<Item> findWithDetailsById(Integer id);

    /** 단원 코드 + 상태로 조회. 정렬은 난이도 내림차순, 같은 난이도면 id 오름차순. */
    @EntityGraph(attributePaths = {"unit", "tags"})
    @Query("""
        select i from Item i
        where i.unit.code = :unitCode and i.status = :status
        order by i.level desc, i.id asc
        """)
    List<Item> findByUnitCodeAndStatus(@Param("unitCode") String unitCode, @Param("status") String status);

    long countByUnitIdAndStatus(Integer unitId, String status);

    /**
     * 문항 검색 조건 — 레거시 search.php 의 WHERE 절. null 인 조건은 적용하지 않는다.
     * level 이 null 이면 난이도 5 를 뺀다(level < 5). LIKE 패턴의 % 와 _ 는 이스케이프하지 않는다.
     */
    String SEARCH_WHERE = """
        where i.status = :status
          and (:keywordPattern is null or i.title like :keywordPattern or i.stem like :keywordPattern)
          and (:unitCode is null or u.code = :unitCode)
          and ((:level is null and i.level < 5) or i.level = :level)
          and (:tag is null or exists (
                select 1 from Item i2 join i2.tags t where i2.id = i.id and t.name = :tag))
        """;

    /** 검색 조건에 맞는 문항 id 한 페이지와 전체 건수. 태그는 {@link #findWithDetailsByIdIn} 로 따로 읽는다. */
    @Query(
        value = "select i.id from Item i join i.unit u " + SEARCH_WHERE,
        countQuery = "select count(i) from Item i join i.unit u " + SEARCH_WHERE)
    Page<Integer> searchIds(
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        @Param("unitCode") String unitCode,
        @Param("level") Integer level,
        @Param("tag") String tag,
        Pageable pageable);

    /** id 목록의 문항 — 단원 · 태그를 함께 가져온다. 순서는 보장하지 않는다. */
    @EntityGraph(attributePaths = {"unit", "tags"})
    List<Item> findWithDetailsByIdIn(Collection<Integer> ids);
}
