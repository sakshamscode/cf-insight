package com.saksham.cf_insight.repository;

import com.saksham.cf_insight.entity.CFUserTagStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CFUserTagStatsRepository extends JpaRepository<CFUserTagStats, Long> {
}
