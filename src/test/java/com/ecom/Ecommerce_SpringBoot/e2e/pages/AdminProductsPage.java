package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AdminProductsPage extends AbstractPage {

    public AdminProductsPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public AdminProductsPage open() {
        open("/admin/products");
        return this;
    }

    /** Click the "Add New" link and return the form page object. */
    public AdminAddProductPage clickAddNew() {
        page.locator("a[href='/admin/loadAddProduct']").first().click();
        assertThat(page).hasURL(Pattern.compile(".*/admin/loadAddProduct$"));
        return new AdminAddProductPage(page, baseUrl);
    }

    /** Assert the product list shows a row containing the given title. */
    public AdminProductsPage assertProductListed(String title) {
        assertThat(page.locator("body")).containsText(title);
        return this;
    }
}
