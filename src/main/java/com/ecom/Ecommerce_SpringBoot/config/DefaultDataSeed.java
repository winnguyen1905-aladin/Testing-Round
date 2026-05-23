package com.ecom.Ecommerce_SpringBoot.config;

import com.ecom.Ecommerce_SpringBoot.entities.Category;
import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;
import com.ecom.Ecommerce_SpringBoot.repository.CategoryRepository;
import com.ecom.Ecommerce_SpringBoot.repository.ProductRepository;
import com.ecom.Ecommerce_SpringBoot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database on first boot so reviewers can exercise the app
 * immediately without having to register a user or add products by hand.
 *
 * <p>All three seed operations are idempotent — they only insert when the
 * respective table is empty, so restarting a container with a persisted
 * volume never duplicates data.
 *
 * <ul>
 *   <li>5 categories that mirror a typical 7-Eleven convenience-store layout</li>
 *   <li>1 ROLE_USER test account (credentials documented in README)</li>
 *   <li>12 sample products spread across the categories, with placeholder
 *       images from picsum.photos so listings render properly out of the box</li>
 * </ul>
 *
 * <p>{@code @Order} runs this after {@link DefaultAdminSetup} so the admin
 * account is guaranteed to exist before any other data appears.
 */
@Component
@Order(2)
public class DefaultDataSeed {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedDemoData() {
        return args -> {
            seedCategories();
            seedTestUser();
            seedProducts();
        };
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        List<Category> categories = List.of(
                category("Beverages",     "https://picsum.photos/seed/beverages/400/300"),
                category("Snacks",        "https://picsum.photos/seed/snacks/400/300"),
                category("Frozen Foods",  "https://picsum.photos/seed/frozen/400/300"),
                category("Personal Care", "https://picsum.photos/seed/care/400/300"),
                category("Household",     "https://picsum.photos/seed/household/400/300")
        );
        categoryRepository.saveAll(categories);
        System.out.println("Seeded " + categories.size() + " categories.");
    }

    private void seedTestUser() {
        String email = "user@example.com";
        if (userRepository.findByEmail(email) != null) {
            return;
        }
        UserDtls u = new UserDtls();
        u.setName("Test User");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("user123"));
        u.setRole("ROLE_USER");
        u.setIsEnabled(true);
        u.setAccountNonBlocked(true);
        u.setFailedAttempt(0);
        u.setMobileNumber("0900000000");
        u.setAddress("123 Le Loi");
        u.setCity("Ho Chi Minh City");
        u.setState("HCM");
        u.setPinCode("700000");
        u.setProfileImage("https://picsum.photos/seed/avatar/200/200");
        userRepository.save(u);
        System.out.println("Seeded test user: " + email);
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }
        List<Product> products = List.of(
                // Beverages
                product("Coca-Cola 330ml",      "Beverages", "Classic Coke, ice-cold and ready to drink.", 12000, 25, 0),
                product("Pepsi 330ml",          "Beverages", "Pepsi cola, 330ml can.",                    11000, 25, 0),
                product("Red Bull Energy 250ml","Beverages", "Boosts performance, concentration & alertness.", 18000, 15, 10),
                product("Vinamilk Fresh Milk 1L","Beverages","100% pure fresh milk, refrigerated.",        35000, 12, 0),
                product("Highlands Iced Coffee","Beverages", "Vietnamese-style iced coffee with condensed milk.", 25000,  8, 5),

                // Snacks
                product("Oreo Original Cookies","Snacks",    "Twist, lick, dunk. 137g pack.",              28000, 30, 0),
                product("Lay's Classic Chips",  "Snacks",    "Crispy salted potato chips, 75g.",           22000, 25, 0),
                product("Pringles Sour Cream",  "Snacks",    "Stackable chips, 107g can.",                 45000, 18, 0),

                // Frozen Foods
                product("Cornetto Vanilla",     "Frozen Foods","Classic vanilla cone with chocolate tip.", 18000, 20, 0),
                product("Magnum Almond",        "Frozen Foods","Belgian chocolate ice cream bar with almonds.", 35000, 12, 15),

                // Personal Care
                product("Colgate Toothpaste",   "Personal Care","Cavity protection, 100g tube.",          32000, 30, 0),

                // Household
                product("AA Alkaline Batteries (4-pack)","Household","Long-lasting alkaline batteries.",   45000, 40, 0)
        );
        productRepository.saveAll(products);
        System.out.println("Seeded " + products.size() + " products.");
    }

    // ---- factory helpers ----

    private Category category(String name, String imageUrl) {
        Category c = new Category();
        c.setName(name);
        c.setImageName(imageUrl);
        c.setIsActive(true);
        return c;
    }

    private Product product(String title, String category, String description,
                            double price, int stock, int discountPercent) {
        Product p = new Product();
        p.setTitle(title);
        p.setCategory(category);
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock);
        p.setDiscount(discountPercent);
        p.setDiscountPrice(discountPercent > 0
                ? price * (1.0 - discountPercent / 100.0)
                : price);
        p.setImage("https://picsum.photos/seed/" + title.replace(' ', '-') + "/400/400");
        p.setIsActive(true);
        return p;
    }
}
