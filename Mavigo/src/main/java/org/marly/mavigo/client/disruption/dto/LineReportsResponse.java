package org.marly.mavigo.client.disruption.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineReportsResponse(
        @JsonProperty("disruptions") List<NavitiaDisruption> disruptions,
        @JsonProperty("line_reports") List<LineReport> lineReports
) {
}

