package com.gymcore.shared;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.gymcore.exception.BusinessException;

public final class Queries {
    private Queries() {}
    public static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException.invalid("INVALID_PAGE", "Use page >= 0 e size entre 1 e 100.");
        }
        return PageRequest.of(page, size, Sort.by("id"));
    }
    public static <T> Specification<T> tenant(UUID tenant) { return equal("tenantId", tenant); }
    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }
    public static <T> Specification<T> contains(String field, String value) {
        return (root, query, cb) -> value == null ? cb.conjunction()
                : cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase(java.util.Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%", '\\');
    }
}
