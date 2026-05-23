package com.ecom.Ecommerce_SpringBoot.web;

import com.ecom.Ecommerce_SpringBoot.controller.HomeController;
import com.ecom.Ecommerce_SpringBoot.controller.UserController;
import com.ecom.Ecommerce_SpringBoot.entities.Cart;
import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import com.ecom.Ecommerce_SpringBoot.entities.RequestOrder;
import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;
import com.ecom.Ecommerce_SpringBoot.service.CartService;
import com.ecom.Ecommerce_SpringBoot.service.CategoryService;
import com.ecom.Ecommerce_SpringBoot.service.OrderService;
import com.ecom.Ecommerce_SpringBoot.service.ProductService;
import com.ecom.Ecommerce_SpringBoot.service.UserService;
import com.ecom.Ecommerce_SpringBoot.service.implement.CloudinaryService;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static com.ecom.Ecommerce_SpringBoot.support.TestFixtures.USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UC2 — web-slice tests covering:
 * <ul>
 *   <li>Public catalog endpoints in {@link HomeController}
 *       ({@code /products}, {@code /product/{id}}, {@code /search})</li>
 *   <li>Authenticated user flow in {@link UserController}: add-to-cart,
 *       cart view, checkout</li>
 * </ul>
 *
 * <p>Security filters are off; {@code Principal} is supplied via
 * {@code .principal(...)} for the endpoints that need it.
 */
@WebMvcTest(controllers = {HomeController.class, UserController.class},
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("web")
class UserOrderFlowWebTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CategoryService categoryService;
    @MockBean private ProductService productService;
    @MockBean private UserService userService;
    @MockBean private CartService cartService;
    @MockBean private OrderService orderService;
    @MockBean private CommonUtil commonUtil;
    @MockBean private CloudinaryService cloudinaryService;
    @MockBean private BCryptPasswordEncoder passwordEncoder;

    private UserDtls shopper;

    @BeforeEach
    void setUp() {
        when(categoryService.getAllActiveCategory()).thenReturn(List.of());
        shopper = TestFixtures.shopper();
        shopper.setId(7);
        when(userService.getUserByEmail(USER_EMAIL)).thenReturn(shopper);
        when(cartService.getCountCart(7)).thenReturn(0);
    }

    // ----- helpers -----

    private static Principal principalFor(String name) {
        return () -> name;
    }

    // ===== Public catalog (no auth) =====

    @Test
    void listProducts_noCategoryParam_returnsAllActive() throws Exception {
        Product a = TestFixtures.product("A"); a.setId(1);
        Product b = TestFixtures.product("B"); b.setId(2);
        when(productService.getAllActiveProducts("")).thenReturn(List.of(a, b));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("product"))
                .andExpect(model().attribute("products", hasSize(2)))
                .andExpect(model().attribute("paramValue", ""));
        verify(productService).getAllActiveProducts("");
    }

    @Test
    void listProducts_withCategoryParam_filters() throws Exception {
        when(productService.getAllActiveProducts("Books"))
                .thenReturn(List.of(TestFixtures.product("Book1", "Books", 10.0)));

        mockMvc.perform(get("/products").param("category", "Books"))
                .andExpect(status().isOk())
                .andExpect(view().name("product"))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(model().attribute("paramValue", "Books"));
    }

    @Test
    void viewProduct_returnsViewProductPage() throws Exception {
        Product p = TestFixtures.product("Widget"); p.setId(42);
        when(productService.getProductById(42)).thenReturn(p);

        mockMvc.perform(get("/product/{id}", 42))
                .andExpect(status().isOk())
                .andExpect(view().name("view_product"))
                .andExpect(model().attribute("product", p));
    }

    @Test
    void search_passesQueryToService() throws Exception {
        when(productService.searchProduct("widget"))
                .thenReturn(List.of(TestFixtures.product("Widget")));

        mockMvc.perform(get("/search").param("search", "widget"))
                .andExpect(status().isOk())
                .andExpect(view().name("product"))
                .andExpect(model().attribute("products", hasSize(1)));
        verify(productService).searchProduct("widget");
    }

    // ===== Logged-in flow =====

    @Test
    void addToCart_success_redirectsToProductPage() throws Exception {
        Cart saved = TestFixtures.cart(shopper, TestFixtures.product(), 1);
        saved.setId(1);
        when(cartService.cartSave(42, 7)).thenReturn(saved);

        mockMvc.perform(get("/user/addCart")
                        .param("productId", "42")
                        .param("userId", "7")
                        .principal(principalFor(USER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/42"));
        verify(cartService).cartSave(42, 7);
    }

    @Test
    void cartPage_returnsCartViewWithUserCarts() throws Exception {
        Cart c = TestFixtures.cart(shopper, TestFixtures.product(), 2);
        c.setId(1);
        c.setTotalPriceOrders(200.0);
        when(cartService.getCartsByUser(7)).thenReturn(List.of(c));

        mockMvc.perform(get("/user/cart").principal(principalFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/cart"))
                .andExpect(model().attribute("carts", hasSize(1)))
                .andExpect(model().attribute("totalOrderPrice", is(200.0)));
    }

    @Test
    void cartPage_emptyCart_omitsTotalAttribute() throws Exception {
        when(cartService.getCartsByUser(7)).thenReturn(List.of());

        mockMvc.perform(get("/user/cart").principal(principalFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/cart"))
                .andExpect(model().attribute("carts", hasSize(0)))
                .andExpect(model().attributeDoesNotExist("totalOrderPrice"));
    }

    @Test
    void cartUpdate_invokesServiceAndRedirects() throws Exception {
        mockMvc.perform(get("/user/cartUpdate")
                        .param("action", "in").param("cid", "55")
                        .principal(principalFor(USER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/cart"));
        verify(cartService).updateQuantity("in", 55);
    }

    @Test
    void orderPage_addsOrderPriceAndShippingTotalsWhenCartNonEmpty() throws Exception {
        Cart c = TestFixtures.cart(shopper, TestFixtures.product(), 1);
        c.setId(1);
        c.setTotalPriceOrders(150.0);
        when(cartService.getCartsByUser(7)).thenReturn(List.of(c));

        mockMvc.perform(get("/user/orders").principal(principalFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/order"))
                .andExpect(model().attribute("orderPrice", is(150.0)))
                .andExpect(model().attribute("totalOrderPrice", is(165.0))); // 150 + 5 + 10 shipping
    }

    @Test
    void saveOrder_invokesServiceAndRedirectsToSuccess() throws Exception {
        mockMvc.perform(post("/user/save-order")
                        .param("firstName", "Jane").param("lastName", "Doe")
                        .param("email", "jane@example.com").param("mobile", "0900000000")
                        .param("address", "1 Main St").param("city", "Hanoi")
                        .param("state", "HN").param("pincode", "100000")
                        .param("paymentType", "COD")
                        .principal(principalFor(USER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/order-success"));

        ArgumentCaptor<RequestOrder> captor = ArgumentCaptor.forClass(RequestOrder.class);
        verify(orderService).saveOrder(eq(7), captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Jane");
        assertThat(captor.getValue().getPaymentType()).isEqualTo("COD");
        assertThat(captor.getValue().getCity()).isEqualTo("Hanoi");
    }

    @Test
    void myOrders_returnsUserOrdersView() throws Exception {
        ProductOrder o = new ProductOrder();
        o.setId(99);
        when(orderService.getOrdersByUser(7)).thenReturn(List.of(o));

        mockMvc.perform(get("/user/user-orders").principal(principalFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/my-orders"))
                .andExpect(model().attribute("orders", hasSize(1)));
    }
}
