package org.salmanekhalili.jobtrack.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>,
        JpaSpecificationExecutor<Application> { // generic typing here is entity type and id type

    /**
     * The single read/write gate for one application. The user id is part of
     * the lookup itself, so a caller cannot reach another user's row even if
     * the controller hands over an id it should not have.
     */
    Optional<Application> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("""
            select a.status as status, count(a) as total
            from Application a
            where a.user.id = :userId
            group by a.status
            """)
    List<StatusCount> countByStatusForUser(@Param("userId") Long userId);

    interface StatusCount {
        ApplicationStatus getStatus();

        long getTotal();
    }
}
