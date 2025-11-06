package org.marly.mavigo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MavigoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MavigoApplication.class, args);
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
