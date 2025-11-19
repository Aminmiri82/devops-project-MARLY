package org.marly.mavigo;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimJourneyRequest;
import org.marly.mavigo.client.prim.PrimJourneyResponse;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;

@SpringBootApplication
 Updated upstream
public class MavigoApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MavigoApplication.class);

	@Autowired
	private StopAreaService stopAreaService;

	@Autowired
	private PrimApiClient primApiClient;
@EnableScheduling
public class MavigoApplication {
 Stashed changes

	public static void main(String[] args) {
		SpringApplication.run(MavigoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		test();
	}

	// there's no "loop" yet, you have to test stuff manually, feel free to change stuff in this method here in order to test stuff while developing
	public void test() {
		logger.info("Testing StopAreaService.findOrCreateByQuery()");
		
		StopArea stopArea1 = stopAreaService.findOrCreateByQuery("gare du nord");
		logger.info("Found stop area: {} (ID: {})", stopArea1.getName(), stopArea1.getExternalId());
		
		StopArea stopArea2 = stopAreaService.findOrCreateByQuery("chatelet");
		logger.info("Found stop area: {} (ID: {})", stopArea2.getName(), stopArea2.getExternalId());
		
		LocalDateTime departureTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0);
		PrimJourneyRequest journeyRequest = new PrimJourneyRequest(
			stopArea1.getExternalId(), 
			stopArea2.getExternalId(), 
			departureTime
		);
		
		logger.info("Requesting journey from {} to {} at {}", 
			stopArea1.getExternalId(), stopArea2.getExternalId(), departureTime);
		PrimJourneyResponse journeyResponse = primApiClient.getJourney(journeyRequest);
		
		if (journeyResponse != null && journeyResponse.journeys() != null) {
			logger.info("Found {} journey options", journeyResponse.journeys().size());
		} else {
			logger.warn("No journeys found in response");
		}
	}

	// user creates account,
	// they can add home and work station ids using user service
	// they can create a comfort profile if they want to
	// they can create a journey using the itinerary service
	// based on the type of journey, we use diffrent modes, normal, comfort, touristic
	// comfort mode changes the PRIM api request based on the comfort profile before calling the API
	// touristic mode recommends points of interest along the way
	// reroute mode will create a new journey based on the alert

}
