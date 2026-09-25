package org.salmanekhalili.jobtrack.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Owner scoping and the list filters, kept in one place on purpose: the
 * ownership predicate is the first thing every application query ANDs in, so it
 * is impossible to add a query that forgets it.
 */
public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    public static Specification<Application> ownedBy(Long userId) {
        return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Application> matching(ApplicationFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.company() != null && !filter.company().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("company")),
                        "%" + escapeLike(filter.company().trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("appliedAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThan(root.get("appliedAt"), filter.to()));
            }
            return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Keeps user input from turning into a wildcard in the LIKE pattern. */
    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
