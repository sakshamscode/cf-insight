package com.saksham.cf_insight.repository;

import com.saksham.cf_insight.entity.CFUserDifficultyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CFUserDifficultyStatsRepository extends JpaRepository<CFUserDifficultyStats, Long> {
}
