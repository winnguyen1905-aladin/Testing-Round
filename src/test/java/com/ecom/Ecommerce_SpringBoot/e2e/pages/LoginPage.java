package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginPage extends AbstractPage {

    public LoginPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /** Navigate to {@code /signin} and return the page object. */
    public LoginPage open() {
        open("/signin");
        return this;
    }

    /** Fill the form, submit, and assert the redirect lands somewhere other than {@code /signin}. */
    public void loginAs(String email, String password) {
        page.locator("form[action='/login'] input[name='username']").fill(email);
        page.locator("form[action='/login'] input[name='password']").fill(password);
        page.locator("form[action='/login'] button[type='submit']").click();
        // Spring Security redirects ROLE_ADMIN to /admin/ and others to /.
        // Either way we shouldn't still be on /signin.
        assertThat(page).not().hasURL(Pattern.compile(".*/signin(\\?.*)?$"));
    }
}
