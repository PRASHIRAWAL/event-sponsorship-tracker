package com.eventtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Audit trail entry written whenever an event, sponsor or payment changes.
 * The subject is referenced by type and id only (no foreign key), so entries survive deletes.
 */
@Entity
@Table(name = "activity_log", indexes = @Index(name = "idx_activity_occurred_at", columnList = "occurred_at"))
public class ActivityLog {

    private static final int MAX_MESSAGE = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType type;

    /** "EVENT" or "SPONSOR", see {@link Alert#EVENT} and {@link Alert#SPONSOR}. */
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "message", nullable = false, length = MAX_MESSAGE)
    private String message;

    public ActivityLog() {
    }

    public ActivityLog(ActivityType type, String subjectType, Long subjectId, String message) {
        this.occurredAt = LocalDateTime.now();
        this.type = type;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.message = message.length() > MAX_MESSAGE ? message.substring(0, MAX_MESSAGE - 3) + "..." : message;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public ActivityType getType() {
        return type;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getMessage() {
        return message;
    }

    /** Deleted subjects can no longer be opened, so the UI shows them without a link. */
    public boolean isLinkable() {
        return subjectId != null && type != ActivityType.DELETED;
    }
}
