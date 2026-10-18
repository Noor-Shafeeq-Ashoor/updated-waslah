
package com.ga.waslah.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Provides reusable JPA Specification helpers for filtering entities.
 *
 * <p>Supports both direct fields, such as "status", and nested fields,
 * such as "studySpace.id".</p>
 */
public final class Specs {

    private Specs() {
        // Utility class; prevent instantiation.
    }

    /**
     * Resolves a direct or nested entity field path.
     */
    @SuppressWarnings("unchecked")
    private static <T, V> Path<V> getPath(Root<T> root, String field) {
        Path<?> path = root;

        // Resolve each part of paths such as "studySpace.id".
        for (String part : field.split("\\.")) {
            path = path.get(part);
        }

        return (Path<V>) path;
    }

    /**Creates an equality filter for a specified entity field.*/
    public static <T> Specification<T> equal(Object value, String field) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(getPath(root, field), value);
        };
    }

    /**
     * Creates a case-insensitive partial-text filter.
     *
     * <p>For example, searching for "man" can match "Manama".
     * SQL LIKE wildcard characters in the input are escaped.</p>
     *
     * @param value the text to search for
     * @param field the entity field name
     * @param <T> the entity type
     * @return a matching specification, or no restriction for blank input
     */
    public static <T> Specification<T> containsIgnoreCase(
            String value,
            String field
    ) {
        return (root, query, criteriaBuilder) -> {
            if (value == null || value.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            // Escape wildcard characters so the input is treated literally.
            String escapedValue = value
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");

            Expression<String> fieldExpression = getPath(root, field);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(fieldExpression),
                    "%" + escapedValue.toLowerCase(Locale.ROOT) + "%",
                    '\\'
            );
        };
    }

    /**Creates a minimum-value filter (greater than or equal to).*/
    public static <T, V extends Comparable<? super V>>
    Specification<T> atLeast(V value, String field) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    Specs.<T, V>getPath(root, field),
                    value
            );
        };
    }

    /**Creates a maximum-value filter (less than or equal to).*/
    public static <T, V extends Comparable<? super V>>
    Specification<T> atMost(V value, String field) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    Specs.<T, V>getPath(root, field),
                    value
            );
        };
    }
}
