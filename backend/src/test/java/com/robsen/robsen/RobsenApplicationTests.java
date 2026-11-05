package com.robsen.robsen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-for-robsen-jwt-tests-only-32chars")
class RobsenApplicationTests {

	@Test
	void contextLoads() {
	}

}
