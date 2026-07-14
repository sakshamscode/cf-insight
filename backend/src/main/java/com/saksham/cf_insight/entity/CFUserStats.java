package com.saksham.cf_insight.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cf_user_stats", indexes = {
    @Index(name = "idx_handle", columnList = "handle", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CFUserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String handle;

    @Column(name = "total_solved")
    private int totalSolved;

    @Column(name = "user_rank")
    private String rank;

    private String avatar;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "userStats", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CFUserTagStats> tagStats = new ArrayList<>();

    @OneToMany(mappedBy = "userStats", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CFUserDifficultyStats> difficultyStats = new ArrayList<>();
}
