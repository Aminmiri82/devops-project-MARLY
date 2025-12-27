package org.marly.mavigo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_CLIENT_ID", matches = ".+")
class MavigoApplicationTests {

	@Test
	void contextLoads() {
	}

}
