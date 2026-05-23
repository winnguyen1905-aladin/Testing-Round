package com.ecom.Ecommerce_SpringBoot.integration;

import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;
import com.ecom.Ecommerce_SpringBoot.support.IntegrationTestBase;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.StatusOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ecom.Ecommerce_SpringBoot.support.TestFixtures.ADMIN_EMAIL;
import static com.ecom.Ecommerce_SpringBoot.support.TestFixtures.ADMIN_PASSWORD;
import static com.ecom.Ecommerce_SpringBoot.support.TestFixtures.USER_EMAIL;
import static com.ecom.Ecommerce_SpringBoot.support.TestFixtures.USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Full-stack integration tests covering the three required use cases. Real
 * Spring Security filter chain, real H2 DB, real Thymeleaf rendering — only
 * {@code JavaMailSender} is mocked (handled by {@link IntegrationTestBase}).
 *
 * <p>Grouped via {@code @Nested} so the IDE renders them as a tree and they
 * share the cached Spring context:
 * <ul>
 *   <li>{@link AuthGates} — the security filter chain enforces roles correctly</li>
 *   <li>{@link AdminProductCrud} — UC1, admin product management</li>
 *   <li>{@link UserOrderFlow} — UC2, user shops &amp; checks out</li>
 *   <li>{@link AdminOrderView} — UC3, admin views &amp; ships the order</li>
 * </ul>
 */
