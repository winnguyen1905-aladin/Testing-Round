package com.ecom.Ecommerce_SpringBoot.e2e;

import com.ecom.Ecommerce_SpringBoot.e2e.pages.AdminAddProductPage.NewProduct;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.AdminOrdersPage;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.AdminProductsPage;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.LoginPage;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.UserCartPage;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.UserCatalogPage;
import com.ecom.Ecommerce_SpringBoot.e2e.pages.UserCheckoutPage;
import com.ecom.Ecommerce_SpringBoot.entities.RequestOrder;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.StatusOrder;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-browser end-to-end test of the three use cases, driven through the
 * Page Object Model in {@link com.ecom.Ecommerce_SpringBoot.e2e.pages}.
 *
 * <p>The test reads top-down like business intent rather than CSS selectors:
 * an admin creates a product, a user buys it, the admin ships it. If a
 * template selector changes, only the relevant page object needs updating.
 *
 * <p>Run headed:    {@code mvn test -Dbrowser.e2e=true -Dbrowser.headless=false -Dbrowser.slowmo=400 -Dtest=EcommerceBrowserE2ETest}
 * <br>Run headless: {@code mvn test -Dbrowser.e2e=true -Dtest=EcommerceBrowserE2ETest}
 */
class EcommerceBrowserE2ETest extends BrowserTestBase {

    private static final String PRODUCT_TITLE = "Playwright Demo Keyboard";

    @Test
    @DisplayName("Admin creates product → user buys it → admin sees order and ships it")
    void fullPurchaseFlow() {
        BrowserContext adminCtx = newContext();
        BrowserContext userCtx = null;
        Page adminPage = adminCtx.newPage();
        try {
            // ---------- UC1: admin creates a product ----------
            new LoginPage(adminPage, baseUrl).open()
                    .loginAs(TestFixtures.ADMIN_EMAIL, TestFixtures.ADMIN_PASSWORD);

            new AdminProductsPage(adminPage, baseUrl).open()
                    .clickAddNew()
                    .createProduct(NewProduct.of(PRODUCT_TITLE, TestFixtures.DEFAULT_CATEGORY, "1234567", "25"))
                    .assertProductListed(PRODUCT_TITLE);

            // ---------- UC2: user shops the product ----------
            userCtx = newContext();
            Page userPage = userCtx.newPage();
            new LoginPage(userPage, baseUrl).open()
                    .loginAs(TestFixtures.USER_EMAIL, TestFixtures.USER_PASSWORD);

            new UserCatalogPage(userPage, baseUrl).open()
                    .clickProduct(PRODUCT_TITLE)
                    .assertShowsTitle(PRODUCT_TITLE)
                    .addToCart();

            new UserCartPage(userPage, baseUrl).open()
                    .assertContains(PRODUCT_TITLE);

            RequestOrder checkout = TestFixtures.checkoutRequest();
            checkout.setEmail(TestFixtures.USER_EMAIL);
            new UserCheckoutPage(userPage, baseUrl).open()
                    .submitOrder(checkout);

            // ---------- UC3: admin sees the order and flips status to DELIVERED ----------
            new AdminOrdersPage(adminPage, baseUrl).open()
                    .assertOrderForProductExists(PRODUCT_TITLE)
                    .updateFirstOrderStatusTo(StatusOrder.DELIVERED.getId());

            // Direct DB verification: the order row is persisted with the new status
            var orders = orderRepository.findAll();
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getStatus()).isEqualTo(StatusOrder.DELIVERED.getName());
        } catch (RuntimeException ex) {
            screenshot(adminPage, "admin-failure");
            if (userCtx != null) {
                userCtx.pages().forEach(p -> screenshot(p, "user-failure"));
            }
            throw ex;
        } finally {
            adminCtx.close();
            if (userCtx != null) userCtx.close();
        }
    }
}
