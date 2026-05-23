package com.ecom.Ecommerce_SpringBoot.support;

import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;
import com.ecom.Ecommerce_SpringBoot.repository.CartRepository;
import com.ecom.Ecommerce_SpringBoot.repository.CategoryRepository;
import com.ecom.Ecommerce_SpringBoot.repository.OrderRepository;
import com.ecom.Ecommerce_SpringBoot.repository.ProductRepository;
import com.ecom.Ecommerce_SpringBoot.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Common scaffolding for {@code @SpringBootTest}-backed integration tests:
 *
 * <ul>
 *   <li>Boots the full app context under the {@code test} profile (H2).</li>
 *   <li>Mocks {@link JavaMailSender} — {@code OrderServiceImpl.saveOrder} sends
 *       a mail per cart line and would otherwise reach a real SMTP host.</li>
 *   <li>On each test, wipes the carts/orders/products/categories tables and
 *       reseeds a {@code Peripherals} category plus a non-admin shopper user,
 *       so individual tests start from a known clean state. The admin user is
 *       seeded by {@code DefaultAdminSetup}'s {@code ApplicationRunner}.</li>
 * </ul>
 *
 * <p>Tagged {@code integration} so {@code mvn test} can exclude it for fast
 * iteration; use {@code -Dgroups=integration} (or {@code -Pall-tests}) to run.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public abstract class IntegrationTestBase {

    @Autowired protected UserRepository userRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected OrderRepository orderRepository;
    @Autowired protected CartRepository cartRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @MockBean protected JavaMailSender mailSender;

    @BeforeEach
    void resetState() {
        // Stub MimeMessage so mail-sending code paths don't NPE
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));

        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        categoryRepository.save(TestFixtures.category(TestFixtures.DEFAULT_CATEGORY));

        if (userRepository.findByEmail(TestFixtures.USER_EMAIL) == null) {
            UserDtls u = TestFixtures.shopper();
            u.setPassword(passwordEncoder.encode(TestFixtures.USER_PASSWORD));
            userRepository.save(u);
        }
    }
}
