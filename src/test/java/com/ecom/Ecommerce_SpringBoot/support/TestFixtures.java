package com.ecom.Ecommerce_SpringBoot.support;

import com.ecom.Ecommerce_SpringBoot.entities.AddressOrder;
import com.ecom.Ecommerce_SpringBoot.entities.Cart;
import com.ecom.Ecommerce_SpringBoot.entities.Category;
import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import com.ecom.Ecommerce_SpringBoot.entities.RequestOrder;
import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;

/**
 * Shared test data + magic-string constants.
 *
 * <p>Two reasons this exists:
 * <ol>
 *   <li>Eliminate the dozen near-identical {@code new Product()} blocks scattered
 *       across test classes — every factory below sets a sensible default for
 *       every required field so tests only need to override what they care
 *       about.</li>
 *   <li>Concentrate credential constants ({@link #ADMIN_EMAIL} etc.) in one
 *       place; if {@code DefaultAdminSetup} ever changes, only this file
 *       follows.</li>
 * </ol>
 *
 * <p>Factories are intentionally not Lombok-builder-driven — they're a few
 * lines each and benefit from being read like recipes.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // --- Credentials ---
    public static final String ADMIN_EMAIL    = "admin@example.com";
    public static final String ADMIN_PASSWORD = "admin123";  // matches DefaultAdminSetup
    public static final String USER_EMAIL     = "shopper@example.com";
    public static final String USER_PASSWORD  = "user-pass-1!";

    // --- Common seed values ---
    public static final String DEFAULT_CATEGORY = "Peripherals";

    // ===== Product =====

    public static Product product() {
        return product("Widget", DEFAULT_CATEGORY, 100.0);
    }

    public static Product product(String title) {
        return product(title, DEFAULT_CATEGORY, 100.0);
    }

    public static Product product(String title, String category, double price) {
        Product p = new Product();
        p.setTitle(title);
        p.setDescription(title + " description");
        p.setCategory(category);
        p.setPrice(price);
        p.setDiscountPrice(price);
        p.setStock(10);
        p.setDiscount(0);
        p.setIsActive(true);
        p.setImage("https://example.com/" + title.replace(' ', '_') + ".jpg");
        return p;
    }

    // ===== Category =====

    public static Category category(String name) {
        Category c = new Category();
        c.setName(name);
        c.setImageName("default.jpg");
        c.setIsActive(true);
        return c;
    }

    // ===== UserDtls =====

    public static UserDtls user(String email, String name) {
        UserDtls u = new UserDtls();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("placeholder"); // tests that need bcrypt encode it themselves
        u.setRole("ROLE_USER");
        u.setIsEnabled(true);
        u.setAccountNonBlocked(true);
        u.setFailedAttempt(0);
        u.setMobileNumber("0900000000");
        u.setAddress("1 Test St");
        u.setCity("Hanoi");
        u.setState("HN");
        u.setPinCode("100000");
        u.setProfileImage("default.jpg");
        return u;
    }

    public static UserDtls shopper() {
        return user(USER_EMAIL, "Test Shopper");
    }

    // ===== Cart =====

    public static Cart cart(UserDtls owner, Product item, int quantity) {
        Cart c = new Cart();
        c.setUser(owner);
        c.setProduct(item);
        c.setQuantity(quantity);
        return c;
    }

    // ===== ProductOrder =====

    public static ProductOrder order(UserDtls owner, Product item, int quantity, String status) {
        ProductOrder o = new ProductOrder();
        o.setOrderId("ORD-" + System.nanoTime());
        o.setProduct(item);
        o.setUser(owner);
        o.setQuantity(quantity);
        o.setPrice(item.getDiscountPrice() == null ? item.getPrice() : item.getDiscountPrice());
        o.setStatus(status);
        o.setPaymentType("COD");
        AddressOrder addr = new AddressOrder();
        addr.setFirstName("Jane");
        addr.setLastName("Doe");
        addr.setEmail("jane@example.com");
        addr.setMobile("0900000000");
        addr.setAddress("1 Main St");
        addr.setCity("Hanoi");
        addr.setState("HN");
        addr.setPincode("100000");
        o.setAddressOrder(addr);
        return o;
    }

    // ===== RequestOrder (checkout payload) =====

    public static RequestOrder checkoutRequest() {
        RequestOrder r = new RequestOrder();
        r.setFirstName("Jane");
        r.setLastName("Doe");
        r.setEmail("jane@example.com");
        r.setMobile("0900000000");
        r.setAddress("1 Main St");
        r.setCity("Hanoi");
        r.setState("HN");
        r.setPincode("100000");
        r.setPaymentType("COD");
        return r;
    }
}
