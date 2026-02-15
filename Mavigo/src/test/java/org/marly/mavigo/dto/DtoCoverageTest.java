package org.marly.mavigo.dto;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.controller.dto.CreateUserRequest;
import org.marly.mavigo.controller.dto.UpdateUserRequest;
import org.marly.mavigo.service.user.dto.GoogleAccountLink;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

class DtoCoverageTest {

    @Test
    void createUserRequestCoverage() {
        CreateUserRequest req = new CreateUserRequest(
            "John", "Doe", "john@ex.com", "pass123456", "pass123456", "Home"
        );
        
        assertThat(req.firstName()).isEqualTo("John");
        assertThat(req.lastName()).isEqualTo("Doe");
        assertThat(req.email()).isEqualTo("john@ex.com");
        assertThat(req.password()).isEqualTo("pass123456");
        assertThat(req.passwordConfirm()).isEqualTo("pass123456");
        assertThat(req.homeAddress()).isEqualTo("Home");
    }

    @Test
    void updateUserRequestCoverage() {
        UpdateUserRequest req = new UpdateUserRequest(
            "ext-123", "jane@ex.com", "Jane Smith", "Home Address"
        );
        
        assertThat(req.externalId()).isEqualTo("ext-123");
        assertThat(req.email()).isEqualTo("jane@ex.com");
        assertThat(req.displayName()).isEqualTo("Jane Smith");
        assertThat(req.homeAddress()).isEqualTo("Home Address");
    }

    @Test
    void googleAccountLinkCoverage() {
        GoogleAccountLink link = new GoogleAccountLink("sub123", "email@ex.com");
        assertThat(link.subject()).isEqualTo("sub123");
        assertThat(link.email()).isEqualTo("email@ex.com");
    }

    @Test
    void primJourneyPlanDtoCoverage() {
        PrimJourneyPlanDto.StopDateTimeDto stop = new PrimJourneyPlanDto.StopDateTimeDto(
            "id", "name", "area", 1.0, 2.0, OffsetDateTime.now(), OffsetDateTime.now()
        );
        assertThat(stop.stopPointId()).isEqualTo("id");
        assertThat(stop.latitude()).isEqualTo(1.0);

        PrimJourneyPlanDto.LegDto leg = new PrimJourneyPlanDto.LegDto(
            1, "sec-id", "walk", "mode", "code", "label", "color", "net",
            OffsetDateTime.now(), OffsetDateTime.now(), 60,
            "f-id", "f-name", 1.0, 2.0, "t-id", "t-name", 3.0, 4.0,
            "line", true, List.of(stop)
        );
        assertThat(leg.sequenceOrder()).isEqualTo(1);
        assertThat(leg.durationSeconds()).isEqualTo(60);
        assertThat(leg.stopDateTimes()).hasSize(1);
        
        PrimJourneyPlanDto journey = new PrimJourneyPlanDto("j-1", OffsetDateTime.now(), OffsetDateTime.now(), 3600, 0, List.of(leg));
        assertThat(journey.journeyId()).isEqualTo("j-1");
        assertThat(journey.hasAirConditioningOnAllTransitLegs()).isTrue();
    }
}