@AutoConfigureMockMvc
class EcommerceFullStackTests extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;

    private static final MockMultipartFile EMPTY_IMG =
            new MockMultipartFile("file", "", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

    // ========================================================
    // Authentication / authorization
    // ========================================================

    @Nested
    @DisplayName("Authentication / authorization gates")
    class AuthGates {

        @Test
        void adminFormLoginSucceeds() throws Exception {
            mockMvc.perform(formLogin("/login").user("username", ADMIN_EMAIL).password("password", ADMIN_PASSWORD))
                    .andExpect(authenticated().withUsername(ADMIN_EMAIL).withRoles("ADMIN"));
        }

        @Test
        void userFormLoginSucceeds() throws Exception {
            mockMvc.perform(formLogin("/login").user("username", USER_EMAIL).password("password", USER_PASSWORD))
                    .andExpect(authenticated().withUsername(USER_EMAIL).withRoles("USER"));
        }

        @Test
        void anonRequestToAdminIsRedirectedToSignin() throws Exception {
            mockMvc.perform(get("/admin/products"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/signin"));
        }

        @Test
        void anonRequestToUserCartIsRedirectedToSignin() throws Exception {
            mockMvc.perform(get("/user/cart"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/signin"));
        }

        @Test
        void roleUserCannotAccessAdminProducts() throws Exception {
            mockMvc.perform(get("/admin/products").with(user(USER_EMAIL).roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================
    // UC1 — Admin product CRUD against real DB
    // ========================================================

    @Nested
    @DisplayName("UC1 — Admin product management (CRUD)")
    class AdminProductCrud {

        @Test
        @DisplayName("Admin can list, create, view/edit, update and delete a product end-to-end")
        @Transactional
        void fullProductCrud() throws Exception {
            // LIST (initially empty)
            mockMvc.perform(get("/admin/products").with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/products"))
                    .andExpect(model().attribute("products", List.of()));

            // ADD form
            mockMvc.perform(get("/admin/loadAddProduct").with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/add_product"))
                    .andExpect(model().attributeExists("product"));

            // CREATE
            mockMvc.perform(multipart("/admin/saveProduct").file(EMPTY_IMG)
                            .param("title", "Mechanical Keyboard")
                            .param("description", "Hot-swappable, RGB")
                            .param("category", "Peripherals")
                            .param("price", "1500000")
                            .param("stock", "20")
                            .param("discount", "10")
                            .param("isActive", "true")
                            .with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/products"));

            List<Product> saved = productRepository.findAll();
            assertThat(saved).hasSize(1);
            Product p = saved.get(0);
            assertThat(p.getTitle()).isEqualTo("Mechanical Keyboard");
            assertThat(p.getStock()).isEqualTo(20);
            assertThat(p.getPrice()).isEqualTo(1500000.0);
            // saveProduct controller sets discountPrice = price (no discount math at create time)
            assertThat(p.getDiscountPrice()).isEqualTo(1500000.0);

            // DETAIL
            int id = p.getId();
            mockMvc.perform(get("/admin/editProduct/{id}", id).with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/edit_product"))
                    .andExpect(model().attribute("product", hasProperty("id", is(id))));

            // UPDATE
            mockMvc.perform(multipart("/admin/updateProduct").file(EMPTY_IMG)
                            .param("id", String.valueOf(id))
                            .param("title", "Mechanical Keyboard v2")
                            .param("description", "Hot-swappable, RGB, wireless")
                            .param("category", "Peripherals")
                            .param("price", "1800000")
                            .param("stock", "15")
                            .param("discount", "20")
                            .param("isActive", "true")
                            .with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/editProduct/" + id));

            Product updated = productRepository.findById(id).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("Mechanical Keyboard v2");
            // ProductServiceImpl.updateProduct re-computes discountPrice = price * (1 - discount/100)
            assertThat(updated.getDiscountPrice()).isEqualTo(1800000.0 * 0.80);

            // DELETE
            mockMvc.perform(get("/admin/deleteProduct/{id}", id).with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/products"));
            assertThat(productRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("Create with invalid input re-renders the add form, no row persisted")
        void createInvalidInputShowsValidationErrors() throws Exception {
            long before = productRepository.count();

            mockMvc.perform(multipart("/admin/saveProduct").file(EMPTY_IMG)
                            .param("title", "")
                            .param("description", "")
                            .param("category", "")
                            .param("price", "0.0")
                            .with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/add_product"));

            assertThat(productRepository.count()).isEqualTo(before);
        }
    }

    // ========================================================
    // UC2 — User browses + orders
    // ========================================================

    @Nested
    @DisplayName("UC2 — User product listing + order creation")
    class UserOrderFlow {

        @Test
        @DisplayName("Anonymous user can browse the public catalog, filter, view detail, and search")
        void publicCatalogIsVisibleWithoutLogin() throws Exception {
            Product coffee = productRepository.save(TestFixtures.product("Coffee Beans", "Beverages", 250000.0));
            productRepository.save(TestFixtures.product("Tea Leaves", "Beverages", 120000.0));
            Product inactive = TestFixtures.product("Inactive Item", "Beverages", 1.0);
            inactive.setIsActive(false);
            productRepository.save(inactive);

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("product"))
                    .andExpect(model().attribute("products", hasSize(2)));

            mockMvc.perform(get("/products").param("category", "Beverages"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("paramValue", "Beverages"));

            mockMvc.perform(get("/product/{id}", coffee.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("view_product"))
                    .andExpect(model().attribute("product",
                            hasProperty("title", is("Coffee Beans"))));

            mockMvc.perform(get("/search").param("search", "Coffee"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("products", hasSize(1)));
        }

        @Test
        @DisplayName("Logged-in user adds product to cart twice → checkout creates ProductOrder")
        @Transactional
        void userAddsToCartAndPlacesOrder() throws Exception {
            Product product = productRepository.save(
                    TestFixtures.product("USB-C Hub", "Peripherals", 500000.0));
            UserDtls shopper = userRepository.findByEmail(USER_EMAIL);

            addToCartTwice(product.getId(), shopper.getId());

            assertThat(cartRepository.findByUserId(shopper.getId())).hasSize(1);
            assertThat(cartRepository.findByUserId(shopper.getId()).get(0).getQuantity()).isEqualTo(2);

            // View cart renders successfully
            mockMvc.perform(get("/user/cart").with(user(USER_EMAIL).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/cart"))
                    .andExpect(model().attribute("carts", hasSize(1)));

            // Checkout
            mockMvc.perform(post("/user/save-order")
                            .param("firstName", "Test").param("lastName", "Shopper")
                            .param("email", USER_EMAIL).param("mobile", "0900000000")
                            .param("address", "1 Test St").param("city", "Hanoi")
                            .param("state", "HN").param("pincode", "100000")
                            .param("paymentType", "COD")
                            .with(user(USER_EMAIL).roles("USER")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/order-success"));

            List<ProductOrder> orders = orderRepository.findByUserId(shopper.getId());
            assertThat(orders).hasSize(1);
            ProductOrder order = orders.get(0);
            assertThat(order.getStatus()).isEqualTo(StatusOrder.IN_PROGRESS.getName());
            assertThat(order.getQuantity()).isEqualTo(2);
            assertThat(order.getAddressOrder().getCity()).isEqualTo("Hanoi");
        }
    }

    // ========================================================
    // UC3 — Admin views & updates orders
    // ========================================================

    @Nested
    @DisplayName("UC3 — Admin order viewing + status update")
    class AdminOrderView {

        @Test
        @DisplayName("Order placed by user appears in /admin/orders; admin flips status to DELIVERED")
        @Transactional
        void adminSeesOrderAndUpdatesStatus() throws Exception {
            Product product = productRepository.save(
                    TestFixtures.product("Webcam", "Peripherals", 800000.0));
            UserDtls shopper = userRepository.findByEmail(USER_EMAIL);

            addToCartTwice(product.getId(), shopper.getId());
            mockMvc.perform(post("/user/save-order")
                            .param("firstName", "Test").param("lastName", "Shopper")
                            .param("email", USER_EMAIL).param("mobile", "0900000000")
                            .param("address", "1").param("city", "Hanoi")
                            .param("state", "HN").param("pincode", "100000")
                            .param("paymentType", "COD")
                            .with(user(USER_EMAIL).roles("USER")))
                    .andExpect(status().is3xxRedirection());

            // Admin sees it
            MvcResult result = mockMvc.perform(get("/admin/orders").with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/orders"))
                    .andExpect(model().attribute("orders", hasSize(1)))
                    .andReturn();

            @SuppressWarnings("unchecked")
            List<ProductOrder> orders = (List<ProductOrder>) result.getModelAndView().getModel().get("orders");
            ProductOrder placed = orders.get(0);
            assertThat(placed.getStatus()).isEqualTo(StatusOrder.IN_PROGRESS.getName());

            // Admin ships it
            mockMvc.perform(post("/admin/status-order-update")
                            .param("id", String.valueOf(placed.getId()))
                            .param("status", String.valueOf(StatusOrder.DELIVERED.getId()))
                            .with(user(ADMIN_EMAIL).roles("ADMIN")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"));

            ProductOrder reloaded = orderRepository.findById(placed.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(StatusOrder.DELIVERED.getName());
        }
    }

    // ----- helpers shared across @Nested classes -----

    private void addToCartTwice(int productId, int userId) throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/user/addCart")
                            .param("productId", String.valueOf(productId))
                            .param("userId", String.valueOf(userId))
                            .with(user(USER_EMAIL).roles("USER")))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
