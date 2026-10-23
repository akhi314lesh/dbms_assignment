package com.smarthome.smart_home_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the full Spring context. Hibernate is told not to read JDBC metadata at boot, so this test
 * (and therefore `mvnw package`) also passes on a machine where Oracle is not running yet.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false")
class SmartHomeBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
