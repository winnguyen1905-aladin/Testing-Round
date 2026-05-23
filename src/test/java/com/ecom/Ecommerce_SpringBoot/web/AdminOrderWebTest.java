package com.ecom.Ecommerce_SpringBoot.web;

import com.ecom.Ecommerce_SpringBoot.controller.AdminController;
import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import com.ecom.Ecommerce_SpringBoot.service.*;
import com.ecom.Ecommerce_SpringBoot.service.implement.CloudinaryService;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.CommonUtil;
import com.ecom.Ecommerce_SpringBoot.util.StatusOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UC3 — web-slice tests for the admin order endpoints in
 * {@link AdminController}: list orders, update status (incl. cancel and
 * mail-failure tolerance).
 */
@WebMvcTest(controllers = AdminController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("web")
class AdminOrderWebTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CategoryService categoryService;
    @MockBean private ProductService productService;
    @MockBean private UserService userService;
    @MockBean private OrderService orderService;
    @MockBean private CommonUtil commonUtil;
    @MockBean private CloudinaryService cloudinaryService;

    @BeforeEach
    void seedCommonMocks() {
        when(categoryService.getAllActiveCategory()).thenReturn(List.of());
    }

    private ProductOrder sampleOrder(int id, String status) {
        ProductOrder o = TestFixtures.order(
                TestFixtures.user("buyer@example.com", "Buyer"),
                TestFixtures.product("P", "C", 100.0),
                1,
                status);
        o.setId(id);
        return o;
    }

    @Test
    void getAllOrders_returnsOrdersViewWithOrdersAttribute() throws Exception {
        List<ProductOrder> orders = List.of(
                sampleOrder(1, StatusOrder.IN_PROGRESS.getName()),
                sampleOrder(2, StatusOrder.DELIVERED.getName()));
        when(orderService.getAllOrdersByUser()).thenReturn(orders);

        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attribute("orders", hasSize(2)));
        verify(orderService).getAllOrdersByUser();
    }

    @Test
    void updateStatus_mapsStatusIdToNameAndCallsService() throws Exception {
        ProductOrder updated = sampleOrder(11, StatusOrder.DELIVERED.getName());
        when(orderService.orderStatusUpdate(eq(11), eq(StatusOrder.DELIVERED.getName())))
                .thenReturn(updated);

        mockMvc.perform(post("/admin/status-order-update")
                        .param("id", "11")
                        .param("status", String.valueOf(StatusOrder.DELIVERED.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));

        verify(orderService).orderStatusUpdate(11, StatusOrder.DELIVERED.getName());
        verify(commonUtil).sendOrderMail(updated, StatusOrder.DELIVERED.getName());
    }

    @Test
    void updateStatus_cancel_callsServiceWithCancelledName() throws Exception {
        when(orderService.orderStatusUpdate(anyInt(), anyString()))
                .thenReturn(sampleOrder(12, StatusOrder.CANCEL.getName()));

        mockMvc.perform(post("/admin/status-order-update")
                        .param("id", "12")
                        .param("status", String.valueOf(StatusOrder.CANCEL.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));

        verify(orderService).orderStatusUpdate(12, StatusOrder.CANCEL.getName());
    }

    @Test
    void updateStatus_mailFailure_doesNotBreakRedirect() throws Exception {
        when(orderService.orderStatusUpdate(anyInt(), anyString()))
                .thenReturn(sampleOrder(13, StatusOrder.DELIVERED.getName()));
        doThrow(new RuntimeException("smtp down"))
                .when(commonUtil).sendOrderMail(any(), anyString());

        // Controller swallows mail exceptions in try/catch — request still redirects
        mockMvc.perform(post("/admin/status-order-update")
                        .param("id", "13")
                        .param("status", String.valueOf(StatusOrder.DELIVERED.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"));
    }
}
