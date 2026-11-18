package com.ga.workspace_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "waslah.booking.scheduler.enabled=false")
@ActiveProfiles("test")
class WorkspaceServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}