package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class UserProductDetailPage extends AbstractPage {

    public UserProductDetailPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public UserProductDetailPage assertShowsTitle(String title) {
        assertThat(page.locator("body")).containsText(title);
        return this;
    }

    /** Click the "Add to cart" link — controller redirects back to {@code /product/{id}}. */
    public UserProductDetailPage addToCart() {
        page.locator("a[href*='/user/addCart']").first().click();
        assertThat(page).hasURL(Pattern.compile(".*/product/\\d+$"));
        return this;
    }
}
