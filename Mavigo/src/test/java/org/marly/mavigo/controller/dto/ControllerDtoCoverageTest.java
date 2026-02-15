package org.marly.mavigo.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.disruption.DisruptionType;
import org.marly.mavigo.models.journey.*;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.NamedComfortSetting;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.service.disruption.dto.LineInfo;
import org.marly.mavigo.service.disruption.dto.RerouteResult;
import org.marly.mavigo.service.disruption.dto.StopInfo;

class ControllerDtoCoverageTest {

    // ── JourneyResponse.from() factory methods ──

    @Test
    void journeyResponse_fromJourney_noSegments() {
        User user = new User("ext-1", "a@x.com", "User A");
        user.setId(UUID.randomUUID());
        Journey journey = new Journey();
        journey.setUser(user);
        journey.setOriginLabel("Origin");
        journey.setDestinationLabel("Destination");

        JourneyResponse response = JourneyResponse.from(journey);

        assertThat(response.journeyId()).isEqualTo(journey.getId());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.originLabel()).isEqualTo("Origin");
        assertThat(response.segments()).isEmpty();
        assertThat(response.tasksOnRoute()).isEmpty();
        assertThat(response.newBadges()).isEmpty();
    }

    @Test
    void journeyResponse_fromJourney_withSegmentsAndPoints() {
        User user = new User("ext-1", "a@x.com", "User A");
        user.setId(UUID.randomUUID());
        Journey journey = new Journey();
        journey.setUser(user);
        journey.setOriginLabel("O");
        journey.setDestinationLabel("D");

        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode("M1");
        segment.setLineName("Metro 1");
        segment.setLineColor("#FFBE00");

        JourneyPoint p1 = new JourneyPoint(segment, 1, JourneyPointType.ORIGIN, "Gare");
        p1.setCoordinates(new GeoPoint(48.84, 2.37));
        segment.addPoint(p1);

        JourneyPoint p2 = new JourneyPoint(segment, 2, JourneyPointType.DESTINATION, "Chatelet");
        p2.setCoordinates(new GeoPoint(48.86, 2.35));
        segment.addPoint(p2);

        journey.addSegment(segment);

        JourneyResponse response = JourneyResponse.from(journey);

        assertThat(response.segments()).hasSize(1);
        assertThat(response.segments().get(0).lineCode()).isEqualTo("M1");
        assertThat(response.segments().get(0).points()).hasSize(2);
        assertThat(response.segments().get(0).points().get(0).latitude()).isEqualTo(48.84);
        assertThat(response.summary().totalSegments()).isEqualTo(1);
    }

    @Test
    void journeyResponse_fromJourney_withBadges() {
        Journey journey = new Journey();
        journey.setOriginLabel("O");
        journey.setDestinationLabel("D");

        Badge badge = new Badge();
        badge.setName("First Trip");
        badge.setDescription("Completed your first trip");
        badge.setIcon("🎉");

        JourneyResponse response = JourneyResponse.from(journey, Collections.emptyList(), List.of(badge));

        assertThat(response.newBadges()).hasSize(1);
        assertThat(response.newBadges().get(0).name()).isEqualTo("First Trip");
    }

    @Test
    void journeyResponse_fromJourney_withNullBadges() {
        Journey journey = new Journey();
        journey.setOriginLabel("O");
        journey.setDestinationLabel("D");

        JourneyResponse response = JourneyResponse.from(journey, null, null);

        assertThat(response.tasksOnRoute()).isEmpty();
        assertThat(response.newBadges()).isEmpty();
    }

    @Test
    void journeyResponse_fromOptimized() {
        Journey journey = new Journey();
        journey.setOriginLabel("O");
        journey.setDestinationLabel("D");

        JourneyResponse.IncludedTaskResponse task = new JourneyResponse.IncludedTaskResponse(
                UUID.randomUUID(), "Buy milk", "nearby store", 300L, "gt-1");

        JourneyResponse response = JourneyResponse.fromOptimized(
                journey, Collections.emptyList(), List.of(task), 1800L);

        assertThat(response.baseDurationSeconds()).isEqualTo(1800L);
        assertThat(response.includedTasks()).hasSize(1);
        assertThat(response.includedTasks().get(0).title()).isEqualTo("Buy milk");
    }

    @Test
    void journeyResponse_fromOptimized_nullLists() {
        Journey journey = new Journey();
        journey.setOriginLabel("O");
        journey.setDestinationLabel("D");

        JourneyResponse response = JourneyResponse.fromOptimized(journey, null, null, null);

        assertThat(response.includedTasks()).isEmpty();
        assertThat(response.baseDurationSeconds()).isNull();
    }

    @Test
    void journeyResponse_fromTask() {
        User user = new User("ext-1", "a@x.com", "User A");
        UserTask task = new UserTask(user, "gt-1", TaskSource.GOOGLE_TASKS, "Buy groceries");
        task.setLocationHint(new GeoPoint(48.85, 2.35));
        task.setNotes("At the supermarket");

        JourneyResponse.TaskOnRouteResponse response = JourneyResponse.fromTask(task, 150.0);

        assertThat(response.title()).isEqualTo("Buy groceries");
        assertThat(response.locationLat()).isEqualTo(48.85);
        assertThat(response.distanceMeters()).isEqualTo(150.0);
    }

    @Test
    void journeyResponse_fromTask_nullLocation() {
        User user = new User("ext-1", "a@x.com", "User A");
        UserTask task = new UserTask(user, "gt-1", TaskSource.GOOGLE_TASKS, "Task");

        JourneyResponse.TaskOnRouteResponse response = JourneyResponse.fromTask(task, 0.0);

        assertThat(response.locationLat()).isNull();
        assertThat(response.locationLng()).isNull();
    }

    // ── UserResponse.from() ──

    @Test
    void userResponse_from_withGoogleLinked() {
        User user = new User("ext-1", "a@x.com", "User A");
        user.setId(UUID.randomUUID());
        user.setGoogleAccountSubject("sub-123");
        user.setGoogleAccountEmail("google@x.com");

        UserResponse response = UserResponse.from(user);

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.googleAccountLinked()).isTrue();
        assertThat(response.googleAccountEmail()).isEqualTo("google@x.com");
    }

    @Test
    void userResponse_from_withoutGoogleLinked() {
        User user = new User("ext-1", "a@x.com", "User A");
        user.setId(UUID.randomUUID());

        UserResponse response = UserResponse.from(user);

        assertThat(response.googleAccountLinked()).isFalse();
        assertThat(response.googleAccountEmail()).isNull();
    }

    @Test
    void userResponse_from_withComfortProfile() {
        User user = new User("ext-1", "a@x.com", "User A");
        user.setId(UUID.randomUUID());
        ComfortProfile profile = new ComfortProfile();
        profile.setWheelchairAccessible(true);
        profile.setMaxNbTransfers(2);
        user.setComfortProfile(profile);

        UserResponse response = UserResponse.from(user);

        assertThat(response.comfortProfile()).isNotNull();
        assertThat(response.comfortProfile().wheelchairAccessible()).isTrue();
        assertThat(response.comfortProfile().maxNbTransfers()).isEqualTo(2);
    }

    // ── ComfortProfileResponse.from() ──

    @Test
    void comfortProfileResponse_fromNull() {
        ComfortProfileResponse response = ComfortProfileResponse.from(null);
        assertThat(response.directPath()).isNull();
        assertThat(response.maxNbTransfers()).isNull();
    }

    @Test
    void comfortProfileResponse_fromProfile() {
        ComfortProfile profile = new ComfortProfile();
        profile.setDirectPath("none");
        profile.setRequireAirConditioning(true);
        profile.setMaxWalkingDuration(600);

        ComfortProfileResponse response = ComfortProfileResponse.from(profile);
        assertThat(response.directPath()).isEqualTo("none");
        assertThat(response.requireAirConditioning()).isTrue();
        assertThat(response.maxWalkingDuration()).isEqualTo(600);
    }

    // ── NamedComfortSettingResponse.from() ──

    @Test
    void namedComfortSettingResponse_from() {
        User user = new User("ext-1", "a@x.com", "User A");
        NamedComfortSetting setting = new NamedComfortSetting("Work", new ComfortProfile(), user);

        NamedComfortSettingResponse response = NamedComfortSettingResponse.from(setting);

        assertThat(response.name()).isEqualTo("Work");
        assertThat(response.comfortProfile()).isNotNull();
    }

    // ── StopInfoResponse.from() ──

    @Test
    void stopInfoResponse_from() {
        StopInfo info = new StopInfo("sa-1", "sp-1", "Gare", 3, "M1");

        StopInfoResponse response = StopInfoResponse.from(info);

        assertThat(response.stopAreaId()).isEqualTo("sa-1");
        assertThat(response.stopPointId()).isEqualTo("sp-1");
        assertThat(response.name()).isEqualTo("Gare");
        assertThat(response.sequenceInJourney()).isEqualTo(3);
        assertThat(response.onLineCode()).isEqualTo("M1");
    }

    // ── LineInfoResponse.from() ──

    @Test
    void lineInfoResponse_from() {
        LineInfo info = new LineInfo("M1", "Metro 1", "#FFBE00", TransitMode.METRO);

        LineInfoResponse response = LineInfoResponse.from(info);

        assertThat(response.lineCode()).isEqualTo("M1");
        assertThat(response.lineName()).isEqualTo("Metro 1");
        assertThat(response.lineColor()).isEqualTo("#FFBE00");
        assertThat(response.mode()).isEqualTo(TransitMode.METRO);
    }

    // ── Simple record instantiation for coverage ──

    @Test
    void planJourneyRequest_coverage() {
        PlanJourneyRequest req = new PlanJourneyRequest(
                UUID.randomUUID(), "Paris", "Lyon", "2025-12-14T18:00",
                true, false, List.of(UUID.randomUUID()),
                List.of(new TaskDetailDto("t1", "Task", "query", 48.0, 2.0, false)));

        assertThat(req.originQuery()).isEqualTo("Paris");
        assertThat(req.ecoModeEnabled()).isTrue();
        assertThat(req.taskDetails()).hasSize(1);
    }

    @Test
    void planJourneyCommand_coverage() {
        PlanJourneyRequest journeyReq = new PlanJourneyRequest(
                UUID.randomUUID(), "O", "D", "2025-12-14T18:00",
                null, null, null, null);
        JourneyPreferencesRequest prefsReq = new JourneyPreferencesRequest(true, null);
        PlanJourneyCommand cmd = new PlanJourneyCommand(journeyReq, prefsReq);
        assertThat(cmd.journey().originQuery()).isEqualTo("O");
        assertThat(cmd.preferences().comfortMode()).isTrue();
    }

    @Test
    void comfortProfileRequest_coverage() {
        ComfortProfileRequest req = new ComfortProfileRequest("none", true, 2, 600, 900, false);
        assertThat(req.directPath()).isEqualTo("none");
        assertThat(req.maxNbTransfers()).isEqualTo(2);
        assertThat(req.requireAirConditioning()).isTrue();
    }

    @Test
    void namedComfortSettingRequest_coverage() {
        ComfortProfileRequest profileReq = new ComfortProfileRequest("none", true, 2, 600, 900, false);
        NamedComfortSettingRequest req = new NamedComfortSettingRequest("Work", profileReq);
        assertThat(req.name()).isEqualTo("Work");
        assertThat(req.comfortProfile().directPath()).isEqualTo("none");
    }

    @Test
    void ecoScoreResponse_coverage() {
        EcoScoreResponse.BadgeResponse badge = new EcoScoreResponse.BadgeResponse("Eco", "desc", "🌿", OffsetDateTime.now());
        EcoScoreResponse.AllBadgeInfo info = new EcoScoreResponse.AllBadgeInfo("All", "desc", "🏆");
        EcoScoreResponse.JourneyActivityResponse activity = new EcoScoreResponse.JourneyActivityResponse(
                UUID.randomUUID(), "O", "D", 10.0, 5.0, OffsetDateTime.now());

        EcoScoreResponse response = new EcoScoreResponse(15.0, 1, List.of(badge), List.of(info), List.of(activity));

        assertThat(response.totalCo2Saved()).isEqualTo(15.0);
        assertThat(response.earnedBadges()).hasSize(1);
        assertThat(response.allBadges()).hasSize(1);
        assertThat(response.history()).hasSize(1);
    }

    @Test
    void journeyPreferencesRequest_coverage() {
        JourneyPreferencesRequest req = new JourneyPreferencesRequest(true, UUID.randomUUID());
        assertThat(req.comfortMode()).isTrue();
        assertThat(req.namedComfortSettingId()).isNotNull();
    }

    @Test
    void lineDisruptionRequest_coverage() {
        LineDisruptionRequest req = new LineDisruptionRequest("M1");
        assertThat(req.lineCode()).isEqualTo("M1");
    }

    @Test
    void stationDisruptionRequest_coverage() {
        StationDisruptionRequest req = new StationDisruptionRequest("sp-1");
        assertThat(req.stopPointId()).isEqualTo("sp-1");
    }

    @Test
    void taskDetailDto_coverage() {
        TaskDetailDto dto = new TaskDetailDto("t1", "Title", "query", 48.0, 2.0, false);
        assertThat(dto.id()).isEqualTo("t1");
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.completed()).isFalse();
    }
}
