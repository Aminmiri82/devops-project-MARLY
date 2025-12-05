package org.marly.mavigo.client.disruption;

import org.marly.mavigo.client.disruption.dto.LineReportsResponse;

public interface DisruptionApiClient {

    LineReportsResponse getLineReports();

    LineReportsResponse getLineReportsByUri(String uri);

    LineReportsResponse getLineReportsForLine(String lineIdOrCode);
}

