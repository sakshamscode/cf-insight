package com.saksham.cf_insight.repository;

import com.saksham.cf_insight.entity.CFUserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CFUserStatsRepository extends JpaRepository<CFUserStats, Long> {
    Optional<CFUserStats> findByHandleIgnoreCase(String handle);
    List<CFUserStats> findTop10ByOrderByLastUpdatedDesc();
}
