package org.salmanekhalili.jobtrack.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> { // generic typing here is entity type and id type
    Optional<Application> findByIdAndUserId(Long id, Long userId);
}
