package com.eventtracker.repository;

import com.eventtracker.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    Page<ActivityLog> findBySubjectTypeOrderByOccurredAtDescIdDesc(String subjectType, Pageable pageable);

    List<ActivityLog> findTop10BySubjectTypeAndSubjectIdOrderByOccurredAtDescIdDesc(String subjectType, Long subjectId);
}
