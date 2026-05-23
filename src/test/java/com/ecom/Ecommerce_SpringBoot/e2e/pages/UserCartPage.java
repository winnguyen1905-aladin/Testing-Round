package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class UserCartPage extends AbstractPage {

    public UserCartPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public UserCartPage open() {
        open("/user/cart");
        return this;
    }

    public UserCartPage assertContains(String productTitle) {
        assertThat(page.locator("body")).containsText(productTitle);
        return this;
    }
}
