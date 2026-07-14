package com.saksham.cf_insight.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cf_user_difficulty_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CFUserDifficultyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_stats_id", nullable = false)
    private CFUserStats userStats;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "solved_count", nullable = false)
    private int solvedCount;
}
