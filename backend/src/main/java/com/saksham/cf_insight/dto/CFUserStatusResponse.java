package com.saksham.cf_insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CFUserStatusResponse {
    private String status;
    private String comment;
    private List<Submission> result;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Submission {
        private Long id;
        private Integer contestId;
        private Problem problem;
        private String verdict;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Problem {
        private Integer contestId;
        private String index;
        private String name;
        private Integer rating;
        private List<String> tags;

        // Custom equals/hashcode or helper to uniquely identify a problem
        public String getProblemId() {
            if (contestId != null && index != null) {
                return contestId + "-" + index;
            }
            return name != null ? name : "";
        }
    }
}
