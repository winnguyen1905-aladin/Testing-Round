package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AdminOrdersPage extends AbstractPage {

    public AdminOrdersPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public AdminOrdersPage open() {
        open("/admin/orders");
        return this;
    }

    public AdminOrdersPage assertOrderForProductExists(String productTitle) {
        assertThat(page.locator("body")).containsText(productTitle);
        return this;
    }

    /**
     * Update the first visible order's status to the given {@code StatusOrder.id}.
     * For example {@code 5} = DELIVERED (see {@link com.ecom.Ecommerce_SpringBoot.util.StatusOrder}).
     */
    public AdminOrdersPage updateFirstOrderStatusTo(int statusId) {
        page.locator("form[action='/admin/status-order-update'] select[name='status']").first()
                .selectOption(String.valueOf(statusId));
        page.locator("form[action='/admin/status-order-update'] button[type='submit']").first().click();
        assertThat(page).hasURL(Pattern.compile(".*/admin/orders$"));
        return this;
    }
}
