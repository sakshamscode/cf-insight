package com.saksham.cf_insight.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryItem {
    private String handle;
    private String rank;
    private String avatar;
    private int totalSolved;
    private LocalDateTime lastUpdated;
}
