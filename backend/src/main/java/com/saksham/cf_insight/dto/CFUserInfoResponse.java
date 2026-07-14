package com.saksham.cf_insight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CFUserInfoResponse {
    private String status;
    private String comment;
    private List<CFUser> result;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CFUser {
        private String handle;
        private String rank;
        private String avatar;
    }
}
