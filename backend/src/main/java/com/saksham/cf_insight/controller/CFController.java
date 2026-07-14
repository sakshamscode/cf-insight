package com.saksham.cf_insight.controller;

import com.saksham.cf_insight.dto.HistoryItem;
import com.saksham.cf_insight.dto.UserStatsResponse;
import com.saksham.cf_insight.service.CFService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cf")
@RequiredArgsConstructor
public class CFController {

    private final CFService cfService;

    @GetMapping("/stats/{handle}")
    public ResponseEntity<UserStatsResponse> getStats(@PathVariable String handle) {
        return ResponseEntity.ok(cfService.getStats(handle));
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryItem>> getHistory() {
        List<HistoryItem> history = cfService.getSearchHistory().stream()
                .map(stats -> HistoryItem.builder()
                        .handle(stats.getHandle())
                        .rank(stats.getRank())
                        .avatar(stats.getAvatar())
                        .totalSolved(stats.getTotalSolved())
                        .lastUpdated(stats.getLastUpdated())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
