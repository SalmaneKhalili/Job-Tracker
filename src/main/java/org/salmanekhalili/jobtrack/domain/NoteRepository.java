package org.salmanekhalili.jobtrack.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    /** Owner scope is part of every note query, not checked afterwards. */
    List<Note> findAllByApplicationIdAndApplicationUserIdOrderByCreatedAtDescIdDesc(Long applicationId, Long userId);

    Optional<Note> findByIdAndApplicationUserId(Long id, Long userId);
}
