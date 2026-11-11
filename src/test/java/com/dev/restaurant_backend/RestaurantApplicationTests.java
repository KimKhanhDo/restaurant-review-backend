package com.dev.restaurant_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RestaurantApplicationTests {

	@Test
	void contextLoads() {
	}

}
