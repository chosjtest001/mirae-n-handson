package com.example.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** 저장소 슬라이스 — H2(MariaDB 모드)에 실제 SQL 을 날린다. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UnitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UnitRepository unitRepository;

    @BeforeEach
    void seed() {
        // 저장 순서를 일부러 섞어 정렬(학년, 코드)을 확인한다.
        Unit division = entityManager.persist(ItemFixtures.unit(4, "M6-1", "분수의 나눗셈", 6));
        Unit decimal = entityManager.persist(ItemFixtures.unit(3, "M5-3", "소수의 곱셈", 5));
        Unit fraction = entityManager.persist(ItemFixtures.unit(1, "M5-1", "분수의 덧셈과 뺄셈", 5));
        entityManager.persist(ItemFixtures.unit(2, "M5-2", "약수와 배수", 5));

        entityManager.persist(ItemFixtures.item(null, fraction, "공개 1", 2, ItemStatus.ACTIVE));
        entityManager.persist(ItemFixtures.item(null, fraction, "공개 2", 4, ItemStatus.ACTIVE));
        entityManager.persist(ItemFixtures.item(null, fraction, "검수 중", 3, ItemStatus.REVIEWING));
        entityManager.persist(ItemFixtures.item(null, fraction, "삭제", 5, ItemStatus.DELETED));
        entityManager.persist(ItemFixtures.item(null, decimal, "소수 곱셈", 1, ItemStatus.ACTIVE));
        entityManager.persist(ItemFixtures.item(null, division, "삭제만 있는 단원", 3, ItemStatus.DELETED));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("countItemsByUnit: 공개 문항만 세고, 문항 0개 단원도 포함, 학년 · 코드 순")
    void countsActiveItemsPerUnit() {
        List<UnitItemCountResponse> counts = unitRepository.countItemsByUnit(ItemStatus.ACTIVE);

        assertThat(counts).containsExactly(
            new UnitItemCountResponse(1, "M5-1", "분수의 덧셈과 뺄셈", 5, 2L),
            new UnitItemCountResponse(2, "M5-2", "약수와 배수", 5, 0L),
            new UnitItemCountResponse(3, "M5-3", "소수의 곱셈", 5, 1L),
            new UnitItemCountResponse(4, "M6-1", "분수의 나눗셈", 6, 0L));
    }
}
