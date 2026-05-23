package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class UserCatalogPage extends AbstractPage {

    public UserCatalogPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public UserCatalogPage open() {
        open("/products");
        return this;
    }

    public UserProductDetailPage clickProduct(String title) {
        page.locator("a:has-text('" + title + "')").first().click();
        assertThat(page).hasURL(Pattern.compile(".*/product/\\d+$"));
        return new UserProductDetailPage(page, baseUrl);
    }
}
