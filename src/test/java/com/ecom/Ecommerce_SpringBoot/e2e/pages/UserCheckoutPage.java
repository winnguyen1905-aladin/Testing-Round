package com.ecom.Ecommerce_SpringBoot.e2e.pages;

import com.ecom.Ecommerce_SpringBoot.entities.RequestOrder;
import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class UserCheckoutPage extends AbstractPage {

    public UserCheckoutPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    public UserCheckoutPage open() {
        open("/user/orders");
        return this;
    }

    /**
     * Fill the checkout form from a {@link RequestOrder} (we reuse the production
     * DTO instead of inventing a parallel one), submit, and assert we land on
     * the success page.
     */
    public void submitOrder(RequestOrder order) {
        page.locator("input[name='firstName']").fill(order.getFirstName());
        page.locator("input[name='lastName']").fill(order.getLastName());
        page.locator("input[name='email']").fill(order.getEmail());
        page.locator("input[name='mobile']").fill(order.getMobile());
        page.locator("input[name='address']").fill(order.getAddress());
        page.locator("input[name='city']").fill(order.getCity());
        page.locator("input[name='state']").fill(order.getState());
        page.locator("input[name='pincode']").fill(order.getPincode());
        // COD is the default radio; tests can override by toggling here if needed.
        page.locator("form[action='/user/save-order'] button[type='submit']").click();
        assertThat(page).hasURL(Pattern.compile(".*/user/order-success$"));
    }
}
