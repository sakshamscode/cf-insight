package com.saksham.cf_insight.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cf_user_tag_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CFUserTagStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_stats_id", nullable = false)
    private CFUserStats userStats;

    @Column(name = "tag_name", nullable = false)
    private String tagName;

    @Column(name = "solved_count", nullable = false)
    private int solvedCount;
}
