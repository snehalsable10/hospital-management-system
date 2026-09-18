package com.hms.util;

import com.hms.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public class PaginationUtil {

    /**
     * Convert Spring's Page object to custom PageResponse
     */
    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    /**
     * Validate pagination parameters
     */
    public static void validatePaginationParams(int pageNumber, int pageSize) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    /**
     * Get default page size (10 records per page)
     */
    public static int getDefaultPageSize() {
        return 10;
    }

    /**
     * Get max page size (100 records - free tier optimization)
     */
    public static int getMaxPageSize() {
        return 100;
    }
}