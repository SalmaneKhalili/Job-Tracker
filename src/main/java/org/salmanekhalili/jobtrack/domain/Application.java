package org.salmanekhalili.jobtrack.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
@Entity
@Getter @Setter
@Table(name = "applications")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    @Column(name = "job_url")
    private String jobUrl;

    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        // A caller-supplied appliedAt is kept (that is what the from/to filters
        // range over); only an absent one falls back to "now". Postgres stores
        // microseconds, so truncate to avoid a response that differs from a
        // later read of the same row.
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (appliedAt == null) {
            appliedAt = now;
        } else {
            appliedAt = appliedAt.truncatedTo(ChronoUnit.MICROS);
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
