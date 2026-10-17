package com.eventtracker.service;

import com.eventtracker.model.ActivityLog;
import com.eventtracker.model.ActivityType;
import com.eventtracker.repository.ActivityLogRepository;
import com.eventtracker.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Records and reads the audit trail of changes to events, sponsors and payments. */
@Service
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityLogRepository repository;

    public ActivityService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Records the change together with the signed-in user who made it.
     * Joins the caller's transaction, so a failed change never leaves a log entry behind.
     */
    @Transactional
    public void record(ActivityType type, String subjectType, Long subjectId, String message) {
        repository.save(new ActivityLog(type, subjectType, subjectId, message, CurrentUser.name()));
    }

    public List<ActivityLog> recent(int limit) {
        return repository.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, limit)).getContent();
    }

    /** All activity, newest first; subjectType "EVENT" or "SPONSOR" narrows it, null shows everything. */
    public Page<ActivityLog> page(String subjectType, Pageable pageable) {
        if (subjectType == null) {
            return repository.findAllByOrderByOccurredAtDescIdDesc(pageable);
        }
        return repository.findBySubjectTypeOrderByOccurredAtDescIdDesc(subjectType, pageable);
    }

    public List<ActivityLog> forSubject(String subjectType, Long subjectId) {
        return repository.findTop10BySubjectTypeAndSubjectIdOrderByOccurredAtDescIdDesc(subjectType, subjectId);
    }
}
