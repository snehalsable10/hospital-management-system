package com.hms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every paginated list goes through here.
 *
 * Without a sort, Postgres returns heap order and an updated row moves to the
 * end of the heap - editing a record on page 1 pushed it onto the last page
 * and shifted every other row up, which a user paging through sees as rows
 * duplicating or vanishing.
 */
class PaginationUtilTest {

    @Test
    @DisplayName("page requests carry a sort, so the order is the same on every call")
    void pageRequestIsSorted() {
        PageRequest request = PaginationUtil.pageRequest(0, 10);

        assertThat(request.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("the sort is the primary key, ascending")
    void sortsByPrimaryKeyAscending() {
        Sort.Order order = PaginationUtil.pageRequest(0, 10).getSort().getOrderFor("id");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("the page number and size are passed through untouched")
    void carriesPageNumberAndSize() {
        PageRequest request = PaginationUtil.pageRequest(3, 25);

        assertThat(request.getPageNumber()).isEqualTo(3);
        assertThat(request.getPageSize()).isEqualTo(25);
    }

    @Test
    @DisplayName("two requests for the same page sort identically")
    void isStableAcrossCalls() {
        assertThat(PaginationUtil.pageRequest(1, 10).getSort())
                .isEqualTo(PaginationUtil.pageRequest(1, 10).getSort());
    }

    @ParameterizedTest(name = "page {0} size {1} is rejected")
    @CsvSource({"-1, 10", "0, 0", "0, -5", "0, 101"})
    @DisplayName("out-of-range pagination parameters are rejected")
    void rejectsBadParameters(int pageNumber, int pageSize) {
        assertThatThrownBy(() -> PaginationUtil.validatePaginationParams(pageNumber, pageSize))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "page {0} size {1} is accepted")
    @CsvSource({"0, 1", "0, 10", "7, 100"})
    @DisplayName("in-range pagination parameters are accepted, including the boundaries")
    void acceptsGoodParameters(int pageNumber, int pageSize) {
        assertThatCode(() -> PaginationUtil.validatePaginationParams(pageNumber, pageSize))
                .doesNotThrowAnyException();
    }
}
