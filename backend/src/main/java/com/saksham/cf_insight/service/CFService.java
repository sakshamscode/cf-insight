package com.saksham.cf_insight.service;

import com.saksham.cf_insight.dto.CFUserInfoResponse;
import com.saksham.cf_insight.dto.CFUserStatusResponse;
import com.saksham.cf_insight.dto.UserStatsResponse;
import com.saksham.cf_insight.entity.CFUserDifficultyStats;
import com.saksham.cf_insight.entity.CFUserStats;
import com.saksham.cf_insight.entity.CFUserTagStats;
import com.saksham.cf_insight.repository.CFUserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CFService {

    private final CFUserStatsRepository cfUserStatsRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CODEFORCES_INFO_URL = "https://codeforces.com/api/user.info?handles=";
    private static final String CODEFORCES_STATUS_URL = "https://codeforces.com/api/user.status?handle=";
    private static final int CACHE_TTL_HOURS = 1;

    @Transactional(readOnly = true)
    public List<CFUserStats> getSearchHistory() {
        return cfUserStatsRepository.findTop10ByOrderByLastUpdatedDesc();
    }

    @Transactional
    public UserStatsResponse getStats(String handle) {
        String trimmedHandle = handle.trim();
        log.info("Fetching stats for handle: {}", trimmedHandle);

        Optional<CFUserStats> cachedStatsOpt = cfUserStatsRepository.findByHandleIgnoreCase(trimmedHandle);

        if (cachedStatsOpt.isPresent()) {
            CFUserStats cachedStats = cachedStatsOpt.get();
            if (cachedStats.getLastUpdated().isAfter(LocalDateTime.now().minusHours(CACHE_TTL_HOURS))) {
                log.info("Returning cached stats for handle: {}", trimmedHandle);
                return mapToResponse(cachedStats);
            }
            log.info("Cached stats expired for handle: {}. Refreshing...", trimmedHandle);
        }

        return refreshStatsFromCF(trimmedHandle, cachedStatsOpt.orElse(null));
    }

    private UserStatsResponse refreshStatsFromCF(String handle, CFUserStats existingStats) {
        CFUserInfoResponse infoResponse;
        CFUserStatusResponse statusResponse;

        try {
            // Fetch User Info
            String infoUrl = CODEFORCES_INFO_URL + handle;
            log.info("Calling CF Info API: {}", infoUrl);
            infoResponse = restTemplate.getForObject(infoUrl, CFUserInfoResponse.class);

            // Fetch User Submissions
            String statusUrl = CODEFORCES_STATUS_URL + handle;
            log.info("Calling CF Status API: {}", statusUrl);
            statusResponse = restTemplate.getForObject(statusUrl, CFUserStatusResponse.class);

        } catch (HttpClientErrorException e) {
            log.warn("Client error from Codeforces API for handle {}: {} - {}", handle, e.getStatusCode(), e.getResponseBodyAsString());
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("not found") || responseBody.contains("User not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Codeforces handle not found: " + handle);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch data from Codeforces API. Codeforces might be down or rate-limiting requests.");
        } catch (Exception e) {
            log.error("Error calling Codeforces API for handle: {}", handle, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch data from Codeforces API. Codeforces might be down or rate-limiting requests.");
        }

        if (infoResponse == null || !"OK".equalsIgnoreCase(infoResponse.getStatus()) || infoResponse.getResult() == null || infoResponse.getResult().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User details not found on Codeforces for: " + handle);
        }

        if (statusResponse == null || !"OK".equalsIgnoreCase(statusResponse.getStatus()) || statusResponse.getResult() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve submissions for: " + handle);
        }

        CFUserInfoResponse.CFUser cfUser = infoResponse.getResult().get(0);
        List<CFUserStatusResponse.Submission> submissions = statusResponse.getResult();

        // Process Submissions
        Set<String> solvedProblems = new HashSet<>();
        Map<String, Integer> tagMap = new HashMap<>();
        Map<Integer, Integer> difficultyMap = new TreeMap<>(); // Keep difficulty sorted

        for (CFUserStatusResponse.Submission submission : submissions) {
            if ("OK".equalsIgnoreCase(submission.getVerdict()) && submission.getProblem() != null) {
                CFUserStatusResponse.Problem problem = submission.getProblem();
                String problemId = problem.getProblemId();

                if (!solvedProblems.contains(problemId)) {
                    solvedProblems.add(problemId);

                    // Map tags
                    if (problem.getTags() != null) {
                        for (String tag : problem.getTags()) {
                            tagMap.put(tag, tagMap.getOrDefault(tag, 0) + 1);
                        }
                    }

                    // Map difficulty
                    if (problem.getRating() != null) {
                        Integer rating = problem.getRating();
                        difficultyMap.put(rating, difficultyMap.getOrDefault(rating, 0) + 1);
                    }
                }
            }
        }

        int totalSolved = solvedProblems.size();
        int officialSolvedCount = scrapeOfficialSolvedCount(handle);
        if (officialSolvedCount > 0) {
            totalSolved = officialSolvedCount;
        }

        // Save to Database
        CFUserStats statsToSave;
        if (existingStats != null) {
            statsToSave = existingStats;
            statsToSave.setTotalSolved(totalSolved);
            statsToSave.setRank(cfUser.getRank());
            statsToSave.setAvatar(cfUser.getAvatar());
            statsToSave.setLastUpdated(LocalDateTime.now());

            statsToSave.getTagStats().clear();
            statsToSave.getDifficultyStats().clear();
        } else {
            statsToSave = CFUserStats.builder()
                    .handle(cfUser.getHandle()) // Save the official casing of handle
                    .totalSolved(totalSolved)
                    .rank(cfUser.getRank())
                    .avatar(cfUser.getAvatar())
                    .lastUpdated(LocalDateTime.now())
                    .tagStats(new ArrayList<>())
                    .difficultyStats(new ArrayList<>())
                    .build();
        }

        final CFUserStats finalStats = statsToSave;

        // Build and add tag stats
        tagMap.forEach((tag, count) -> finalStats.getTagStats().add(CFUserTagStats.builder()
                .userStats(finalStats)
                .tagName(tag)
                .solvedCount(count)
                .build()));

        // Build and add difficulty stats
        difficultyMap.forEach((rating, count) -> finalStats.getDifficultyStats().add(CFUserDifficultyStats.builder()
                .userStats(finalStats)
                .rating(rating)
                .solvedCount(count)
                .build()));

        cfUserStatsRepository.save(finalStats);

        return UserStatsResponse.builder()
                .handle(finalStats.getHandle())
                .rank(finalStats.getRank())
                .avatar(finalStats.getAvatar())
                .totalSolved(finalStats.getTotalSolved())
                .lastUpdated(finalStats.getLastUpdated())
                .tagStats(tagMap)
                .difficultyStats(difficultyMap)
                .build();
    }

    private UserStatsResponse mapToResponse(CFUserStats stats) {
        Map<String, Integer> tagMap = stats.getTagStats().stream()
                .collect(Collectors.toMap(CFUserTagStats::getTagName, CFUserTagStats::getSolvedCount));

        Map<Integer, Integer> difficultyMap = stats.getDifficultyStats().stream()
                .collect(Collectors.toMap(CFUserDifficultyStats::getRating, CFUserDifficultyStats::getSolvedCount,
                        (v1, v2) -> v1, TreeMap::new)); // Sort by rating

        return UserStatsResponse.builder()
                .handle(stats.getHandle())
                .rank(stats.getRank())
                .avatar(stats.getAvatar())
                .totalSolved(stats.getTotalSolved())
                .lastUpdated(stats.getLastUpdated())
                .tagStats(tagMap)
                .difficultyStats(difficultyMap)
                .build();
    }

    private int scrapeOfficialSolvedCount(String handle) {
        try {
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            String cmd = "python -c \"import urllib.request, re; req=urllib.request.Request('https://codeforces.com/profile/" + handle + "', headers={'User-Agent': '" + userAgent + "'}); html=urllib.request.urlopen(req).read().decode('utf-8'); m=re.search(r'class=\\\"_UserActivityFrame_counterValue\\\">([0-9,]+) problems</div>', html); print(m.group(1).replace(',', '')) if m else print(0)\"";
            
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", cmd);
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line = reader.readLine();
            process.waitFor();
            
            if (line != null) {
                int count = Integer.parseInt(line.trim());
                if (count > 0) {
                    log.info("Scraped official solved count via python process for {}: {}", handle, count);
                    return count;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scrape official solved count via python process for handle: {}. Falling back to API count.", handle, e);
        }
        return 0;
    }
}
