package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AdminAddProductPage extends AbstractPage {

    public AdminAddProductPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * Fill every required field on the add-product form, click Save, and assert
     * we land back on {@code /admin/products} (Spring controller's redirect on
     * successful save).
     */
    public AdminProductsPage createProduct(NewProduct input) {
        page.locator("input[name='title']").fill(input.title());
        page.locator("textarea[name='description']").fill(input.description());
        // The first <option> is "-- Select --" with empty value; pick by value.
        page.locator("select[name='category']").selectOption(input.category());
        page.locator("input[name='price']").fill(input.price());
        page.locator("input[name='stock']").fill(input.stock());
        page.locator("input[name='discount']").fill(input.discount());
        page.locator("#activeT").check(); // Active radio
        page.locator("form button[type='submit']").click();

        assertThat(page).hasURL(Pattern.compile(".*/admin/products$"));
        return new AdminProductsPage(page, baseUrl);
    }

    /** Compact input record so tests read like {@code addPage.createProduct(new NewProduct(...))}. */
    public record NewProduct(
            String title,
            String description,
            String category,
            String price,
            String stock,
            String discount) {
        public static NewProduct of(String title, String category, String price, String stock) {
            return new NewProduct(title, title + " description", category, price, stock, "0");
        }
    }
}
