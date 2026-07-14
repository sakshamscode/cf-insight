package com.saksham.cf_insight.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    private String handle;
    private String rank;
    private String avatar;
    private int totalSolved;
    private LocalDateTime lastUpdated;
    private Map<String, Integer> tagStats;
    private Map<Integer, Integer> difficultyStats;
}
