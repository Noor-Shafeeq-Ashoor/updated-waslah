
package com.ga.waslah.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PagingUtil {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PagingUtil() {
    }

    public static Pageable createPageable(
            int page,
            int size,
            String sort,
            String direction,
            String defaultSort,
            Set<String> allowedSortFields
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative."
            );
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100."
            );
        }

        String sortField = (sort == null || sort.isBlank())
                ? defaultSort
                : sort;

        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: " + sortField
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(
                    direction == null || direction.isBlank()
                            ? "asc"
                            : direction
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Sort direction must be 'asc' or 'desc'."
            );
        }

        Sort sorting = Sort.by(sortDirection, sortField)
                .and(Sort.by(Sort.Direction.ASC, "id"));

        return PageRequest.of(page, size, sorting);
    }
}
