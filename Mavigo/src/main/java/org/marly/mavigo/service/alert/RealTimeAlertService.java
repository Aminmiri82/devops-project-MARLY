package org.marly.mavigo.service.alert;

import java.util.UUID;

import org.marly.mavigo.models.alert.TrafficAlert;

public interface RealTimeAlertService {

    void subscribeJourney(UUID journeyId);

    void unsubscribeJourney(UUID journeyId);

    void handleIncomingAlert(TrafficAlert alert);
}

