package com.ecom.Ecommerce_SpringBoot;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring context wires up against the H2 test profile
 * and the DefaultAdminSetup ApplicationRunner can run without exploding.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class EcommerceSpringBootApplicationTests {

	@MockBean
	JavaMailSender mailSender; // prevent real SMTP if any startup code ever tries to send mail

	@Test
	void contextLoads() {
	}

}
